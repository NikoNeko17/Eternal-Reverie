package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.scheduler.BukkitRunnable

/**
 * Maneja el costo de Stamina de Sprint: costo fijo al activar (PlayerToggleSprintEvent)
 * + costo continuo por tick mientras el jugador sigue sprintando (chequeado vía
 * player.isSprinting cada tick, ya que Bukkit no dispara un evento "sigue sprintando").
 *
 * Si el jugador queda Exhausto mientras corre, se le fuerza isSprinting = false
 * (Bukkit permite cancelar el toggle pero no "des-sprintar" a mitad de carrera
 * sin tocar la propiedad directamente).
 */
class SprintStaminaListener(private val plugin: EternalReverie) : Listener {

    @EventHandler
    fun onToggleSprint(event: PlayerToggleSprintEvent) {
        if (!event.isSprinting) return // solo nos importa cuando EMPIEZA a sprintar

        val player = event.player

        if (StaminaManager.isExhausted(player)) {
            event.isCancelled = true
            return
        }

        val success = StaminaManager.tryConsumeSprintActivation(player)
        if (!success) {
            event.isCancelled = true
        }
    }

    fun startTick() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (!player.isSprinting) continue

                    if (StaminaManager.isExhausted(player)) {
                        player.isSprinting = false
                        continue
                    }

                    StaminaManager.tickSprintCost(player)

                    if (StaminaManager.isExhausted(player)) {
                        player.isSprinting = false
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }
}
