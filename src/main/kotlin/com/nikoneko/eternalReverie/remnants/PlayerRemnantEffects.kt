package com.nikoneko.eternalReverie.remnants

import org.bukkit.entity.Player

/**
 * Suma los efectos de los Vestigios equipados y los aplica. Por ahora solo
 * cubre Vitalidad (VITALIDAD_MENOR, prueba de concepto); a medida que se
 * agreguen más VestigioType con otras categorías, este objeto crece con un
 * "when" similar a MarkEffects.
 */
object PlayerRemnantEffects {

    /** Suma de bonus de Vitalidad de TODOS los Vestigios equipados (categoría ESTADISTICA). */
    fun computeVitalityBonus(player: Player): Double {
        return RemnantSlotManager.getEquipped(player)
            .filter { it.type == RemnantType.VITALIDAD_MENOR }
            .sumOf { it.type.data.valueAt(it.level) }
    }

    /** Llamar cada vez que cambia el set de Vestigios equipados (equip/unequip). */
    fun recalculate(player: Player) {
        val vitalityBonus = computeVitalityBonus(player)
        com.nikoneko.eternalReverie.player.PlayerStats.recalculateMaxHp(player, vitalityBonus)
    }
}
