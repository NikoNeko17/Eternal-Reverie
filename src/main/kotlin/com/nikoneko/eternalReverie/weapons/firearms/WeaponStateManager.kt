package com.nikoneko.eternalReverie.weapons.firearms

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.WeaponClass
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

object WeaponStateManager {

    private val states = mutableMapOf<Pair<UUID, UUID>, WeaponState>()

    val firearmClasses = setOf(WeaponClass.PISTOLA, WeaponClass.ESCOPETA, WeaponClass.RIFLE)

    // ======================
    // Utilidades internas
    // ======================

    private fun getKey(player: Player, weaponUuid: UUID): Pair<UUID, UUID> =
        player.uniqueId to weaponUuid

    private fun getState(player: Player, weaponUuid: UUID): WeaponState =
        states.getOrPut(getKey(player, weaponUuid)) { WeaponState() }

    fun getWeaponFamily(weapon: ItemStack): WeaponFamily? = runCatching { WeaponFamily.valueOf(weapon.itemMeta.persistentDataContainer.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: "") }.getOrNull()

    fun getAmmoData(player: Player, weapon: ItemStack): Pair<Int, Int?> = Pair(getAmmo(player,
        UUID.fromString(weapon.itemMeta.persistentDataContainer.get(Keys.INSTANCE_UUID, PersistentDataType.STRING))),
        getWeaponFamily(weapon)?.magazineSize)

    // El debuff de Hielo (Congelación) reduce attackSpeed efectivo, alargando
    // el cooldown real sin tocar el attackSpeed base del arma en sí.
    private fun getCooldownMillis(player: Player, attackSpeed: Double): Double {
        val debuffMultiplier = AttackSpeedDebuff.getActiveMultiplier(player)
        val effectiveAttackSpeed = (attackSpeed * debuffMultiplier).coerceAtLeast(0.05)
        return 1000.0 / effectiveAttackSpeed
    }

    // ======================
    // Cooldown
    // ======================

    fun trigger(player: Player, weaponUuid: UUID) {
        getState(player, weaponUuid).lastShot = System.currentTimeMillis()
    }

    fun canShoot(player: Player, weaponUuid: UUID, attackSpeed: Double): Boolean {
        val state = getState(player, weaponUuid)
        if (state.isReloading) return false
        // ammo == 0 con magazineSize == 0 significa que el arma no tiene sistema de munición → se permite
        // el caller (onFireWeaponEvent) ya verifica ammo > 0 solo si magazineSize > 0
        val elapsed = System.currentTimeMillis() - state.lastShot
        return elapsed >= getCooldownMillis(player, attackSpeed)
    }

    fun getCooldownProgress(player: Player, weaponUuid: UUID, attackSpeed: Double): Double {
        val state = getState(player, weaponUuid)
        if (state.lastShot == 0L) return 1.0
        val elapsed = System.currentTimeMillis() - state.lastShot
        return (elapsed / getCooldownMillis(player, attackSpeed)).coerceAtLeast(0.0)
    }

    // ======================
    // Bala en recámara
    // ======================

    fun hasRoundChambered(player: Player, weaponUuid: UUID): Boolean =
        getState(player, weaponUuid).chambered

    fun setRoundChambered(player: Player, weaponUuid: UUID, value: Boolean) {
        getState(player, weaponUuid).chambered = value
    }

    // ======================
    // Munición
    // ======================

    fun getAmmo(player: Player, weaponUuid: UUID): Int =
        getState(player, weaponUuid).ammo

    fun setAmmo(player: Player, weaponUuid: UUID, amount: Int) {
        getState(player, weaponUuid).ammo = amount
    }

    fun addAmmo(player: Player, weaponUuid: UUID, amount: Int) {
        getState(player, weaponUuid).ammo += amount.coerceAtLeast(0)
    }

    // ======================
    // Recarga
    // ======================

    fun isReloading(player: Player, weaponUuid: UUID): Boolean =
        getState(player, weaponUuid).isReloading

    fun startReload(
        player: Player,
        weaponUuid: UUID,
        reloadTicks: Int,
        magazineSize: Int,
        plugin: EternalReverie
    ) {
        val state = getState(player, weaponUuid)
        if (state.isReloading) return
        if (state.ammo >= magazineSize) return

        state.isReloading = true

        object : BukkitRunnable() {
            var ticksLeft = reloadTicks

            override fun run() {
                if (!player.isOnline) {
                    state.isReloading = false
                    cancel()
                    return
                }

                if (ticksLeft > 0) {
                    val progress = 1.0 - (ticksLeft.toDouble() / reloadTicks)
                    val filled = (progress * 20).toInt()
                    val bar = "█".repeat(filled) + "░".repeat(20 - filled)
                    player.sendActionBar(Component.text("§eRecargando  §f[$bar]"))
                    ticksLeft--
                } else {
                    state.ammo = magazineSize
                    state.isReloading = false
                    player.sendActionBar(Component.text("§a¡Listo!  §f[████████████████████]"))
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    // ======================
    // Semi-auto
    // ======================

    // Paper repite PlayerInteractEvent en cada tick mientras se mantiene click derecho.
    // Distinguimos "click nuevo" de "evento repetido por sostener" midiendo el gap
    // desde el último evento de interact recibido para esta arma. Un gap mayor al
    // umbral significa que el jugador soltó y volvió a clickear.
    private const val NEW_CLICK_GAP_MILLIS = 150L  // ~3 ticks de margen

    fun isNewClick(player: Player, weaponUuid: UUID): Boolean {
        val state = getState(player, weaponUuid)
        val now = System.currentTimeMillis()
        val isNew = (now - state.lastInteractEventAt) >= NEW_CLICK_GAP_MILLIS
        state.lastInteractEventAt = now
        return isNew
    }


    // ======================
    // Limpieza
    // ======================

    fun clearPlayer(player: Player) {
        states.keys.removeIf { it.first == player.uniqueId }
    }

    fun clearWeapon(player: Player, weaponUuid: UUID) {
        states.remove(getKey(player, weaponUuid))
    }

    fun clearAll() {
        states.clear()
    }
}