package com.nikoneko.eternalReverie.economy

import com.nikoneko.eternalReverie.items.Keys
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * Balance numérico de Chatarra (moneda del juego) por jugador, persistido en su
 * PersistentDataContainer. La Chatarra también existe como ItemStack físico
 * (ver CurrencyItem/CurrencyListener) que se "deposita" con click derecho,
 * convirtiéndose en este balance. No es fraccionable (Int).
 */
object CurrencyManager {

    fun getBalance(player: Player): Int =
        player.persistentDataContainer.get(Keys.CURRENCY_BALANCE, PersistentDataType.INTEGER) ?: 0

    fun setBalance(player: Player, amount: Int) {
        player.persistentDataContainer.set(
            Keys.CURRENCY_BALANCE,
            PersistentDataType.INTEGER,
            amount.coerceAtLeast(0)
        )
    }

    fun addBalance(player: Player, amount: Int) {
        setBalance(player, getBalance(player) + amount)
    }

    /** @return true si había balance suficiente y se descontó; false si no alcanzaba (no se toca nada). */
    fun tryRemoveBalance(player: Player, amount: Int): Boolean {
        val current = getBalance(player)
        if (current < amount) return false
        setBalance(player, current - amount)
        return true
    }

    fun hasEnough(player: Player, amount: Int): Boolean =
        getBalance(player) >= amount
}
