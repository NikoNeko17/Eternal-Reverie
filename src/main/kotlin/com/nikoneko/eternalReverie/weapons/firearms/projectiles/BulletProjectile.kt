package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.FluidCollisionMode
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector

class BulletProjectile(
    shooter: LivingEntity,
    origin: Location,
    direction: Vector,
    damage: Double,
    speed: Double,
    maxDistance: Double
) : AbstractProjectile(
    shooter,
    origin,
    direction,
    damage,
    speed,
    maxDistance
) {

    override fun tick() {
        val previousPosition = currentPosition.clone()
        move()
        val world = currentPosition.world ?: return destroy()
        val ray: RayTraceResult? = world.rayTrace(
            previousPosition,
            velocity.clone().normalize(),
            speed,
            FluidCollisionMode.NEVER,
            true,
            0.15
        ) {
                entity ->
            entity != shooter
        }

        val hit = ray?.hitEntity

        if (hit is LivingEntity) {

            if (!hitEntities.contains(hit.uniqueId)) {

                hitEntities.add(hit.uniqueId)

                onHit(hit)

            }

        }

        spawnParticles()

        if (reachedMaxDistance()) {

            destroy()

        }

    }

    override fun spawnParticles() {

        val world = currentPosition.world ?: return

        world.spawnParticle(
            Particle.CRIT,
            currentPosition,
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
    }

    override fun onHit(target: LivingEntity) {
        target.damage(0.0, shooter)
        destroy()
    }

}