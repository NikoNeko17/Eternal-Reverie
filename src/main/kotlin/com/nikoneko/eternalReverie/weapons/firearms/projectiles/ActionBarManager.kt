package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object ActionBarManager {
    fun render(
        player: Player
    ) {

        val itemMeta = player.inventory.itemInMainHand.itemMeta ?: return
        val weaponUuid = UUID.fromString(itemMeta.persistentDataContainer.get(Keys.INSTANCE_UUID, PersistentDataType.STRING))
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
        player.sendActionBar(
            Component.text(message)
        )
    }
}