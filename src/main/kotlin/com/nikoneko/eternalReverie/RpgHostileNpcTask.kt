package com.nikoneko.eternalReverie

import net.citizensnpcs.api.npc.NPC
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.nextUp

class RpgHostileNpcTask(
    private val plugin: JavaPlugin,
    private val npc: NPC,
    private val scanRadius: Double = 15.0,
) : BukkitRunnable() {

    private var currentTarget: Player? = null
    private var cooldownCounter = 0
    override fun run() {
        if (!npc.isSpawned) {
            this.cancel()
            return
        }

        val npcEntity = npc.entity as? LivingEntity ?: return

        npcEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.35

        val attackRange = npcEntity.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).

        val attackCooldownTicks = npcEntity.getAttribute(Attribute.GENERIC_ATTACK_SPEED)?.value?.nextUp()?.toInt() ?: 4
        // Reducir el cooldown del ataque de forma precisa en cada tick
        if (cooldownCounter > 0) {
            cooldownCounter--
        }

        // 1. Validar el objetivo actual si ya tiene uno
        if (currentTarget != null) {
            val target = currentTarget!!
            val isTargetValid = target.isOnline &&
                    !target.isDead &&
                    target.world == npcEntity.world &&
                    target.location.distance(npcEntity.location) <= (scanRadius * 1.5) &&
                    target.gameMode == GameMode.SURVIVAL

            if (isTargetValid) {
                // MODIFICACIÓN CRÍTICA: Solo actualizar el pathfinder si el objetivo cambió de entidad.
                // Esto evita que setTarget() rompa el contador de ticks y cause el spam de golpes.
                if (npc.navigator.entityTarget != target) {
                    npc.navigator.localParameters
                        .speedModifier(1.0f)
                        .range(scanRadius.toFloat() * 1.5f)
                        .useNewPathfinder(true)
                }

                // Ejecutar ataque manual con cooldown estricto
                val distance = npcEntity.location.distance(target.location)
                if (distance <= attackRange && cooldownCounter == 0) {
                    executeManualAttack(npcEntity, target)
                    cooldownCounter = attackCooldownTicks
                }
                return
            } else {
                currentTarget = null
                npc.navigator.cancelNavigation()
            }
        }

        // 2. Escanear un nuevo objetivo (solo si el actual quedó inválido)
        val nearbyPlayers = npcEntity.getNearbyEntities(scanRadius, scanRadius, scanRadius)
            .filterIsInstance<Player>()
            .filter { it.gameMode == GameMode.SURVIVAL && !it.isDead }

        if (nearbyPlayers.isNotEmpty()) {
            val closestPlayer = nearbyPlayers.minByOrNull { it.location.distanceSquared(npcEntity.location) }
            if (closestPlayer != null) {
                currentTarget = closestPlayer
                npc.navigator.setTarget(closestPlayer, false)
            }
        }
    }

    private fun executeManualAttack(attacker: LivingEntity, victim: Player) {
        attacker.swingMainHand()
        victim.damage(0.5, attacker)
    }
}
