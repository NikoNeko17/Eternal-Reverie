package com.nikoneko.eternalReverie.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Arrow
import org.bukkit.entity.Display
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

object DamageIndicator {

    fun spawn(plugin: Plugin, victim: LivingEntity, damage: Double, isCrit: Boolean) {
        if (victim is Arrow) return
        val spawnLoc = victim.location.clone().add(
            (Math.random() - 0.5) * 0.6, // offset X aleatorio para que no se apilen
            victim.height + 0.3,
            (Math.random() - 0.5) * 0.6
        )

        val text = if (isCrit)
            Component.text("⚔ ${damage.toInt()}!")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.UNDERLINED, true)
        else
            Component.text(damage.toInt().toString())
                .color(NamedTextColor.RED)

        val display = spawnLoc.world.spawn(spawnLoc, TextDisplay::class.java) { td ->
            td.text(text)
            td.billboard = Display.Billboard.CENTER
            td.isShadowed = true
            td.backgroundColor = org.bukkit.Color.fromARGB(0, 0, 0, 0)
        }

        // Sube suavemente durante 20 ticks y luego se va desvaneciendo otros 10
        object : BukkitRunnable() {
            var tick = 0
            override fun run() {
                if (!display.isValid) { cancel(); return }
                tick++

                // Subida
                display.teleport(display.location.add(0.0, 0.045, 0.0))

                // A partir del tick 20 achica la escala para simular fade
                if (tick >= 20) {
                    val scale = 1.0f - (tick - 20) / 10.0f
                    display.transformation = Transformation(
                        Vector3f(0f, 0f, 0f),
                        AxisAngle4f(0f, 0f, 0f, 1f),
                        Vector3f(scale, scale, scale),
                        AxisAngle4f(0f, 0f, 0f, 1f)
                    )
                }

                if (tick >= 30) {
                    display.remove()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}