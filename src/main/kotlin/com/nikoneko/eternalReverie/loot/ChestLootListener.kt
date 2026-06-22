package com.nikoneko.eternalReverie.loot

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType

/**
 * Rellena cofres marcados con loot generado por ChestLootGenerator.
 *
 * TEMPORAL: hasta que exista el sistema de instancias (ASP), el área se
 * detecta por el NOMBRE RAW del cofre (item.itemMeta.displayName como string
 * plano, no Component) al colocarlo: si coincide con una key exacta de
 * areas.json, se marca para loot. Cuando ASP esté listo, lo correcto sería
 * resolver el área desde la instancia/mundo en vez de depender del nombre.
 */
class ChestLootListener : Listener {

    companion object {
        lateinit var LOOT_PENDING_KEY: org.bukkit.NamespacedKey

        fun init(plugin: com.nikoneko.eternalReverie.EternalReverie) {
            LOOT_PENDING_KEY = org.bukkit.NamespacedKey(plugin, "chest_loot_pending")
        }

        fun markChestForLoot(chest: Chest, areaId: String) {
            chest.persistentDataContainer.set(LOOT_PENDING_KEY, PersistentDataType.STRING, areaId)
            chest.update()
        }
    }

    // Al colocar un cofre: si el ítem colocado tiene un nombre custom (yunque,
    // comando, etc.) que coincide EXACTO con una key de areas.json, se marca
    // para que el siguiente ChunkLoadEvent lo rellene.
    @EventHandler
    fun onChestPlace(event: BlockPlaceEvent) {
        if (event.block.type != Material.CHEST) return

        val rawName = event.itemInHand.itemMeta?.let {
            if (it.hasDisplayName()) it.displayName() else null
        } ?: return

        // displayName() (Adventure) devuelve Component; para comparar contra la key
        // raw del JSON usamos el texto plano, sin colores/formato.
        val plainName = event.itemInHand.itemMeta.displayName().toString()


        if (AreaLootRegistry.get(plainName) == null) return

        val chestState = event.block.state as? Chest ?: return
        markChestForLoot(chestState, plainName)
    }

    // Relleno perezoso: cuando el chunk carga, busca cofres marcados y los llena
    // (más robusto que intentar hacerlo al colocar, ya que sobrevive a reinicios
    // de servidor mientras el cofre siga marcado en su PDC).
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        for (blockState in event.chunk.tileEntities) {
            val chest = blockState as? Chest ?: continue
            val areaId = chest.persistentDataContainer.get(LOOT_PENDING_KEY, PersistentDataType.STRING) ?: continue

            fillChest(chest, areaId)
            chest.persistentDataContainer.remove(LOOT_PENDING_KEY)
            chest.update()
        }
    }

    private fun fillChest(chest: Chest, areaId: String) {
        val loot = ChestLootGenerator.generate(areaId)
        val inventory = chest.blockInventory

        for (item in loot) {
            inventory.addItem(item) // si no entra todo, el resto se descarta (cofre limitado a 27 slots)
        }
    }
}
