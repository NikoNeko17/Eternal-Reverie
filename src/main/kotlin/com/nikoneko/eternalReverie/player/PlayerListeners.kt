package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.WeaponClass
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.BulletProjectile
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileManager
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class PlayerListeners(val plugin: EternalReverie) : Listener {
    @EventHandler
    fun onOpenInventory(event: InventoryOpenEvent) {
        return
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        //event.damage = 0.0
        val attacker = event.damager
        val victim = event.entity as LivingEntity
        if (attacker is Player && attacker.attackCooldown < 1f) {
            attacker.playSound(attacker, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.4f, 1.0f)
            event.isCancelled = true
        }
        if (event.cause == DamageCause.ENTITY_SWEEP_ATTACK) event.isCancelled = true
        victim.maximumNoDamageTicks = 0
    }

    @EventHandler
    fun onFireWeaponEvent(event: PlayerInteractEvent) {
        if (listOf(
                Material.DIRT,
                Material.GRASS_BLOCK,
                Material.COARSE_DIRT
            ).contains(event.clickedBlock?.type)
        ) event.isCancelled = true
        val itemMeta = event.item?.itemMeta ?: return
        val familyToClass =
            WeaponFamily.entries
                .associate {
                    it.name to it.weaponClass
                }
        val weaponType =
            familyToClass[itemMeta.persistentDataContainer.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING)]

        val player = event.player
        val attackSpeed = itemMeta.persistentDataContainer.get(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE) ?: return

        val weaponUuid = UUID.fromString(
            itemMeta
                .persistentDataContainer
                .get(
                    Keys.INSTANCE_UUID,
                    PersistentDataType.STRING
                )

        )

        if (
            !WeaponStateManager.canShoot(
                player,
                weaponUuid,
                attackSpeed)
        ) return

        if (event.action.isRightClick && listOf(WeaponClass.PISTOLA, WeaponClass.ESCOPETA, WeaponClass.RIFLE).contains(weaponType)){
            val projectile = BulletProjectile(
                shooter = player,
                origin = player.eyeLocation,
                direction = player.eyeLocation.direction,
                damage = 10.0,
                speed = 1.0,
                maxDistance = event.item?.persistentDataContainer?.get(Keys.REACH, PersistentDataType.DOUBLE) ?: return
            )

            WeaponStateManager.trigger(player, UUID.fromString(player.inventory.itemInMainHand.itemMeta?.persistentDataContainer?.get(Keys.INSTANCE_UUID, PersistentDataType.STRING)) ?: return)
            ProjectileManager.register(projectile)
            when (weaponType){
                else -> {
                    player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1f, 1.1f)
                    player.playSound(player, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 2f)
                }
            }
        } else return
    }
}