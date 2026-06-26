package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.affinities.AffinityMarkManager
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.player.PlayerStats
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType

class FoodListener : Listener {

    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val pdc = event.item.itemMeta?.persistentDataContainer ?: return

        val foodId = pdc.get(Keys.FOOD_ID, PersistentDataType.STRING) ?: return
        val foodType = FoodType.fromId(foodId) ?: return
        val foodData = foodType.data

        // Aplicar efectos de stat y obtener afinidades pendientes
        val pendingAffinities = FoodEffectManager.apply(player.uniqueId, foodData)

        // Aplicar marcas de afinidad al jugador consumidor
        // Usamos el jugador como attacker y victim a la vez (se envenena a sí mismo)
        for (affinity in pendingAffinities) {
            AffinityMarkManager.forceApplyMark(player, player, affinity, foodData.effects
                .firstOrNull { (it.type as? FoodEffectType.AffinityMark)?.affinity == affinity }
                ?.durationTicks ?: 600
            )
        }

        // Curar HP
        val currentHp = PlayerStats.getCurrentHp(player)
        val maxHp = PlayerStats.getMaxHp(player)
        PlayerStats.setCurrentHp(player, (currentHp + foodData.healAmount).coerceAtMost(maxHp))

        // Recalcular stats para que los nuevos buffs surtan efecto inmediato
        PlayerStats.recalculateMaxHp(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        // Los efectos de comida son volátiles; limpiar al salir evita leaks de memoria
        FoodEffectManager.clear(event.player.uniqueId)
    }
}
