package com.nikoneko.eternalReverie.economy

import com.nikoneko.eternalReverie.items.Keys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Chatarra: la moneda física del juego. Cada unidad del ItemStack vale 1 de
 * balance al depositarse (click derecho en mano, ver CurrencyListener).
 * Pese al nombre humilde, es de alto valor (poco frecuente, se obtiene como
 * loot raro/recompensa, no como drop trivial).
 */
object CurrencyItem {

    const val VALUE_PER_UNIT = 1

    fun create(amount: Int = 1): ItemStack {
        val item = ItemStack(Material.NETHERITE_SCRAP, amount) // placeholder visual
        val meta = item.itemMeta

        meta.displayName(
            Component.text("Chatarra", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(
            listOf(
                Component.text("La moneda de los expedicionarios.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true),
                Component.text(""),
                Component.text("Click derecho para depositar 1 unidad.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        )
        meta.persistentDataContainer.set(Keys.IS_CURRENCY, PersistentDataType.BYTE, 1)
        meta.setMaxStackSize(99)

        item.itemMeta = meta
        return item
    }

    fun isCurrency(item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(Keys.IS_CURRENCY, PersistentDataType.BYTE)
    }
}
