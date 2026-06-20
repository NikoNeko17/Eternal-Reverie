package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.affinities.AffinityMark
import com.nikoneko.eternalReverie.affinities.AffinityMarkManager
import com.nikoneko.eternalReverie.affinities.MarkRegistry
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

/**
 * Resuelve un golpe (cuerpo a cuerpo o de proyectil) sobre una víctima,
 * aplicando mitigación por Defensa, crítico, Exposición (Fragilidad), Robo de
 * Vida (Sangre), y proc de afinidades. Reutilizado tanto por
 * PlayerListeners.onEntityDamage (melee) como por BulletProjectile/BowListeners
 * (proyectiles), para no duplicar la fórmula ni el riesgo de desincronizarla.
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

        var finalDamage = (rawDamage
            * (1 + attackerEquipment.strengthMultiplier)
            * (1 + attackerEquipment.critDamageMultiplier * isCrit)) * damageMitigation

        // Fragilidad (Exposición): si la VÍCTIMA tiene esta Marca activa, recibe
        // daño final aumentado en este golpe.
        if (AffinityMarkManager.hasMark(victim, Affinity.FRAGILIDAD)) {
            val config = MarkRegistry.configs[Affinity.FRAGILIDAD]
            if (config != null) {
                finalDamage *= (1.0 + config.effectValue)
            }
        }

        PlayerStats.setCurrentHp(victim, victimHealth - finalDamage)

        // Sangre (Robo de Vida): si la VÍCTIMA tiene esta Marca activa, el ATACANTE
        // de ESTE golpe se cura un % del daño infligido (no necesariamente quien
        // originalmente aplicó la Marca, sino quien la está aprovechando ahora).
        if (AffinityMarkManager.hasMark(victim, Affinity.SANGRE)) {
            val config = MarkRegistry.configs[Affinity.SANGRE]
            if (config != null) {
                val healAmount = finalDamage * config.effectValue
                val attackerCurrentHp = PlayerStats.getCurrentHp(attacker)
                val attackerMaxHp = PlayerStats.getMaxHp(attacker)
                PlayerStats.setCurrentHp(attacker, (attackerCurrentHp + healAmount).coerceAtMost(attackerMaxHp))
            }
        }

        AffinityMarkManager.onHit(
            attacker = attacker,
            target = victim,
            weaponAffinities = weaponAffinities,
            hitDamage = finalDamage
        )

        return finalDamage
    }
}
