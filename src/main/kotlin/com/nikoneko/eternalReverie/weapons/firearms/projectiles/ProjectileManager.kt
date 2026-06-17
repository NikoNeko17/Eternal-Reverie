package com.nikoneko.eternalReverie.weapons.firearms.projectiles

object ProjectileManager {

    private val projectiles = mutableSetOf<AbstractProjectile>()

    fun register(projectile: AbstractProjectile) {
        projectiles.add(projectile)
    }

    fun unregister(projectile: AbstractProjectile) {
        projectiles.remove(projectile)
    }

    fun tick() {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            if (!projectile.alive) {
                iterator.remove()
                continue
            }
            projectile.tick()
        }
    }
}