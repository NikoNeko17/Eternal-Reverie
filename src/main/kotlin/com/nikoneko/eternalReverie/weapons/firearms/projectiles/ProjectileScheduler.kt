package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class ProjectileScheduler(

    private val plugin: JavaPlugin

) {
    fun start() {
        object : BukkitRunnable() {
            override fun run() {
                ProjectileManager.tick()
            }
        }.runTaskTimer(
            plugin,
            0L,
            1L
        )
    }
}