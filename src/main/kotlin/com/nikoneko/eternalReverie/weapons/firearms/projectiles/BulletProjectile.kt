package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.player.CombatResolver
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.FluidCollisionMode
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity

class BulletProjectile(
    val plugin: EternalReverie,
    shooter: LivingEntity,
    origin: Location,
    direction: Vector,
    damage: Double,
    speed: Double,
    maxDistance: Double,
    weaponAffinities: List<Pair<Affinity, Double>> = emptyList(),
    shooterEquipment: PlayerStats.EquipmentStats = PlayerStats.computeEquipmentStats(shooter)
) : AbstractProjectile(
    shooter,
    origin,
    direction,
    damage,
    speed,
    maxDistance,
    weaponAffinities,
    shooterEquipment
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
        // El daño se calcula con las stats capturadas AL MOMENTO DEL DISPARO
        // (damage, weaponAffinities, shooterEquipment), no con lo que el shooter
        // tenga en mano ahora. Esto evita que cambiar de arma a mitad de vuelo
        // afecte el resultado del impacto.
        CombatResolver.resolveHit(
            attacker = shooter,
            victim = target,
            rawDamage = damage,
            attackerEquipment = shooterEquipment,
            weaponAffinities = weaponAffinities,
            plugin = plugin
        )

        // damage(0.0, shooter) solo dispara la animación/sonido/knockback vanilla
        // de "fui golpeado", sin aplicar daño vanilla real (ya lo aplicamos arriba
        // directo al HP custom). No pasa por PlayerListeners.onEntityDamage porque
        // ese listener es específicamente para combate cuerpo a cuerpo.
        target.damage(0.0, shooter)
        destroy()
    }

}