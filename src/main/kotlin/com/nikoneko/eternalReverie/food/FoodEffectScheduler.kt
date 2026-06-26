package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.player.PlayerStats
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * Corre cada 20 ticks (1 segundo).
 * Por cada jugador online con efectos activos:
 *   1. Decrementa remainingTicks.
 *   2. Si alguno expiró, recalcula stats del jugador.
 */
class FoodEffectScheduler(private val plugin: Plugin) {

    fun start() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    val expired = FoodEffectManager.tick(player.uniqueId, deltaTicks = 20)
                    if (expired) {
                        // Recalcular stats para reflejar el efecto que expiró
                        PlayerStats.recalculateMaxHp(player)
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L)
    }
}
