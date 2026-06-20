package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.affinities.AffinityMarkManager
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

/**
 * Resuelve un golpe (cuerpo a cuerpo o de proyectil) sobre una víctima,
 * aplicando mitigación por Defensa, crítico, y proc de afinidades.
 * Reutilizado tanto por PlayerListeners.onEntityDamage (melee) como por
 * BulletProjectile.onHit (armas de fuego/arcos), para no duplicar la fórmula
 * ni el riesgo de desincronizarla entre ambos caminos.
 */
object CombatResolver {

    /**
     * @param rawDamage daño base del arma YA calculado (con materiales aplicados),
     *   tomado en el momento relevante (disparo para proyectiles, impacto para melee).
     * @param attackerEquipment stats de equipo del atacante (Fuerza, Crit, etc.)
     * @param weaponAffinities afinidades normalizadas del arma usada, para el proc.
     */
    fun resolveHit(
        attacker: LivingEntity,
        victim: LivingEntity,
        rawDamage: Double,
        attackerEquipment: PlayerStats.EquipmentStats,
        weaponAffinities: List<Pair<Affinity, Double>>
    ): Double {
        val victimDefense = PlayerStats.computeEquipmentStats(victim).defense
        val victimHealth = PlayerStats.getCurrentHp(victim)

        val damageMitigation = 100.0 / (100.0 + victimDefense)
        val isCrit = if (Random.nextDouble() <= attackerEquipment.critChance) 1 else 0

        val finalDamage = (rawDamage
            * (1 + attackerEquipment.strengthMultiplier)
            * (1 + attackerEquipment.critDamageMultiplier * isCrit)) * damageMitigation

        PlayerStats.setCurrentHp(victim, victimHealth - finalDamage)

        AffinityMarkManager.onHit(
            target = victim,
            weaponAffinities = weaponAffinities
        )

        return finalDamage
    }
}
