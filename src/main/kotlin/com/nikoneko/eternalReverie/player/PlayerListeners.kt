package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EnemyObject
import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.affinities.AffinityMarkManager
import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.crafting.MaterialType
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.decrementDurability
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.isCustomItem
import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.refreshLore
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.Affinity
import com.nikoneko.eternalReverie.weapons.WeaponClass
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.BulletProjectile
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileManager
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import net.citizensnpcs.api.CitizensAPI
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

class PlayerListeners(val plugin: EternalReverie) : Listener {

    @EventHandler
    fun onOpenInventory(event: InventoryOpenEvent) {
        return
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        // ── Caso: NPC ataca a jugador ─────────────────────────────────────────────
        if (victim is Player && damager is LivingEntity && damager.hasMetadata("NPC")) {
            event.isCancelled = true
            val npc = CitizensAPI.getNPCRegistry().getNPC(damager) ?: return
            EnemyObject.get(npc.id) ?: return

            val attacker = damager as Player

            if (AffinityMarkManager.hasMark(damager, Affinity.ATADURA)) return

            val weaponBlueprint = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
            val weaponMaterials = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

            val (weaponStats, equipmentStats) = buildAttackerStats(damager, weaponBlueprint, weaponMaterials)

            val finalDamage = CombatResolver.resolveHit(
                attacker = attacker,
                victim = victim as LivingEntity,
                rawDamage = weaponStats.damage,
                attackerEquipment = equipmentStats,
                weaponAffinities = weaponStats.affinities,
                plugin
            )

            event.damage = if (PlayerStats.getCurrentHp(victim) <= 0.0)
                (victim.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0) else 0.0

            return
        }

        // ── Caso: jugador ataca NPC ───────────────────────────────────────────────
        if (damager is Player && victim.hasMetadata("NPC")) {
            event.isCancelled = true

            val npc = CitizensAPI.getNPCRegistry().getNPC(victim) ?: return
            val enemy = EnemyObject.get(npc.id) ?: return

            if (AffinityMarkManager.hasMark(damager, Affinity.ATADURA)) return
            if (StaminaManager.isExhausted(damager)) return
            if (damager.attackCooldown < 1f) {
                damager.playSound(damager, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.4f, 1.0f)
                return
            }
            if (event.cause == DamageCause.ENTITY_SWEEP_ATTACK) return

            val weaponBlueprint = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
            val weaponMaterials = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

            val (weaponStats, equipmentStats) = buildAttackerStats(damager, weaponBlueprint, weaponMaterials)
            val weaponFamily = weaponBlueprint?.let {
                runCatching { BlueprintRegistry.get(it) }.getOrNull()?.family
            }

            StaminaManager.tryConsumeForAttack(damager, weaponFamily)

            val finalDamage = CombatResolver.resolveHit(
                attacker = damager,
                victim = victim as LivingEntity,
                rawDamage = weaponStats.damage,
                attackerEquipment = equipmentStats,
                weaponAffinities = weaponStats.affinities,
                plugin
            )

            return
        }

        // ── Caso: jugador ataca jugador ───────────────────────────────────────────
        if (damager is Player && victim is Player) {
            if (AffinityMarkManager.hasMark(damager, Affinity.ATADURA)) {
                event.isCancelled = true; return
            }
            if (StaminaManager.isExhausted(damager)) {
                event.isCancelled = true; return
            }
            if (damager.attackCooldown < 1f) {
                damager.playSound(damager, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.4f, 1.0f)
                event.isCancelled = true; return
            }
            if (event.cause == DamageCause.ENTITY_SWEEP_ATTACK) {
                event.isCancelled = true; return
            }

            victim.maximumNoDamageTicks = 0

            val weaponBlueprint = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
            val weaponMaterials = damager.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

            val (weaponStats, equipmentStats) = buildAttackerStats(damager, weaponBlueprint, weaponMaterials)
            val weaponFamily = weaponBlueprint?.let {
                runCatching { BlueprintRegistry.get(it) }.getOrNull()?.family
            }

            StaminaManager.tryConsumeForAttack(damager, weaponFamily)

            val finalDamage = CombatResolver.resolveHit(
                attacker = damager,
                victim = victim,
                rawDamage = weaponStats.damage,
                attackerEquipment = equipmentStats,
                weaponAffinities = weaponStats.affinities,
                plugin
            )

            event.damage = if (PlayerStats.getCurrentHp(victim) <= 0.0)
                (victim.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0) else 0.0
            return
        }

        // ── Caso: flecha ataca jugador (ya manejado por BowListeners) ─────────────
        // No hacer nada; BowListeners cancela el evento y aplica daño directamente.
    }

    @EventHandler
    fun onFireWeaponEvent(event: PlayerInteractEvent) {
        // Atadura: el jugador anclado no puede usar ítems/disparar mientras la Marca esté activa.
        if (AffinityMarkManager.hasMark(event.player, Affinity.ATADURA)) {
            event.isCancelled = true
            return
        }

        // Exhausto: sin stamina suficiente, no puede disparar.
        if (StaminaManager.isExhausted(event.player)) {
            event.isCancelled = true
            return
        }

        val itemMeta = event.item?.itemMeta ?: return
        val pdc = itemMeta.persistentDataContainer

        val familyName = pdc.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: return
        val family = runCatching { WeaponFamily.valueOf(familyName) }.getOrNull() ?: return
        val weaponType = family.weaponClass

        val player = event.player
        val attackSpeed = pdc.get(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE) ?: return
        val weaponUuid = runCatching {
            UUID.fromString(pdc.get(Keys.INSTANCE_UUID, PersistentDataType.STRING) ?: return)
        }.getOrNull() ?: return

        if (!event.action.isRightClick) return
        if (weaponType !in listOf(WeaponClass.PISTOLA, WeaponClass.ESCOPETA, WeaponClass.RIFLE)) return

        // ── Chequeo semi-auto ─────────────────────────────────────────────────────
        // Para semi-auto: cada "click nuevo" (gap detectado) dispara una vez.
        // Eventos repetidos por mantener sostenido el botón se ignoran.
        // Armas automáticas (isSemiAuto = false) disparan en cada evento, limitadas
        // solo por el cooldown de attackSpeed.
        if (family.isSemiAuto) {
            if (!WeaponStateManager.isNewClick(player, weaponUuid)) return
        }


        // ── Chequeo munición ──────────────────────────────────────────────────────
        if (family.magazineSize > 0) {
            if (WeaponStateManager.isReloading(player, weaponUuid)) {
                player.sendActionBar(Component.text("§eRecargando..."))
                return
            }
            val ammo = WeaponStateManager.getAmmo(player, weaponUuid)
            if (ammo <= 0) {
                player.sendActionBar(Component.text("§c[ Sin munición — Q para recargar ]"))
                return
            }
        }

        // ── Chequeo cooldown ──────────────────────────────────────────────────────
        if (!WeaponStateManager.canShoot(player, weaponUuid, attackSpeed)) return

        // ── Cancelar interacción con bloques de tierra si aplica ──────────────────
        if (listOf(Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT)
                .contains(event.clickedBlock?.type)) {
            event.isCancelled = true
        }

        // ── Calcular stats del arma disparada ─────────────────────────────────────
        val firedMeta = player.inventory.itemInMainHand.itemMeta
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

        // ── Disparar proyectil ────────────────────────────────────────────────────
        val projectile = BulletProjectile(
            plugin,
            shooter = player,
            origin = player.eyeLocation,
            direction = player.eyeLocation.direction,
            damage = firedStats.damage,
            speed = 1.0,
            maxDistance = pdc.get(Keys.REACH, PersistentDataType.DOUBLE) ?: return,
            weaponAffinities = firedStats.affinities,
            shooterEquipment = PlayerStats.computeEquipmentStats(player)
        )

        // ── Post-disparo: munición y cooldown ─────────────────────────────────────
        if (family.magazineSize > 0) {
            WeaponStateManager.setAmmo(player, weaponUuid,
                WeaponStateManager.getAmmo(player, weaponUuid) - 1)
        }
        WeaponStateManager.trigger(player, weaponUuid)
        ProjectileManager.register(projectile)

        // ── Sonido ────────────────────────────────────────────────────────────────
        when (weaponType) {
            else -> {
                player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1f, 1.1f)
                player.playSound(player, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 2f)
            }
        }
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

    private fun buildAttackerStats(
        attacker: Player,
        blueprintId: String?,
        materialIds: List<String>?
    ): Pair<CraftingCalculator.ComputedWeaponStats, PlayerStats.EquipmentStats> {
        val fallback = CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList())
        if (blueprintId == null || materialIds == null)
            return fallback to PlayerStats.computeEquipmentStats(attacker)

        val blueprint = runCatching { BlueprintRegistry.get(blueprintId) }.getOrNull()
            ?: return fallback to PlayerStats.computeEquipmentStats(attacker)
        val materials = materialIds.mapNotNull { runCatching { MaterialType.valueOf(it) }.getOrNull() }
        return CraftingCalculator.computeWeaponStatsPublic(blueprint, materials) to
                PlayerStats.computeEquipmentStats(attacker)
    }
}