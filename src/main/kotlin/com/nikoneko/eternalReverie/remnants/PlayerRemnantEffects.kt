package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.food.FoodStat
import org.bukkit.entity.Player

/**
 * Calcula los bonus de stats que aportan los Vestigios equipados.
 *
 * El resultado se expresa como Map<FoodStat, Double> — mismo tipo que
 * FoodEffectManager.getStatMultipliers — para que PlayerStats.computeEquipmentStats
 * los sume con la misma lógica sin distinguir la fuente.
 *
 * Convención de valores:
 *   - VITALIDAD / RESISTENCIA: valor absoluto (se suma al pool base, ej. +30 HP)
 *   - Resto de stats:          multiplier porcentual (0.10 = +10%)
 */
object PlayerRemnantEffects {

    /**
     * Suma de efectos de TODOS los Vestigios equipados, agrupados por stat.
     * Si dos Vestigios afectan la misma stat, sus valores se suman.
     */
    fun getStatBonuses(player: Player): Map<FoodStat, Double> {
        val result = mutableMapOf<FoodStat, Double>()

        for (equipped in RemnantSlotManager.getEquipped(player)) {
            for (effect in equipped.type.data.effects) {
                val value = effect.valueAt(equipped.level)
                result[effect.stat] = (result[effect.stat] ?: 0.0) + value
            }
        }

        return result
    }

    /**
     * Llama a recalculateMaxHp pasando el bonus de Vitalidad como extraVitalityBonus,
     * igual que antes — pero ahora extrae el valor desde el mapa en vez de hardcodear
     * el filtro a VITALIDAD_MENOR.
     */
    fun recalculate(player: Player) {
        val bonuses = getStatBonuses(player)
        val vitalityBonus = bonuses[FoodStat.VITALIDAD] ?: 0.0
        com.nikoneko.eternalReverie.player.PlayerStats.recalculateMaxHp(player, vitalityBonus)
    }
}
