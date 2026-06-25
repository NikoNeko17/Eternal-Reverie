package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.items.Keys
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * Maneja los Espacios de Vestigio equipados por jugador. Persistido en el PDC
 * del jugador como PersistentDataType.LIST.strings(), cada entrada con formato
 * "VESTIGIO_ID:nivel" (ej. "VITALIDAD_MENOR:3"), igual patrón que Keys.MATERIALS
 * en las armas.
 *
 * Espacios iniciales: 2. Espacios máximos: 9 (ampliables permanentemente vía
 * contenido especial de expediciones, a futuro).
 */
object RemnantSlotManager {

    const val INITIAL_SLOTS = 2
    const val MAX_SLOTS = 9

    data class EquippedRemnants(val type: RemnantType, val level: Int)

    fun getUnlockedSlots(player: Player): Int =
        player.persistentDataContainer.get(Keys.VESTIGIO_UNLOCKED_SLOTS, PersistentDataType.INTEGER)
            ?: INITIAL_SLOTS

    fun unlockSlot(player: Player) {
        val current = getUnlockedSlots(player)
        if (current >= MAX_SLOTS) return
        player.persistentDataContainer.set(
            Keys.VESTIGIO_UNLOCKED_SLOTS, PersistentDataType.INTEGER, current + 1
        )
    }

    fun getEquipped(player: Player): List<EquippedRemnants> {
        val raw = player.persistentDataContainer.get(Keys.VESTIGIO_EQUIPPED, PersistentDataType.LIST.strings())
            ?: emptyList()

        return raw.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val type = runCatching { RemnantType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val level = parts[1].toIntOrNull() ?: return@mapNotNull null
            EquippedRemnants(type, level)
        }
    }

    private fun saveEquipped(player: Player, equipped: List<EquippedRemnants>) {
        val raw = equipped.map { "${it.type.name}:${it.level}" }
        player.persistentDataContainer.set(Keys.VESTIGIO_EQUIPPED, PersistentDataType.LIST.strings(), raw)
    }

    /** @return true si se pudo equipar (había espacio libre). */
    fun equip(player: Player, type: RemnantType, level: Int): Boolean {
        val current = getEquipped(player)
        val unlockedSlots = getUnlockedSlots(player)

        if (current.size >= unlockedSlots) return false
        if (current.any { it.type == type }) return false // no duplicados del mismo tipo equipado a la vez

        val updated = current + EquippedRemnants(type, level)
        saveEquipped(player, updated)
        PlayerRemnantEffects.recalculate(player)
        return true
    }

    fun unequip(player: Player, type: RemnantType) {
        val current = getEquipped(player)
        val updated = current.filterNot { it.type == type }
        saveEquipped(player, updated)
        PlayerRemnantEffects.recalculate(player)
    }

    fun isEquipped(player: Player, type: RemnantType): Boolean =
        getEquipped(player).any { it.type == type }
}
