package com.nikoneko.eternalReverie.economy

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Maneja el depósito de Chatarra: click derecho con el ítem en mano (sin GUI
 * abierta) convierte 1 unidad a balance numérico (CurrencyManager).
 */
class CurrencyListener : Listener {

    @EventHandler
    fun onDepositCurrency(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        if (event.hand != EquipmentSlot.HAND) return // evita procesar 2 veces (mano + offhand)

        val player = event.player
        val item = event.item ?: return

        if (!CurrencyItem.isCurrency(item)) return

        event.isCancelled = true // evita interactuar con bloques/aire de paso

        depositOne(player, item)
    }

    private fun depositOne(player: Player, item: org.bukkit.inventory.ItemStack) {
        CurrencyManager.addBalance(player, CurrencyItem.VALUE_PER_UNIT)

        if (item.amount <= 1) {
            player.inventory.setItemInMainHand(null)
        } else {
            item.amount -= 1
        }

        player.sendActionBar(
            net.kyori.adventure.text.Component.text(
                "+${CurrencyItem.VALUE_PER_UNIT} Chatarra (Balance: ${CurrencyManager.getBalance(player)})",
                net.kyori.adventure.text.format.NamedTextColor.GOLD
            )
        )
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
    }
}
