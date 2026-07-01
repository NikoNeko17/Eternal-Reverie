package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.crafting.MaterialRarity
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.remnants.athanor.SynthesizedRemnant
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    data class EquippedRemnants(val type: SynthesizedRemnant)

    fun getUnlockedSlots(player: Player): Int =
        player.persistentDataContainer.get(Keys.REMNANT_UNLOCKED_SLOTS, PersistentDataType.INTEGER)
            ?: INITIAL_SLOTS

    fun unlockSlot(player: Player) {
        val current = getUnlockedSlots(player)
        if (current >= MAX_SLOTS) return
        player.persistentDataContainer.set(
            Keys.REMNANT_UNLOCKED_SLOTS, PersistentDataType.INTEGER, current + 1
        )
    }

    fun getEquipped(player: Player): List<ItemStack> {
        val pdc = player.persistentDataContainer

        val equippedRemnants = pdc.get(Keys.REMNANT_EQUIPPED, PersistentDataType.LIST.strings()) ?: return emptyList()

        return equippedRemnants.map {
            ItemStackSerializer.deserialize(it) ?: ItemStack(Material.AIR)
        }
    }

    private fun saveEquipped(player: Player, remnants: List<ItemStack>) {
        player.persistentDataContainer.set(Keys.REMNANT_EQUIPPED, PersistentDataType.LIST.strings(), remnants.map { ItemStackSerializer.serialize(it) })
    }

    /** @return true si se pudo equipar (había espacio libre). */
    fun equip(player: Player, type: SynthesizedRemnant): Boolean {
        val current = getEquipped(player)
        val unlockedSlots = getUnlockedSlots(player)

        if (current.size >= unlockedSlots) return false
        if (current.any { RemnantItemFactory.recompute(it).primaryEffect?.displayName == type.primaryEffect?.displayName }) return false

        val updated = getEquipped(player) + RemnantItemFactory.create(type, type.cellRarity)
        saveEquipped(player, updated)
        return true
    }

    fun unequip(player: Player, type: SynthesizedRemnant) {
        val current = getEquipped(player)
        val updated = current.filterNot { RemnantItemFactory.recompute(it) == type }
        saveEquipped(player, updated)
    }

    fun isEquipped(player: Player, type: SynthesizedRemnant): Boolean =
        getEquipped(player).any { RemnantItemFactory.recompute(it) == type }
}
