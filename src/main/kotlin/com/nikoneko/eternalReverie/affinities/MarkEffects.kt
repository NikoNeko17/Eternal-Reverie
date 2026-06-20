package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.entity.LivingEntity

/**
 * Aplica el efecto periódico (1 vez por segundo, vía AffinityMarkManager.tickAll)
 * de cada Marca activa sobre una entidad. Solo Sangre (Hemorragia) y Fuego
 * (Incineración) están implementadas como prueba de concepto end-to-end;
 * las demás están registradas en MarkRegistry pero su rama del when no hace nada
 * todavía (placeholder explícito, no un error).
 */
object MarkEffects {

    fun applyTick(target: LivingEntity, mark: AffinityMark, config: MarkConfig) {
        val mitigation = AffinityMarkManager.computeArmorAffinityMitigation(target, mark.affinity)
        val effectiveMultiplier = (1.0 - mitigation).coerceIn(0.0, 1.0)

        when (mark.affinity) {

            Affinity.SANGRE -> applyBleed(target, mark, config, effectiveMultiplier)

            Affinity.FUEGO -> applyBurn(target, mark, config, effectiveMultiplier)

            // --- Placeholders: estructura lista, efecto a implementar después ---
            Affinity.HIELO -> { /* TODO: Congelación, -%velocidad de movimiento por stack */ }
            Affinity.ELECTRICIDAD -> { /* TODO: Sobrecarga, -stamina máxima temporal por stack */ }
            Affinity.VENENO -> { /* TODO: Intoxicación, -%regeneración HP/Stamina por stack */ }
            Affinity.ATADURA -> { /* TODO: Restricción, -%movilidad y +cooldown por stack */ }
            Affinity.FRAGILIDAD -> { /* TODO: Exposición, +%daño recibido por stack (leído en PlayerListeners al calcular finalDamage) */ }
        }
    }

    private fun applyBleed(
        target: LivingEntity,
        mark: AffinityMark,
        config: MarkConfig,
        effectiveMultiplier: Double
    ) {
        val maxHp = PlayerStats.getMaxHp(target)
        val pctPerStack = config.effectPerStack // 0.02 = 2% del HP máximo, por stack
        val rawDamage = maxHp * pctPerStack * mark.stacks
        val mitigatedDamage = rawDamage * effectiveMultiplier

        val currentHp = PlayerStats.getCurrentHp(target)
        PlayerStats.setCurrentHp(target, (currentHp - mitigatedDamage).coerceAtLeast(0.0))
    }

    private fun applyBurn(
        target: LivingEntity,
        mark: AffinityMark,
        config: MarkConfig,
        effectiveMultiplier: Double
    ) {
        val flatPerStack = config.effectPerStack // 1.5 daño plano, por stack
        val rawDamage = flatPerStack * mark.stacks
        val mitigatedDamage = rawDamage * effectiveMultiplier

        val currentHp = PlayerStats.getCurrentHp(target)
        PlayerStats.setCurrentHp(target, (currentHp - mitigatedDamage).coerceAtLeast(0.0))
    }
}
