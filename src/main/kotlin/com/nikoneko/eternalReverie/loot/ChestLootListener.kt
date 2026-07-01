package com.nikoneko.eternalReverie.loot

import com.nikoneko.eternalReverie.EternalReverie
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Rellena cofres marcados con loot generado por ChestLootGenerator — PERO
 * de forma independiente por jugador: cada jugador que abre el cofre ve su
 * propia copia del loot, generada la primera vez que lo abre.
 *
 * Cómo funciona:
 * 1. Al colocar un cofre marcado (ver onChestPlace), se le asigna un
 *    CHEST_ID_KEY único (UUID) además del LOOT_PENDING_KEY con el areaId.
 * 2. Al hacer click derecho sobre ese cofre (onChestInteract), se cancela
 *    la apertura default y en su lugar se le muestra al jugador un
 *    Inventory propio, generado on-demand vía PerPlayerLootManager.
 * 3. El inventario mostrado NO es el blockInventory real del cofre — es
 *    un inventario custom en memoria por (chestId, jugador). Los cambios
 *    de items (sacar/mover) se reflejan solos porque es el mismo objeto
 *    Inventory guardado en el manager.
 */
class ChestLootListener(val plugin: EternalReverie) : Listener {

    companion object {
        lateinit var LOOT_PENDING_KEY: org.bukkit.NamespacedKey
        lateinit var CHEST_ID_KEY: org.bukkit.NamespacedKey

        fun init(plugin: EternalReverie) {
            LOOT_PENDING_KEY = org.bukkit.NamespacedKey(plugin, "chest_loot_pending")
            CHEST_ID_KEY = org.bukkit.NamespacedKey(plugin, "chest_loot_id")
        }

        fun markChestForLoot(chest: Chest, areaId: String) {
            chest.persistentDataContainer.set(LOOT_PENDING_KEY, PersistentDataType.STRING, areaId)
            // Asignamos el chestId acá mismo, en colocación — así sobrevive
            // reinicios de servidor sin depender de ChunkLoadEvent.
            if (!chest.persistentDataContainer.has(CHEST_ID_KEY, PersistentDataType.STRING)) {
                chest.persistentDataContainer.set(CHEST_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString())
            }
            chest.update()
        }

        fun getChestId(chest: Chest): UUID? {
            val raw = chest.persistentDataContainer.get(CHEST_ID_KEY, PersistentDataType.STRING) ?: return null
            return runCatching { UUID.fromString(raw) }.getOrNull()
        }
    }

    // Al colocar un cofre: si el ítem colocado tiene un nombre custom que
    // coincide EXACTO con una key de areas.json, se marca para loot y se
    // le asigna un chestId único.
    @EventHandler
    fun onChestPlace(event: BlockPlaceEvent) {
        if (event.block.type != Material.CHEST) return
        event.itemInHand.let {
            if (it.itemMeta.hasDisplayName()) it.displayName() else null
        } ?: return

        val plainName = PlainTextComponentSerializer
            .plainText().serialize(event.itemInHand.itemMeta.displayName() ?: return)

        if (AreaLootRegistry.get(plainName) == null) return

        val chestState = event.block.state as? Chest ?: return

        markChestForLoot(chestState, plainName)
    }

    // Intercepta la apertura del cofre: si está marcado para loot, cancela
    // la apertura default y muestra el inventario per-player del jugador.
    @EventHandler
    fun onChestInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.CHEST) return

        val chest = block.state as? Chest ?: return
        val areaId = chest.persistentDataContainer.get(LOOT_PENDING_KEY, PersistentDataType.STRING)
        val chestId = getChestId(chest)

        // Cofre normal (no marcado para loot instanciado): dejar comportamiento default.
        if (areaId == null || chestId == null) return

        event.isCancelled = true

        val player = event.player
        val inventory = PerPlayerLootManager.getOrCreate(
            chestId = chestId,
            player = player.uniqueId,
            title = "Botín"
        ) {
            buildLootInventory(areaId)
        }

        player.openInventory(inventory)
    }

    private fun buildLootInventory(areaId: String): org.bukkit.inventory.Inventory {
        val loot = ChestLootGenerator.generate(areaId)
        val inventory = Bukkit.createInventory(null, 27, Component.text("Botín"))

        for (stack in loot) {
            repeat(stack.amount) {
                val single = stack.clone().apply { amount = 1 }
                placeItemScattered(inventory, single)
            }
        }

        return inventory
    }

    // Coloca un ítem (de a 1 unidad) en un slot aleatorio del inventario.
    // Si el slot elegido está ocupado por un ítem distinto, reintenta con
    // otro slot no probado todavía. Si está ocupado por el MISMO ítem y
    // tiene lugar en el stack, suma 1 ahí en vez de buscar otro slot.
    // Si ya se probaron todos los slots sin éxito, el cofre está lleno y
    // el ítem se descarta (igual que hacía el Inventory.addItem original).
    private fun placeItemScattered(
        inventory: org.bukkit.inventory.Inventory,
        item: org.bukkit.inventory.ItemStack,
        attemptedSlots: MutableSet<Int> = mutableSetOf()
    ): Boolean {
        val size = inventory.size
        if (attemptedSlots.size >= size) return false

        val slot = (0 until size).filter { it !in attemptedSlots }.random()
        val current = inventory.getItem(slot)

        return when {
            current == null -> {
                inventory.setItem(slot, item)
                true
            }
            current.isSimilar(item) && current.amount < current.maxStackSize -> {
                current.amount += 1
                true
            }
            else -> {
                attemptedSlots.add(slot)
                placeItemScattered(inventory, item, attemptedSlots)
            }
        }
    }
}