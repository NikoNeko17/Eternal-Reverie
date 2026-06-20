package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import java.util.UUID

abstract class AbstractProjectile(

    val shooter: LivingEntity,
    origin: Location,
    direction: Vector,
    val damage: Double,
    val speed: Double,
    val maxDistance: Double,
    val weaponAffinities: List<Pair<Affinity, Double>> = emptyList(),
    val shooterEquipment: PlayerStats.EquipmentStats = PlayerStats.computeEquipmentStats(shooter)
) {

    var currentPosition = origin.clone()
    var velocity = direction.clone().normalize().multiply(speed)
    var travelledDistance = 0.0
    var alive = true
    val hitEntities = mutableSetOf<UUID>()

    abstract fun tick()
    abstract fun spawnParticles()
    abstract fun onHit(target: LivingEntity)

    open fun destroy() {
        alive = false
        ProjectileManager.unregister(this)
    }

    protected fun move() {
        currentPosition.add(velocity)
        travelledDistance += speed
    }

    protected fun reachedMaxDistance(): Boolean {
        return travelledDistance >= maxDistance
    }
}