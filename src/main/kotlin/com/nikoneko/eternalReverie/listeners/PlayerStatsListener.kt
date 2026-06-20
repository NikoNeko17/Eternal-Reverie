package com.nikoneko.eternalReverie.listeners

import com.nikoneko.eternalReverie.player.PlayerStats
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot

class PlayerStatsListener : Listener {

    // Recalcula Vitalidad/Stamina cuando cambia cualquier pieza de armadura.
    // Cubre jugadores Y NPCs de Citizens tipo PLAYER (ambos son LivingEntity).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEquipmentChanged(event: EntityEquipmentChangedEvent) {
        val entity = event.entity

        // Solo nos importan cambios en slots de armadura para Vitalidad/Stamina
        val relevantSlots = setOf(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
        )
        val touchesArmor = event.equipmentChanges.keys.any { it in relevantSlots }
        if (!touchesArmor) return

        PlayerStats.initializeIfAbsent(entity)
        PlayerStats.recalculateMaxHp(entity)
        PlayerStats.recalculateMaxStamina(entity)
    }

    // Inicializa stats al entrar por primera vez (o reentrar) al servidor.
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        PlayerStats.initializeIfAbsent(player)
        PlayerStats.recalculateMaxHp(player)
        PlayerStats.recalculateMaxStamina(player)
    }

    companion object {
        /**
         * Llamar manualmente al spawnear un NPC de Citizens (en spawnAndActivateEnemy
         * o equivalente), ya que los NPCs no disparan PlayerJoinEvent.
         */
        fun initializeNpc(entity: LivingEntity) {
            PlayerStats.initializeIfAbsent(entity)
            PlayerStats.recalculateMaxHp(entity)
            PlayerStats.recalculateMaxStamina(entity)
        }
    }
}