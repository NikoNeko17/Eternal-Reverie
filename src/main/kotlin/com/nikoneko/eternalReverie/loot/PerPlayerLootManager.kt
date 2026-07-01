package com.nikoneko.eternalReverie.loot

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import java.util.UUID

/**
 * Mantiene, en memoria, un inventario de loot independiente por (cofre, jugador).
 *
 * No persiste entre reinicios de servidor ni entre reciclajes de instancia
 * de forma intencional: es loot de mazmorra instanciada, se espera que se
 * regenere junto con la instancia. Si en el futuro se necesita persistencia
 * real, acá es el único lugar que habría que tocar (serializar por chestId).
 */
object PerPlayerLootManager {

    // chestId -> (playerUuid -> inventory)
    private val lootByChest = mutableMapOf<UUID, MutableMap<UUID, Inventory>>()

    /**
     * Devuelve el inventario de loot del jugador para este cofre, generándolo
     * la primera vez que lo pide (via [generator]).
     */
    fun getOrCreate(
        chestId: UUID,
        player: UUID,
        title: String,
        generator: () -> Inventory
    ): Inventory {
        val playerMap = lootByChest.getOrPut(chestId) { mutableMapOf() }
        return playerMap.getOrPut(player) { generator() }
    }

    fun hasLoot(chestId: UUID, player: UUID): Boolean {
        return lootByChest[chestId]?.containsKey(player) == true
    }

    /** Limpia todo el loot asociado a un cofre (ej. al destruirse la instancia). */
    fun clearChest(chestId: UUID) {
        lootByChest.remove(chestId)
    }

    /** Limpia el loot de un jugador específico para un cofre puntual. */
    fun clearPlayerLoot(chestId: UUID, player: UUID) {
        lootByChest[chestId]?.remove(player)
    }

    /** Limpia todo lo asociado a un jugador (ej. al desconectarse), en todos los cofres. */
    fun clearPlayer(player: UUID) {
        lootByChest.values.forEach { it.remove(player) }
    }

    /** Limpia absolutamente todo (ej. al reiniciar/recargar el plugin). */
    fun clearAll() {
        lootByChest.clear()
    }
}
