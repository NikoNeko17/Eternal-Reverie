package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.durability.DurabilityListener.Companion.isCustomItem
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

object ActionBarManager {
    fun render(
        player: Player
    ) {

        val item = player.inventory.itemInMainHand
        val itemMeta = item.itemMeta

        val showCooldownBar = isCustomItem(item) && run {
            val weaponUuidStr = itemMeta?.persistentDataContainer?.get(Keys.INSTANCE_UUID, PersistentDataType.STRING)
            val attackSpeed = itemMeta?.persistentDataContainer?.get(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE)
            weaponUuidStr != null && attackSpeed != null &&
                WeaponStateManager.getCooldownProgress(player, UUID.fromString(weaponUuidStr), attackSpeed) < 1.0
        }

        if (showCooldownBar) {
            val weaponUuid = UUID.fromString(itemMeta!!.persistentDataContainer.get(Keys.INSTANCE_UUID, PersistentDataType.STRING))
            val attackSpeed = itemMeta.persistentDataContainer.get(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE) ?: return

            val progress = WeaponStateManager
                .getCooldownProgress(
                    player,
                    weaponUuid,
                    attackSpeed
                )

            val filled =
                (progress * 10)
                    .toInt()

            val empty =
                10 - filled.coerceIn(0, 10)

            val bar =

                "█".repeat(filled) +

                        "░".repeat(empty)

            val message =

                if (progress == 1.0)
                    "$bar ✓"
                else
                    bar



            if (progress > 1.0) {
                WeaponStateManager.clearWeapon(player, weaponUuid)
                return
            }
            if (!WeaponStateManager.isReloading(player, weaponUuid) || WeaponStateManager.getAmmo(player, weaponUuid) <= 0) {
                player.sendActionBar(
                    Component.text(message)
                )
            }
        } else {
            val health: Pair<Double, Double> =
                Pair(player.persistentDataContainer.get(Keys.CURRENT_HP, PersistentDataType.DOUBLE) ?: 0.0,
                player.persistentDataContainer.get(Keys.MAX_HP, PersistentDataType.DOUBLE) ?: 0.0)

            val stamina: Pair<Double, Double> =
                Pair(player.persistentDataContainer.get(Keys.CURRENT_STAMINA, PersistentDataType.DOUBLE) ?: 0.0,
                    player.persistentDataContainer.get(Keys.MAX_STAMINA, PersistentDataType.DOUBLE) ?: 0.0)

            val ammo: Pair<Int, Int?>? = if (WeaponStateManager.getWeaponFamily(item)?.weaponClass in WeaponStateManager.firearmClasses) {
                WeaponStateManager.getAmmoData(player, item)
            } else null

            val finalComponent = Component.text("%.0f/%.0f ❦    ".format(health.first, health.second), NamedTextColor.RED)
                .append(Component.text("%.0f/%.0f ⚡".format(stamina.first, stamina.second), NamedTextColor.YELLOW))
                .append(if (ammo != null) Component.text("    %d/%d".format(ammo.first, ammo.second), NamedTextColor.WHITE) else Component.empty())

            player.sendActionBar(finalComponent)
        }

    }
}