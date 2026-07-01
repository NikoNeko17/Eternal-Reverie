package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.random.Random

object AffinityMarkManager {

    private val activeMarks  = mutableMapOf<UUID, MutableMap<Affinity, AffinityMark>>()
    private val markSources  = mutableMapOf<UUID, MutableMap<Affinity, UUID>>()

    private const val BASE_PROC_CHANCE = 1.0

    // ── onHit (proc probabilístico desde CombatResolver) ─────────────────────

    fun onHit(
        attacker: LivingEntity,
        target: LivingEntity,
        weaponAffinities: List<Pair<Affinity, Double>>,
        hitDamage: Double,
        procBonusExternal: Double = 0.0
    ) {
        if (weaponAffinities.isEmpty()) return

        val procChance = (BASE_PROC_CHANCE + procBonusExternal).coerceIn(0.0, 1.0)
        if (Random.nextDouble() > procChance) return

        val chosenAffinity = pickWeighted(weaponAffinities) ?: return

        // Inmunidad: si el objetivo es Player con el Vestigio correspondiente, no aplica
        // TODO if (target is Player && RemnantSpecialEffectHandler.isImmuneToAffinity(target, chosenAffinity)) return

        applyMark(attacker, target, chosenAffinity, hitDamage)
    }

    // ── forceApplyMark (desde FoodListener — sin tirada de proc) ─────────────

    /**
     * Aplica una marca directamente, sin tirada de proc.
     * Usado por el sistema de alimentos (ej. Carne Cruda envenena al jugador).
     * Respeta inmunidades de Vestigios igualmente.
     *
     * @param source  Entidad origen de la marca (puede ser el mismo jugador).
     * @param durationTicks Duración explícita en ticks (ignora config.baseDurationTicks).
     */
    fun forceApplyMark(
        source: LivingEntity,
        target: LivingEntity,
        affinity: Affinity,
        durationTicks: Int
    ) {
        val config = MarkRegistry.configs[affinity] ?: return
        val entityMarks   = activeMarks.getOrPut(target.uniqueId) { mutableMapOf() }
        val entitySources = markSources.getOrPut(target.uniqueId) { mutableMapOf() }

        val existing = entityMarks[affinity]
        if (existing == null) {
            entityMarks[affinity] = AffinityMark(
                affinity        = affinity,
                durationTicks   = durationTicks,
                sourceHitDamage = 0.0  // alimentos no tienen hitDamage de referencia
            )
        } else {
            // Reiniciar duración (misma regla que reaplicar desde combate)
            existing.durationTicks = durationTicks.coerceAtMost(AffinityMark.MAX_DURATION_TICKS)
        }

        entitySources[affinity] = source.uniqueId
    }

    // ── Aplicación interna ────────────────────────────────────────────────────

    private fun applyMark(attacker: LivingEntity, target: LivingEntity, affinity: Affinity, hitDamage: Double) {
        val config        = MarkRegistry.configs[affinity] ?: return
        val entityMarks   = activeMarks.getOrPut(target.uniqueId) { mutableMapOf() }
        val entitySources = markSources.getOrPut(target.uniqueId) { mutableMapOf() }

        val existing = entityMarks[affinity]
        if (existing == null) {
            entityMarks[affinity] = AffinityMark(
                affinity        = affinity,
                durationTicks   = config.baseDurationTicks,
                sourceHitDamage = hitDamage
            )
        } else {
            existing.durationTicks = (existing.durationTicks + AffinityMark.REAPPLY_BONUS_TICKS)
                .coerceAtMost(AffinityMark.MAX_DURATION_TICKS)
            existing.sourceHitDamage = hitDamage
        }

        entitySources[affinity] = attacker.uniqueId
    }

    private fun pickWeighted(weighted: List<Pair<Affinity, Double>>): Affinity? {
        val total = weighted.sumOf { it.second }
        if (total <= 0.0) return null

        var roll = Random.nextDouble() * total
        for ((affinity, weight) in weighted) {
            if (roll < weight) return affinity
            roll -= weight
        }
        return weighted.last().first
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    fun getActiveMarks(entity: LivingEntity): Map<Affinity, AffinityMark> =
        activeMarks[entity.uniqueId] ?: emptyMap()

    fun hasMark(entity: LivingEntity, affinity: Affinity): Boolean =
        activeMarks[entity.uniqueId]?.containsKey(affinity) == true

    fun getMarkSource(entity: LivingEntity, affinity: Affinity): UUID? =
        markSources[entity.uniqueId]?.get(affinity)

    fun clearEntity(entity: LivingEntity) {
        activeMarks.remove(entity.uniqueId)
        markSources.remove(entity.uniqueId)
    }

    // ── Tick global ───────────────────────────────────────────────────────────

    fun tickAll(entitiesProvider: () -> List<LivingEntity>) {
        val entities = entitiesProvider().associateBy { it.uniqueId }

        val iterator = activeMarks.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, marks) = iterator.next()
            val entity = entities[uuid]

            if (entity == null || entity.isDead) {
                iterator.remove()
                markSources.remove(uuid)
                continue
            }

            val sources = markSources[uuid]

            val markIterator = marks.entries.iterator()
            while (markIterator.hasNext()) {
                val (affinity, mark) = markIterator.next()
                val config     = MarkRegistry.configs[affinity]
                val sourceUuid = sources?.get(affinity)
                val source     = sourceUuid?.let { entities[it] }

                if (config != null) MarkEffects.applyTick(entity, source, mark, config)

                mark.durationTicks -= 20
                if (mark.durationTicks <= 0) {
                    markIterator.remove()
                    sources?.remove(affinity)
                    MarkEffects.onMarkExpire(entity, affinity)
                }
            }

            if (marks.isEmpty()) {
                iterator.remove()
                markSources.remove(uuid)
            }
        }
    }

    fun computeArmorAffinityMitigation(target: LivingEntity, affinity: Affinity): Double {
        val equipment = target.equipment ?: return 0.0
        val pieces = listOf(equipment.helmet, equipment.chestplate, equipment.leggings, equipment.boots)

        var sumPct = 0.0
        for (piece in pieces) {
            val pieceAffinities = PlayerStats.readArmorPieceAffinities(piece) ?: continue
            val match = pieceAffinities.firstOrNull { it.first == affinity }
            if (match != null) sumPct += match.second / 100.0
        }

        return (sumPct / 4.0).coerceIn(0.0, 1.0)
    }
}
