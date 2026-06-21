package com.nikoneko.eternalReverie.crafting

import com.nikoneko.eternalReverie.items.ItemFactory
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
import java.util.UUID

class CraftingGuiListener : Listener {

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder
        if (holder is CraftingGuiHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is CraftingGuiHolder) return

        val clickedInv = event.clickedInventory

        // Cualquier click cuyo inventario "clickeado" sea la GUI custom (o sea null,
        // lo cual pasa con clicks fuera del inventario) se cancela primero por defecto.
        // Esto cubre NUMBER_KEY, SWAP_OFFHAND, DOUBLE_CLICK, y cualquier ClickType
        // que no hayamos previsto explícitamente.
        if (clickedInv == event.inventory) {
            event.isCancelled = true
        }

        // NUMBER_KEY (atajo 1-9) intercambia con el hotbar sin usar el cursor;
        // ya está cancelado arriba, pero lo cortamos explícito para no procesar nada más.
        if (event.click == ClickType.NUMBER_KEY) {
            return
        }

        // Double click siempre cancelado (placeholder para futuro "llenado rápido")
        if (event.click == ClickType.DOUBLE_CLICK) {
            event.isCancelled = true
            return
        }

        val player = event.whoClicked as? Player ?: return

        // Click en el inventario del jugador (parte de abajo)
        if (clickedInv != null && clickedInv != event.inventory) {
            handlePlayerInventoryClick(event, holder, player)
            return
        }

        if (clickedInv == null) return // click fuera del inventario, ya cancelado arriba

        // Click en la GUI custom (parte de arriba)
        handleGuiClick(event, holder, player)
    }

    // --- Click en inventario del jugador: enviar material/plano/catalizador a su slot ---
    private fun handlePlayerInventoryClick(
        event: InventoryClickEvent,
        holder: CraftingGuiHolder,
        player: Player
    ) {
        val clicked = event.currentItem ?: return

        val materialType = ItemFactory.readMaterialType(clicked)
        if (materialType != null) {
            sendToMatrix(event, holder, player, clicked)
            return
        }

        val blueprint = ItemFactory.readBlueprintData(clicked)
        if (blueprint != null) {
            sendToUniqueSlot(
                event, holder, player, clicked,
                slot = CraftingGuiHolder.SLOT_BLUEPRINT
            )
            return
        }

        val catalystType = ItemFactory.readCatalystType(clicked)
        if (catalystType != null) {
            sendToUniqueSlot(
                event, holder, player, clicked,
                slot = CraftingGuiHolder.SLOT_CATALYST
            )
            return
        }

        // Ítem no reconocido (ni material, ni plano, ni catalizador):
        // comportamiento vanilla normal, no tocar el evento.
    }

    private fun sendToMatrix(
        event: InventoryClickEvent,
        holder: CraftingGuiHolder,
        player: Player,
        clicked: ItemStack
    ) {
        event.isCancelled = true

        val emptySlot = holder.findEmptyMatrixSlot()
        if (emptySlot == null) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val single = clicked.clone()
        single.amount = 1
        holder.inventory.setItem(emptySlot, single)
        decrementOrClear(event, clicked)

        holder.updateCraftButton()
        updatePreview(holder)
        player.updateInventory()
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f)
    }

    // Plano/Catalizador: solo se envían si el slot de destino está vacío.
    // Si ya está ocupado, NO se sobreescribe (evita voidear lo que ya había puesto).
    private fun sendToUniqueSlot(
        event: InventoryClickEvent,
        holder: CraftingGuiHolder,
        player: Player,
        clicked: ItemStack,
        slot: Int
    ) {
        event.isCancelled = true

        if (holder.inventory.getItem(slot) != null) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage("§cEse slot ya está ocupado, retiralo primero.")
            return
        }

        val single = clicked.clone()
        single.amount = 1
        holder.inventory.setItem(slot, single)
        decrementOrClear(event, clicked)

        holder.updateCraftButton()
        updatePreview(holder)
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
        holder: CraftingGuiHolder,
        player: Player
    ) {
        when (event.slot) {

            in CraftingGuiHolder.MATRIX_SLOTS -> handleMatrixSlotClick(event, holder, player)

            CraftingGuiHolder.SLOT_BLUEPRINT -> {
                handleUniqueSlotClick(
                    event, holder, player,
                    slot = CraftingGuiHolder.SLOT_BLUEPRINT,
                    isValid = { ItemFactory.readBlueprintData(it) != null }
                )
            }

            CraftingGuiHolder.SLOT_CATALYST -> {
                handleUniqueSlotClick(
                    event, holder, player,
                    slot = CraftingGuiHolder.SLOT_CATALYST,
                    isValid = { ItemFactory.readCatalystType(it) != null }
                )
            }

            CraftingGuiHolder.SLOT_CRAFT_BUTTON -> {
                attemptCraft(holder, player)
            }

            else -> {
                // Slots decorativos, preview, o cualquier otro: nunca aceptan nada.
                // El evento ya está cancelado en onClick, así que esto es solo
                // un resguardo explícito; no se ejecuta lógica de colocación aquí.
            }
        }
    }

    // Slot de matriz: solo acepta MaterialType válido desde el cursor.
    // Si el slot tiene algo, el click lo retira y lo devuelve al jugador.
    private fun handleMatrixSlotClick(
        event: InventoryClickEvent,
        holder: CraftingGuiHolder,
        player: Player
    ) {
        val current = holder.inventory.getItem(event.slot)
        val cursor = event.cursor

        if (current != null) {
            returnSingleItemToPlayer(player, current)
            holder.inventory.setItem(event.slot, null)
            holder.updateCraftButton()
            updatePreview(holder)
            player.updateInventory()
            return
        }

        // Slot vacío: solo aceptar si el cursor es un material válido
        if (cursor.type != Material.AIR && ItemFactory.readMaterialType(cursor) != null) {
            val single = cursor.clone()
            single.amount = 1
            holder.inventory.setItem(event.slot, single)

            if (cursor.amount <= 1) {
                event.cursor.withType(Material.AIR)
            } else {
                cursor.amount -= 1
            }

            holder.updateCraftButton()
            updatePreview(holder)
            player.updateInventory()
        }
        // Si el cursor no es válido (vacío o ítem incorrecto), no se hace nada:
        // el evento ya está cancelado, así que el ítem no se mueve a ningún lado.
    }

    // Slot único (Plano/Catalizador): permite colocar si el cursor trae un ítem válido,
    // o retirar (devolver al jugador) si el slot ya tiene algo y el cursor está vacío.
    private fun handleUniqueSlotClick(
        event: InventoryClickEvent,
        holder: CraftingGuiHolder,
        player: Player,
        slot: Int,
        isValid: (ItemStack?) -> Boolean
    ) {
        val cursor = event.cursor
        val current = holder.inventory.getItem(slot)

        if (current != null) {
            // Retirar lo que hay
            returnSingleItemToPlayer(player, current)
            holder.inventory.setItem(slot, null)
        } else if (cursor.type != Material.AIR && isValid(cursor)) {
            // Colocar 1 unidad del cursor
            val single = cursor.clone()
            single.amount = 1
            holder.inventory.setItem(slot, single)

            if (cursor.amount <= 1) {
                event.cursor.withType(Material.AIR)
            } else {
                cursor.amount -= 1
            }
        }

        holder.updateCraftButton()
        updatePreview(holder)
        player.updateInventory()
    }

    private fun returnSingleItemToPlayer(player: Player, item: ItemStack) {
        val leftover = player.inventory.addItem(item)
        for (extra in leftover.values) {
            player.world.dropItem(player.location, extra)
        }
    }

    // --- Preview ---
    private fun updatePreview(holder: CraftingGuiHolder) {
        val blueprint = ItemFactory.readBlueprintData(holder.getBlueprintItem())
        if (blueprint == null) {
            holder.updatePreview(null)
            return
        }

        val materials = holder.getMaterialItems()
            .mapNotNull { ItemFactory.readMaterialType(it) }

        val previewItem = when (blueprint.itemType) {
            com.nikoneko.eternalReverie.items.ItemType.WEAPON ->
                CraftingCalculator.buildPreviewItem(blueprint, materials)
            com.nikoneko.eternalReverie.items.ItemType.ARMOR ->
                CraftingCalculator.buildArmorPreviewItem(blueprint, materials)
        }
        holder.updatePreview(previewItem)
    }

    // --- Crafteo ---
    private fun attemptCraft(holder: CraftingGuiHolder, player: Player) {
        val blueprint = ItemFactory.readBlueprintData(holder.getBlueprintItem())
        if (blueprint == null) {
            player.sendMessage("§cFalta colocar un plano.")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val materials = holder.getMaterialItems()
            .mapNotNull { ItemFactory.readMaterialType(it) }

        if (materials.isEmpty()) {
            player.sendMessage("§cFalta colocar al menos un material.")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }
        
        val totalCost = materials.sumOf { it.data.fabricationCost }
        if (!com.nikoneko.eternalReverie.economy.CurrencyManager.tryRemoveBalance(player, totalCost)) {
            val balance = com.nikoneko.eternalReverie.economy.CurrencyManager.getBalance(player)
            player.sendMessage("§cNo tenés suficiente Chatarra (necesitás $totalCost, tenés $balance).")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val catalystType = ItemFactory.readCatalystType(holder.getCatalystItem())

        val resultItem = when (blueprint.itemType) {
            com.nikoneko.eternalReverie.items.ItemType.WEAPON ->
                CraftingCalculator.buildFinalWeapon(
                    blueprint = blueprint,
                    materials = materials,
                    catalystType = catalystType,
                    instanceUuid = UUID.randomUUID()
                )
            com.nikoneko.eternalReverie.items.ItemType.ARMOR ->
                CraftingCalculator.buildFinalArmor(
                    blueprint = blueprint,
                    materials = materials,
                    catalystType = catalystType,
                    instanceUuid = UUID.randomUUID()
                )
        }

        // Consumir todo: materiales, plano y catalizador
        holder.clearAll()
        holder.updateCraftButton()
        holder.updatePreview(null)

        // Dar el ítem resultante
        val leftover = player.inventory.addItem(resultItem)
        for (extra in leftover.values) {
            player.world.dropItem(player.location, extra)
        }

        player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 1f)
        player.sendMessage("§a¡Has fabricado un nuevo equipamiento!")
    }

    // --- Cierre de la GUI: devolver todo lo que quede ---
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        if (holder !is CraftingGuiHolder) return

        val player = event.player as? Player ?: return

        val itemsToReturn = mutableListOf<ItemStack>()
        for (slot in CraftingGuiHolder.MATRIX_SLOTS) {
            holder.inventory.getItem(slot)?.let { itemsToReturn.add(it) }
        }
        holder.getBlueprintItem()?.let { itemsToReturn.add(it) }
        holder.getCatalystItem()?.let { itemsToReturn.add(it) }

        for (item in itemsToReturn) {
            val leftover = player.inventory.addItem(item)
            for (extra in leftover.values) {
                val dropped = player.world.dropItem(player.location, extra)
                dropped.owner = player.uniqueId
                // El despawn vanilla por defecto ya aplica (5 min) si no se modifica.
            }
        }
    }
}