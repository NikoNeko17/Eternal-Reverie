package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.decrementDurability
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.isCustomItem
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.refreshLore
import com.nikoneko.eternalReverie.items.BlueprintData
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.items.MaterialType
import com.nikoneko.eternalReverie.weapons.WeaponClass
import com.nikoneko.eternalReverie.weapons.WeaponData
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.BulletProjectile
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileManager
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.random.Random

class PlayerListeners(val plugin: EternalReverie) : Listener {
    @EventHandler
    fun onOpenInventory(event: InventoryOpenEvent) {
        return
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as Player
        val victim = event.entity as Player

        // Atadura: el jugador anclado no puede atacar mientras la Marca esté activa.
        if (com.nikoneko.eternalReverie.affinities.AffinityMarkManager.hasMark(
                attacker, com.nikoneko.eternalReverie.weapons.Affinity.ATADURA
            )
        ) {
            event.isCancelled = true
            return
        }

        // Exhausto: sin stamina suficiente, no puede atacar.
        if (StaminaManager.isExhausted(attacker)) {
            event.isCancelled = true
            return
        }

        if (attacker.attackCooldown < 1f) {
            attacker.playSound(attacker, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.4f, 1.0f)
            event.isCancelled = true
        }
        if (event.cause == DamageCause.ENTITY_SWEEP_ATTACK) event.isCancelled = true
        victim.maximumNoDamageTicks = 0

        val attackerWeaponBlueprint : String? = attacker.inventory.itemInMainHand.itemMeta?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
        val attackerWeaponMaterials = attacker.inventory.itemInMainHand.itemMeta?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())
        lateinit var attackerStats : Pair<CraftingCalculator.ComputedWeaponStats, PlayerStats.EquipmentStats>
        var attackerWeaponFamily: WeaponFamily? = null
        if (attackerWeaponBlueprint != null && attackerWeaponMaterials != null) {
            val parsedWeaponBlueprint : BlueprintData? = runCatching {
                BlueprintRegistry.get(attackerWeaponBlueprint)
            }.getOrNull()
            val parsedWeaponMaterials: List<MaterialType> = attackerWeaponMaterials.mapNotNull { materialStr ->
                runCatching { MaterialType.valueOf(materialStr) }.getOrNull()
            }
            attackerWeaponFamily = parsedWeaponBlueprint?.family
            attackerStats = if (parsedWeaponBlueprint != null) {
                Pair(CraftingCalculator.computeWeaponStatsPublic(parsedWeaponBlueprint, parsedWeaponMaterials),
                    PlayerStats.computeEquipmentStats(attacker))
            } else {
                Pair(CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList()),
                    PlayerStats.computeEquipmentStats(attacker))
            }
        } else {
            attackerStats = Pair(CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList()),
                PlayerStats.computeEquipmentStats(attacker))
        }

        StaminaManager.tryConsumeForAttack(attacker, attackerWeaponFamily)

        val finalDamage = CombatResolver.resolveHit(
            attacker = attacker,
            victim = victim,
            rawDamage = attackerStats.first.damage,
            attackerEquipment = attackerStats.second,
            weaponAffinities = attackerStats.first.affinities
        )

        event.damage = if (PlayerStats.getCurrentHp(victim) <= 0.0
        ) (victim.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0) else 0.0
    }

    @EventHandler
    fun onFireWeaponEvent(event: PlayerInteractEvent) {
        // Atadura: el jugador anclado no puede usar ítems/disparar mientras la Marca esté activa.
        if (com.nikoneko.eternalReverie.affinities.AffinityMarkManager.hasMark(
                event.player, com.nikoneko.eternalReverie.weapons.Affinity.ATADURA
            )
        ) {
            event.isCancelled = true
            return
        }

        // Exhausto: sin stamina suficiente, no puede disparar.
        if (StaminaManager.isExhausted(event.player)) {
            event.isCancelled = true
            return
        }

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
            familyToClass[itemMeta.persistentDataContainer.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: return]

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
            val firedItem = player.inventory.itemInMainHand
            val firedMeta = firedItem.itemMeta
            val firedBlueprintId = firedMeta?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
            val firedMaterialIds = firedMeta?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

            var firedWeaponFamily: WeaponFamily? = null
            val firedStats: CraftingCalculator.ComputedWeaponStats =
                if (firedBlueprintId != null && firedMaterialIds != null) {
                    val parsedBlueprint = runCatching { BlueprintRegistry.get(firedBlueprintId) }.getOrNull()
                    val parsedMaterials = firedMaterialIds.mapNotNull {
                        runCatching { MaterialType.valueOf(it) }.getOrNull()
                    }
                    firedWeaponFamily = parsedBlueprint?.family
                    if (parsedBlueprint != null) {
                        CraftingCalculator.computeWeaponStatsPublic(parsedBlueprint, parsedMaterials)
                    } else {
                        CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList())
                    }
                } else {
                    CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList())
                }

            StaminaManager.tryConsumeForAttack(player, firedWeaponFamily)

            val projectile = BulletProjectile(
                shooter = player,
                origin = player.eyeLocation,
                direction = player.eyeLocation.direction,
                damage = firedStats.damage,
                speed = 1.0,
                maxDistance = event.item?.persistentDataContainer?.get(Keys.REACH, PersistentDataType.DOUBLE) ?: return,
                weaponAffinities = firedStats.affinities,
                shooterEquipment = PlayerStats.computeEquipmentStats(player)
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

    @EventHandler
    fun onPlayerSpawn(event: PlayerRespawnEvent) {
        val player = event.player
        val maxHp = player.persistentDataContainer.get(Keys.MAX_HP, PersistentDataType.DOUBLE) ?: return
        PlayerStats.setCurrentHp(event.player, maxHp)

        val armorPieces = listOf(
            player.inventory.helmet,
            player.inventory.chestplate,
            player.inventory.leggings,
            player.inventory.boots
        )

        val eligibleSlots = armorPieces.indices.filter { i ->
            armorPieces[i] != null && isCustomItem(armorPieces[i]!!)
        }

        if (eligibleSlots.isNotEmpty()) {
            val chosenIndex = eligibleSlots.random()
            val armorPiece = armorPieces[chosenIndex]!!

            val broke = decrementDurability(armorPiece, percentage = 0.1)
            if (broke) {
                when (chosenIndex) {
                    0 -> player.inventory.helmet = null
                    1 -> player.inventory.chestplate = null
                    2 -> player.inventory.leggings = null
                    3 -> player.inventory.boots = null
                }
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                player.sendMessage(
                    Component.text("Una pieza de tu armadura se ha roto.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                )
            } else {
                refreshLore(armorPiece)
            }
            player.updateInventory()
        }
    }
}