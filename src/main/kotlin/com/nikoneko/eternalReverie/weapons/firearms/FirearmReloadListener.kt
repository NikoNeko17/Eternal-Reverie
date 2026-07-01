package com.nikoneko.eternalReverie.weapons.firearms

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.WeaponClass
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class FirearmReloadListener(private val plugin: EternalReverie) : Listener {

    private val firearmClasses = setOf(WeaponClass.PISTOLA, WeaponClass.ESCOPETA, WeaponClass.RIFLE)

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val meta = event.itemDrop.itemStack.itemMeta ?: return

        val pdc = meta.persistentDataContainer

        // Solo armas con WEAPON_FAMILY registrada
        val familyName = pdc.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: return
        val family = runCatching { WeaponFamily.valueOf(familyName) }.getOrNull() ?: return

        // Solo clases de fuego con sistema de munición activo
        if (family.weaponClass !in firearmClasses) return
        if (family.magazineSize == 0) return

        // Leer UUID del arma
        val weaponUuidStr = pdc.get(Keys.INSTANCE_UUID, PersistentDataType.STRING) ?: return
        val weaponUuid = runCatching { UUID.fromString(weaponUuidStr) }.getOrNull() ?: return

        // Cancelar el drop y arrancar recarga
        event.isCancelled = true

        if (WeaponStateManager.isReloading(player, weaponUuid)) {
            player.sendActionBar(Component.text("§cYa estás recargando."))
            return
        }

        if (WeaponStateManager.getAmmo(player, weaponUuid) >= family.magazineSize) {
            player.sendActionBar(Component.text("§7El cargador ya está lleno."))
            return
        }

        WeaponStateManager.startReload(player, weaponUuid, family.reloadTicks, family.magazineSize, plugin)
    }
}