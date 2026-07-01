package com.nikoneko.eternalReverie.remnants.athanor

import com.nikoneko.eternalReverie.items.ItemFactory
import com.nikoneko.eternalReverie.remnants.RemnantItemFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

/**
 * Listener del Athanor. Mismo patrón que CraftingGuiListener: todo click
 * dentro de la GUI se cancela por defecto, con excepciones explícitas por
 * slot. Al cerrar, se devuelve cualquier ítem puesto (5 materiales + Celda).
 *
 * Deliberadamente SIN preview del resultado — ver AthanorGuiHolder.
 */
class AthanorGuiListener : Listener {

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder
        if (holder is AthanorGuiHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is AthanorGuiHolder) return

        val clickedInv = event.clickedInventory

        if (clickedInv == event.inventory) {
            event.isCancelled = true
        }

        if (event.click == ClickType.NUMBER_KEY) {
            return
        }

        if (event.click == ClickType.DOUBLE_CLICK) {
            event.isCancelled = true
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

    // --- Click en inventario del jugador: enviar material/celda a su slot ---
    private fun handlePlayerInventoryClick(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player
    ) {
        val clicked = event.currentItem ?: return

        val materialType = ItemFactory.readMaterialType(clicked)
        if (materialType != null) {
            sendToMaterialSlot(event, holder, player, clicked)
            return
        }

        if (isEssenceCell(clicked)) {
            sendToEssenceCellSlot(event, holder, player, clicked)
            return
        }

        // Ítem no reconocido: comportamiento vanilla normal, no tocar el evento.
    }

    private fun sendToMaterialSlot(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player,
        clicked: ItemStack
    ) {
        event.isCancelled = true

        val emptySlot = holder.findEmptyMaterialSlot()
        if (emptySlot == null) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val single = clicked.clone()
        single.amount = 1
        holder.inventory.setItem(emptySlot, single)
        decrementOrClear(event, clicked)

        holder.updateSynthesizeButton()
        player.updateInventory()
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f)
    }

    private fun sendToEssenceCellSlot(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player,
        clicked: ItemStack
    ) {
        event.isCancelled = true

        if (holder.getEssenceCellItem() != null) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage("§cYa hay una Celda colocada, retirala primero.")
            return
        }

        val single = clicked.clone()
        single.amount = 1
        holder.inventory.setItem(AthanorGuiHolder.SLOT_ESSENCE_CELL, single)
        decrementOrClear(event, clicked)

        holder.updateSynthesizeButton()
        player.updateInventory()
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f)
    }

    private fun decrementOrClear(event: InventoryClickEvent, clicked: ItemStack) {
        if (clicked.amount <= 1) {
            event.clickedInventory?.setItem(event.slot, null)
        } else {
            clicked.amount -= 1
        }
    }

    // --- Click dentro de la GUI custom ---
    private fun handleGuiClick(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player
    ) {
        when (event.slot) {

            in AthanorGuiHolder.MATERIAL_SLOTS -> handleMaterialSlotClick(event, holder, player)

            AthanorGuiHolder.SLOT_ESSENCE_CELL -> handleEssenceCellSlotClick(event, holder, player)

            AthanorGuiHolder.SLOT_SYNTHESIZE_BUTTON -> attemptSynthesis(holder, player)

            else -> {
                // Slots decorativos: nunca aceptan nada.
            }
        }
    }

    private fun handleMaterialSlotClick(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player
    ) {
        val current = holder.inventory.getItem(event.slot)
        val cursor = event.cursor

        if (current != null) {
            returnSingleItemToPlayer(player, current)
            holder.inventory.setItem(event.slot, null)
            holder.updateSynthesizeButton()
            player.updateInventory()
            return
        }

        if (cursor.type != Material.AIR && ItemFactory.readMaterialType(cursor) != null) {
            val single = cursor.clone()
            single.amount = 1
            holder.inventory.setItem(event.slot, single)

            if (cursor.amount <= 1) {
                event.cursor.withType(Material.AIR)
            } else {
                cursor.amount -= 1
            }

            holder.updateSynthesizeButton()
            player.updateInventory()
        }
    }

    private fun handleEssenceCellSlotClick(
        event: InventoryClickEvent,
        holder: AthanorGuiHolder,
        player: Player
    ) {
        val cursor = event.cursor
        val current = holder.getEssenceCellItem()

        if (current != null) {
            returnSingleItemToPlayer(player, current)
            holder.inventory.setItem(AthanorGuiHolder.SLOT_ESSENCE_CELL, null)
        } else if (cursor.type != Material.AIR && isEssenceCell(cursor)) {
            val single = cursor.clone()
            single.amount = 1
            holder.inventory.setItem(AthanorGuiHolder.SLOT_ESSENCE_CELL, single)

            if (cursor.amount <= 1) {
                event.cursor.withType(Material.AIR)
            } else {
                cursor.amount -= 1
            }
        }

        holder.updateSynthesizeButton()
        player.updateInventory()
    }

    private fun returnSingleItemToPlayer(player: Player, item: ItemStack) {
        val leftover = player.inventory.addItem(item)
        for (extra in leftover.values) {
            player.world.dropItem(player.location, extra)
        }
    }

    private fun isEssenceCell(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(EssenceKeys.ESSENCE_STORED)
    }

    // --- Síntesis ---
    private fun attemptSynthesis(holder: AthanorGuiHolder, player: Player) {
        if (!holder.isReadyToSynthesize()) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage("§cFaltan materiales o la Celda de Esencia no está sellada.")
            return
        }

        val cell = holder.getEssenceCellItem() ?: return
        val sealedRarity = EssenceCellManager.getSealedRarity(cell) ?: return

        val materialItems = holder.getMaterialItems()
        val materialTypes = materialItems.mapNotNull { ItemFactory.readMaterialType(it) }
        if (materialTypes.size != AthanorGuiHolder.MATERIAL_SLOTS.size) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val result = RemnantSynthesizer.synthesize(materialTypes, sealedRarity)

        holder.clearAll()
        holder.updateSynthesizeButton()

        if (player.inventory.firstEmpty() >= 0) {
            player.inventory.addItem(RemnantItemFactory.create(result, sealedRarity))
        } else {
            player.world.dropItemNaturally(player.location, RemnantItemFactory.create(result, sealedRarity))
        }

        player.playSound(player.location, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f)
        player.sendMessage("§d¡Transmutación completada! (${result.primaryEffect?.displayName ?: "Vestigio Neutro"})")
    }

    // --- Cierre de la GUI: devolver todo lo que quede ---
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        if (holder !is AthanorGuiHolder) return

        val player = event.player as? Player ?: return

        val itemsToReturn = mutableListOf<ItemStack>()
        for (slot in AthanorGuiHolder.MATERIAL_SLOTS) {
            holder.inventory.getItem(slot)?.let { itemsToReturn.add(it) }
        }
        holder.getEssenceCellItem()?.let { itemsToReturn.add(it) }

        for (item in itemsToReturn) {
            val leftover = player.inventory.addItem(item)
            for (extra in leftover.values) {
                val dropped = player.world.dropItem(player.location, extra)
                dropped.owner = player.uniqueId
            }
        }
    }
}