package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.EternalReverie
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

/**
 * Corre 1 vez por segundo (20 ticks) y aplica el efecto periódico de todas
 * las Marcas activas. Incluye jugadores online y NPCs vivos de Citizens
 * (cualquier LivingEntity que tenga Marcas registradas en el manager).
 *
 * Llamar AffinityTickScheduler(plugin).start() una vez en onEnable.
 */
class AffinityTickScheduler(private val plugin: EternalReverie) {

    fun start() {
        object : BukkitRunnable() {
            override fun run() {
                AffinityMarkManager.tickAll { collectTrackedEntities() }
            }
        }.runTaskTimer(plugin, 20L, 20L) // empieza tras 1s, se repite cada 1s
    }

    // Junta jugadores en línea + cualquier LivingEntity de los mundos cargados
    // que actualmente tenga Marcas (evita recorrer TODAS las entidades del mundo
    // cada tick; el manager ya filtra por uuid existente en su mapa interno).
    private fun collectTrackedEntities(): List<LivingEntity> {
        val players: List<LivingEntity> = Bukkit.getOnlinePlayers().toList()

        val npcs: List<LivingEntity> = CitizensAPI.getNPCRegistry().map { it.entity as LivingEntity }

        return players + npcs
    }
}
