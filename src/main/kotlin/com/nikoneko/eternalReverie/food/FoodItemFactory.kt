package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.items.Keys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object FoodItemFactory {

    fun create(type: FoodType): ItemStack {
        val data = type.data
        val item = ItemStack(data.material)
        val meta = item.itemMeta ?: return item

        // Nombre del ítem
        meta.displayName(
            Component.text(data.displayName)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Bypass de hambre: el jugador puede comer aunque tenga el hambre llena
        @Suppress("UnstableApiUsage")
        meta.setFood(
            ItemMeta.Food.builder()
                .nutrition(0)
                .saturation(0f)
                .canAlwaysEat(true)
                .build()
        )

        // Lore
        meta.lore(buildLore(data))

        // PDC: identificador del tipo de comida
        meta.persistentDataContainer.set(
            Keys.FOOD_ID,
            PersistentDataType.STRING,
            data.id
        )

        item.itemMeta = meta
        return item
    }

    private fun buildLore(data: FoodData): List<Component> {
        val lore = mutableListOf<Component>()
        val noItalic = TextDecoration.ITALIC to TextDecoration.State.FALSE

        // "Alimento - Categoría"
        lore += Component.text("Alimento - ${data.category.displayName}")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

        // Rareza en estrellas
        val stars = "★".repeat(data.rarity.stars) + "☆".repeat(4 - data.rarity.stars)
        lore += Component.text(stars)
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

        lore += Component.empty()

        // Curación
        lore += Component.text("❦ +${data.healAmount} HP al consumir")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false)

        // Efectos
        if (data.effects.isNotEmpty()) {
            lore += Component.empty()
            lore += Component.text("Efectos:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)

            for (effect in data.effects) {
                lore += when (val t = effect.type) {
                    is FoodEffectType.StatModifier -> {
                        val sign   = if (t.multiplier >= 0) "+" else ""
                        val pct    = "${sign}${(t.multiplier * 100).toInt()}%"
                        val secs   = effect.durationTicks / 20
                        val color  = if (t.multiplier >= 0) NamedTextColor.AQUA else NamedTextColor.RED
                        Component.text("• ${statDisplayName(t.stat)}:", NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("$pct/${secs}s", color)
                            )
                    }
                    is FoodEffectType.AffinityMark -> {
                        val secs = effect.durationTicks / 20
                        Component.text("• ${t.affinity.name.lowercase().replaceFirstChar { it.uppercase() }}: ${secs}s")
                            .color(NamedTextColor.DARK_RED)
                            .decoration(TextDecoration.ITALIC, false)
                    }
                }
            }
        }

        return lore
    }

    private fun statDisplayName(stat: FoodStat) = when (stat) {
        FoodStat.FUERZA          -> "Fuerza"
        FoodStat.DEFENSA         -> "Defensa"
        FoodStat.RESISTENCIA     -> "Resistencia"
        FoodStat.PRECISION       -> "Precisión"
        FoodStat.DESTREZA        -> "Destreza"
        FoodStat.VITALIDAD       -> "Vitalidad"
        FoodStat.MOVILIDAD       -> "Movilidad"
        FoodStat.SUERTE          -> "Suerte"
        FoodStat.VELOCIDAD_ATAQUE -> "Vel. Ataque"
    }
}
