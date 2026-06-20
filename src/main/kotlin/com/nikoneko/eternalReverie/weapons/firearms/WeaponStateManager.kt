package com.nikoneko.eternalReverie.weapons.firearms

import org.bukkit.entity.Player
import java.util.UUID

object WeaponStateManager {

    private val states =
        mutableMapOf<Pair<UUID, UUID>, WeaponState>()

    // ======================
    // Utilidades internas
    // ======================

    private fun getKey(
        player: Player,
        weaponUuid: UUID
    ): Pair<UUID, UUID> {

        return player.uniqueId to weaponUuid

    }

    private fun getState(
        player: Player,
        weaponUuid: UUID
    ): WeaponState {

        return states.getOrPut(
            getKey(
                player,
                weaponUuid
            )
        ) {

            WeaponState()

        }

    }

    // El debuff de Hielo (Congelación) reduce attackSpeed efectivo, alargando
    // el cooldown real sin tocar el attackSpeed base del arma en sí.
    private fun getCooldownMillis(
        player: Player,
        attackSpeed: Double
    ): Double {

        val debuffMultiplier = AttackSpeedDebuff.getActiveMultiplier(player)
        val effectiveAttackSpeed = (attackSpeed * debuffMultiplier).coerceAtLeast(0.05)

        return 1000.0 / effectiveAttackSpeed

    }

    // ======================
    // Cooldown
    // ======================

    fun trigger(
        player: Player,
        weaponUuid: UUID
    ) {

        getState(
            player,
            weaponUuid
        ).lastShot =
            System.currentTimeMillis()

    }

    fun canShoot(
        player: Player,
        weaponUuid: UUID,
        attackSpeed: Double
    ): Boolean {

        val state =
            getState(
                player,
                weaponUuid
            )
        val elapsed = System.currentTimeMillis() - state.lastShot
        return elapsed >=
            getCooldownMillis(
                player,
                attackSpeed
            )
    }

    fun getCooldownProgress(
        player: Player,
        weaponUuid: UUID,
        attackSpeed: Double
    ): Double {

        val state =
            getState(
                player,
                weaponUuid
            )

        if (state.lastShot == 0L)
            return 1.0

        val elapsed = System.currentTimeMillis() - state.lastShot

        return ( elapsed / getCooldownMillis(player, attackSpeed)).coerceAtLeast(0.0)

    }

    // ======================
    // Bala en recámara
    // ======================

    fun hasRoundChambered(
        player: Player,
        weaponUuid: UUID
    ): Boolean {
        return getState(
            player,
            weaponUuid
        ).chambered
    }

    fun setRoundChambered(
        player: Player,
        weaponUuid: UUID,
        value: Boolean
    ) {
        getState(
            player,
            weaponUuid
        ).chambered = value
    }

    // ======================
    // Munición
    // ======================

    fun getAmmo(
        player: Player,
        weaponUuid: UUID
    ): Int {

        return getState(
            player,
            weaponUuid
        ).ammo
    }

    fun setAmmo(
        player: Player,
        weaponUuid: UUID,
        amount: Int
    ) {

        getState(
            player,
            weaponUuid
        ).ammo = amount
    }

    fun addAmmo(
        player: Player,
        weaponUuid: UUID,
        amount: Int
    ) {

        getState(
            player,
            weaponUuid
        ).ammo += amount.coerceAtLeast(0)

    }

    // ======================
    // Limpieza
    // ======================

    fun clearPlayer(
        player: Player
    ) {

        states.keys
            .removeIf {
                it.first == player.uniqueId
            }
    }

    fun clearWeapon(
        player: Player,
        weaponUuid: UUID
    ) {
        states.remove(
            getKey(
                player,
                weaponUuid
            )
        )
    }

    fun clearAll() {
        states.clear()
    }
}