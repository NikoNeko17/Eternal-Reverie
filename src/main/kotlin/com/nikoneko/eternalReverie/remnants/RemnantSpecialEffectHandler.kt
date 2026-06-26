package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.Player
import kotlin.random.Random

/**
 * Punto de entrada único para que los sistemas externos consulten efectos
 * especiales de Vestigios activos en un jugador.
 *
 * Cada función itera los Vestigios equipados buscando el RemnantSpecialEffect
 * relevante, sin que CombatResolver o MarkEffects necesiten saber nada de
 * la estructura interna de los Vestigios.
 */
object RemnantSpecialEffectHandler {

    // ── AttackEvasion ─────────────────────────────────────────────────────────

    /**
     * Llamar desde CombatResolver.resolveHit() ANTES de calcular daño.
     * Si devuelve true, el hit se ignora completamente (daño = 0).
     *
     * Si el jugador tiene varios Vestigios con AttackEvasion, las chances
     * se suman (no se multiplican), con techo en 1.0.
     */
    fun shouldEvadeAttack(player: Player): Boolean {
        val totalChance = RemnantSlotManager.getEquipped(player).sumOf { equipped ->
            equipped.type.data.specialEffects
                .filterIsInstance<RemnantSpecialEffect.AttackEvasion>()
                .sumOf { it.chanceAt(equipped.level, equipped.type.data.maxLevel) }
        }.coerceAtMost(1.0)

        return totalChance > 0.0 && Random.nextDouble() < totalChance
    }

    // ── AffinityImmunity ──────────────────────────────────────────────────────

    /**
     * Llamar desde AffinityMarkManager.onHit() / forceApplyMark() antes de
     * aplicar una marca al jugador víctima.
     * Si devuelve true, la marca no se aplica.
     */
    fun isImmuneToAffinity(player: Player, affinity: Affinity): Boolean {
        return RemnantSlotManager.getEquipped(player).any { equipped ->
            equipped.type.data.specialEffects
                .filterIsInstance<RemnantSpecialEffect.AffinityImmunity>()
                .any { it.affinity == affinity }
        }
    }

    // ── AffinityWeaponDamageBonus ─────────────────────────────────────────────

    /**
     * Llamar desde CombatResolver.resolveHit() DESPUÉS de calcular finalDamage.
     * Devuelve el daño adicional total a sumar.
     *
     * Fórmula por efecto: bonusDamage = finalDamage × multiplier × affinityWeight
     * donde affinityWeight es el peso normalizado (0.0–1.0) de la afinidad
     * del efecto en el arma usada.
     *
     * @param weaponAffinities Lista de (Affinity, weight) del arma atacante,
     *   igual formato que el que ya pasa CombatResolver.
     */
    fun getAffinityDamageBonus(
        player: Player,
        finalDamage: Double,
        weaponAffinities: List<Pair<Affinity, Double>>
    ): Double {
        val affinityWeightMap = weaponAffinities.toMap()

        return RemnantSlotManager.getEquipped(player).sumOf { equipped ->
            equipped.type.data.specialEffects
                .filterIsInstance<RemnantSpecialEffect.AffinityWeaponDamageBonus>()
                .sumOf { effect ->
                    val weight = affinityWeightMap[effect.affinity] ?: 0.0
                    val multiplier = effect.multiplierAt(equipped.level, equipped.type.data.maxLevel)
                    finalDamage * multiplier * weight
                }
        }
    }
}
