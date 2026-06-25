package com.nikoneko.eternalReverie.remnants

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * GUI de Espacios de Vestigio. Mismo patrón que CraftingGuiHolder: los slots
 * desbloqueados son interactivos (aceptan/quitan Vestigios), los bloqueados
 * (por encima de unlockedSlots) se muestran como Barrier decorativo.
 *
 * 9 slots máximos en una sola fila central de un inventario de 27 (igual
 * estética que la mesa de fabricación).
 */
class RemnantGuiHolder(val player: Player) : InventoryHolder {

    companion object {
        const val SIZE = 9
        val TITLE: Component = Component.text("Espacios de Vestigio", NamedTextColor.DARK_GRAY)

        // Los 9 slots de Vestigio ocupan la fila central (9-17).
        val VESTIGIO_SLOTS = (0..9).toList()
    }

    private lateinit var inventory: Inventory

    init {
        inventory = Bukkit.createInventory(this, SIZE, TITLE)
        refresh()
    }

    override fun getInventory(): Inventory = inventory

    fun refresh() {
        val unlocked = RemnantSlotManager.getUnlockedSlots(player)
        val equipped = RemnantSlotManager.getEquipped(player)

        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val fillerMeta = filler.itemMeta
        fillerMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
        filler.itemMeta = fillerMeta

        for ((index, slot) in VESTIGIO_SLOTS.withIndex()) {
            val slotNumber = index + 1 // 1-indexed para comparar contra unlocked

            when {
                slotNumber > unlocked -> {
                    inventory.setItem(slot, lockedSlotItem())
                }
                index < equipped.size -> {
                    val eq = equipped[index]
                    inventory.setItem(slot, RemnantItemFactory.create(eq.type, eq.level))
                }
                else -> {
                    inventory.setItem(slot, emptySlotItem())
                }
            }
        }

        // Resto de la GUI decorativo
        for (slot in 0 until SIZE) {
            if (slot in VESTIGIO_SLOTS) continue
            inventory.setItem(slot, filler)
        }
    }

    private fun lockedSlotItem(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta.displayName(
            Component.text("Espacio Bloqueado", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(
            listOf(
                Component.text("Desbloqueable mediante contenido", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("especial de expediciones.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        )
        item.itemMeta = meta
        return item
    }

    private fun emptySlotItem(): ItemStack {
        val item = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(
            Component.text("Espacio Vacío", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(
            listOf(
                Component.text("Colocá un Vestigio acá.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        )
        item.itemMeta = meta
        return item
    }
}
