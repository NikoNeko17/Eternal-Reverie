package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import java.util.UUID
import kotlin.random.Random

/**
 * Mantiene en memoria las Marcas activas por entidad (jugador o NPC).
 * No persiste entre reinicios; las Marcas son efectos de combate de corta duración,
 * coherente con que los NPCs tampoco persisten (confirmado por el usuario).
 */
object AffinityMarkManager {

    // entityUuid -> (Affinity -> Marca activa)
    private val activeMarks: MutableMap<UUID, MutableMap<Affinity, AffinityMark>> = mutableMapOf()

    private const val BASE_PROC_CHANCE = 0.20      // 20% base de intentar proc por hit
    private const val BASE_EXTRA_STACK_CHANCE = 0.20 // 20% base de +1 stack extra al aplicar

    /**
     * Llamar en cada hit exitoso de un atacante con afinidades en su arma.
     * `weaponAffinities` son los pares (Affinity, %normalizado) ya calculados al craftear.
     * `procBonusExternal` permite sumar bonos externos al 20% base (ej. futuros buffs).
     */
    fun onHit(
        target: LivingEntity,
        weaponAffinities: List<Pair<Affinity, Double>>,
        procBonusExternal: Double = 0.0
    ) {
        if (weaponAffinities.isEmpty()) return

        val procChance = (BASE_PROC_CHANCE + procBonusExternal).coerceIn(0.0, 1.0)
        if (Random.nextDouble() > procChance) return

        val chosenAffinity = pickWeighted(weaponAffinities) ?: return
        applyMark(target, chosenAffinity)
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

    private fun applyMark(target: LivingEntity, affinity: Affinity, extraStackBonus: Double = 0.0) {
        val config = MarkRegistry.configs[affinity] ?: return
        val entityMarks = activeMarks.getOrPut(target.uniqueId) { mutableMapOf() }

        val gainedStacks = if (Random.nextDouble() < (BASE_EXTRA_STACK_CHANCE + extraStackBonus)) 1 else 0

        val existing = entityMarks[affinity]
        if (existing == null) {
            entityMarks[affinity] = AffinityMark(
                affinity = affinity,
                stacks = gainedStacks,
                durationTicks = config.baseDurationTicks
            )
        } else {
            existing.stacks += gainedStacks
            existing.durationTicks = (existing.durationTicks + AffinityMark.REAPPLY_BONUS_TICKS)
                .coerceAtMost(AffinityMark.MAX_DURATION_TICKS)
        }
    }

    fun getActiveMarks(entity: LivingEntity): Map<Affinity, AffinityMark> =
        activeMarks[entity.uniqueId] ?: emptyMap()

    fun hasMark(entity: LivingEntity, affinity: Affinity): Boolean =
        activeMarks[entity.uniqueId]?.containsKey(affinity) == true

    fun clearEntity(entity: LivingEntity) {
        activeMarks.remove(entity.uniqueId)
    }

    /**
     * Tick global (llamar 1 vez por segundo desde un scheduler). Decrementa duración,
     * aplica el efecto periódico de cada Marca activa (mitigado por la Tenacidad/afinidad
     * de armadura del objetivo), y elimina las que expiraron.
     */
    fun tickAll(entitiesProvider: () -> List<LivingEntity>) {
        val entities = entitiesProvider().associateBy { it.uniqueId }

        val iterator = activeMarks.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, marks) = iterator.next()
            val entity = entities[uuid]

            if (entity == null || entity.isDead) {
                iterator.remove()
                continue
            }

            val markIterator = marks.entries.iterator()
            while (markIterator.hasNext()) {
                val (affinity, mark) = markIterator.next()
                val config = MarkRegistry.configs[affinity]

                if (config != null) {
                    MarkEffects.applyTick(entity, mark, config)
                }

                mark.durationTicks -= 20 // 1 segundo
                if (mark.durationTicks <= 0) {
                    markIterator.remove()
                }
            }

            if (marks.isEmpty()) {
                iterator.remove()
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
