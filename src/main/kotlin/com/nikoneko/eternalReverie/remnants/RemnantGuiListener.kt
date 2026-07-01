package com.nikoneko.eternalReverie.remnants

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/**
 * Maneja clicks en la GUI de Espacios de Vestigio. Mismo criterio que
 * CraftingGuiListener: shift-click/drag deshabilitados, todo el evento se
 * cancela primero por defecto dentro de la GUI.
 */
class RemnantGuiListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is RemnantGuiHolder) return

        val clickedInv = event.clickedInventory

        if (clickedInv == event.inventory) {
            event.isCancelled = true
        }

        if (event.click == ClickType.NUMBER_KEY || event.click == ClickType.DOUBLE_CLICK) {
            return
        }

        val player = event.whoClicked as? Player ?: return

        if (clickedInv != null && clickedInv != event.inventory) {
            handlePlayerInventoryClick(event, holder, player)
            return
        }

        if (clickedInv == null) return

        handleGuiClick(event, holder, player)
    }

    // Clic en el inventario del jugador: si trae un Vestigio, lo manda al
    // primer espacio vacío disponible.
    private fun handlePlayerInventoryClick(
        event: InventoryClickEvent,
        holder: RemnantGuiHolder,
        player: Player
    ) {
        val clicked = event.currentItem ?: return
        if (!RemnantItemFactory.isVestigio(clicked)) return
        val type = RemnantItemFactory.recompute(clicked)

        event.isCancelled = true

        val success = RemnantSlotManager.equip(player, type)
        if (!success) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage("§cNo hay espacios libres, o ese Vestigio ya está equipado.")
            return
        }

        if (clicked.amount <= 1) {
            event.clickedInventory?.setItem(event.slot, null)
        } else {
            clicked.amount -= 1
        }

        holder.refresh()
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f)
    }

    // Clic en un slot de Vestigio ya equipado dentro de la GUI: lo desequipa
    // y devuelve el ítem al jugador.
    private fun handleGuiClick(
        event: InventoryClickEvent,
        holder: RemnantGuiHolder,
        player: Player
    ) {
        if (event.slot !in RemnantGuiHolder.VESTIGIO_SLOTS) return

        val current = holder.inventory.getItem(event.slot) ?: return
        if (current.type != Material.PRISMARINE_SHARD) return
        val type = RemnantItemFactory.recompute(current)

        RemnantSlotManager.unequip(player, type)

        val returnedItem: ItemStack = RemnantItemFactory.create(type, type.cellRarity)
        val leftover = player.inventory.addItem(returnedItem)
        for (extra in leftover.values) {
            player.world.dropItem(player.location, extra)
        }

        holder.refresh()
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 0.8f)
    }

    // Al cerrar, no hay nada flotante que devolver (a diferencia de la mesa de
    // fabricación): los Vestigios equipados quedan persistidos en el PDC del
    // jugador, no en la GUI misma.
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        if (holder !is RemnantGuiHolder) return
        // No-op intencional.
    }
}
