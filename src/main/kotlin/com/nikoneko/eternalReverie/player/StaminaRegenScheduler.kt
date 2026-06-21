package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

/**
 * Corre 1 vez por tick (20/seg) y regenera Stamina de jugadores online + NPCs vivos,
 * respetando el delay de StaminaManager.EXHAUSTION_DELAY_MILLIS desde el último gasto.
 *
 * Llamar StaminaRegenScheduler(plugin).start() una vez en onEnable.
 */
class StaminaRegenScheduler(private val plugin: EternalReverie) {

    fun start() {
        object : BukkitRunnable() {
            override fun run() {
                for (entity in collectTrackedEntities()) {
                    StaminaManager.tickRegen(entity)
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun collectTrackedEntities(): List<LivingEntity> {
        val players: List<LivingEntity> = Bukkit.getOnlinePlayers().toList()
        val npcs: List<LivingEntity> = Bukkit.getWorlds().flatMap { world ->
            world.livingEntities.filter { it !is Player }
        }
        return players + npcs
    }
}
