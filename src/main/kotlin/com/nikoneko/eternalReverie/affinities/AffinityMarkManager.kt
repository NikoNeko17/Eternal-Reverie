package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.LivingEntity
import java.util.UUID
import kotlin.random.Random

/**
 * Mantiene en memoria las Marcas activas por entidad (jugador o NPC).
 * No persiste entre reinicios (coherente con que los NPCs tampoco persisten).
 * Sin stacks: cada Marca está activa sí/no, su efecto es fijo mientras dure.
 */
object AffinityMarkManager {

    // entityUuid -> (Affinity -> Marca activa)
    private val activeMarks: MutableMap<UUID, MutableMap<Affinity, AffinityMark>> = mutableMapOf()

    // entityUuid -> quién le aplicó cada Marca (necesario para Sangre/Electricidad,
    // que benefician al ATACANTE mientras la Marca esté activa en la víctima).
    private val markSources: MutableMap<UUID, MutableMap<Affinity, UUID>> = mutableMapOf()

    private const val BASE_PROC_CHANCE = 0.20 // 20% base de proc por hit, fijo (no escala con %afinidad)

    /**
     * Llamar en cada hit exitoso de un atacante con afinidades en su arma.
     * `weaponAffinities` son los pares (Affinity, %normalizado) ya calculados al craftear,
     * usados SOLO para decidir cuál Affinity se elige si hay proc (no afectan el % de proc en sí).
     * `hitDamage` es el daño de ESTE golpe puntual, usado por Fuego para su DoT proporcional.
     */
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
        applyMark(attacker, target, chosenAffinity, hitDamage)
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

    private fun applyMark(attacker: LivingEntity, target: LivingEntity, affinity: Affinity, hitDamage: Double) {
        val config = MarkRegistry.configs[affinity] ?: return
        val entityMarks = activeMarks.getOrPut(target.uniqueId) { mutableMapOf() }
        val entitySources = markSources.getOrPut(target.uniqueId) { mutableMapOf() }

        val existing = entityMarks[affinity]
        if (existing == null) {
            entityMarks[affinity] = AffinityMark(
                affinity = affinity,
                durationTicks = config.baseDurationTicks,
                sourceHitDamage = hitDamage
            )
        } else {
            existing.durationTicks = (existing.durationTicks + AffinityMark.REAPPLY_BONUS_TICKS)
                .coerceAtMost(AffinityMark.MAX_DURATION_TICKS)
            existing.sourceHitDamage = hitDamage // Fuego: el DoT se actualiza al daño del último golpe
        }

        entitySources[affinity] = attacker.uniqueId
    }

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

    /**
     * Tick global (llamar 1 vez por segundo desde AffinityTickScheduler). Decrementa
     * duración, aplica el efecto periódico de cada Marca activa (mitigado por la
     * afinidad de armadura del objetivo), y elimina las que expiraron.
     */
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
                val config = MarkRegistry.configs[affinity]
                val sourceUuid = sources?.get(affinity)
                val source = sourceUuid?.let { entities[it] }

                if (config != null) {
                    MarkEffects.applyTick(entity, source, mark, config)
                }

                mark.durationTicks -= 20 // 1 segundo
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

    /**
     * % de mitigación de una Marca específica según la afinidad del SET de armadura
     * del objetivo (suma de %afinidad de cada pieza que la tenga, dividido entre 4).
     * Devuelve un valor 0.0-1.0 para multiplicar como (1 - mitigacion).
     */
    fun computeArmorAffinityMitigation(target: LivingEntity, affinity: Affinity): Double {
        val equipment = target.equipment ?: return 0.0
        val pieces = listOf(equipment.helmet, equipment.chestplate, equipment.leggings, equipment.boots)

        var sumPct = 0.0
        for (piece in pieces) {
            val pieceAffinities = PlayerStats.readArmorPieceAffinities(piece) ?: continue
            val match = pieceAffinities.firstOrNull { it.first == affinity }
            if (match != null) {
                sumPct += match.second / 100.0 // viene normalizado 0-100
            }
        }

        return (sumPct / 4.0).coerceIn(0.0, 1.0)
    }
}
