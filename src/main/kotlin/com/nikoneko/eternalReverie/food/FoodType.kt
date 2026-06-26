package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.items.Rarity
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.Material

/**
 * Registro central de todos los alimentos del plugin.
 * Añadir un alimento nuevo = añadir una entrada aquí.
 *
 * Convención de curación por rareza: 10 / 25 / 40 / 65
 * (índice 0-3 → 1★ a 4★)
 */
enum class FoodType(val data: FoodData) {

    RAW_MEAT(FoodData(
        id          = "raw_meat",
        displayName = "Carne Cruda",
        category    = FoodCategory.CARNE,
        rarity      = Rarity.COMMON,
        healAmount  = 10,
        material    = Material.BEEF,
        effects     = listOf(
            FoodEffect(FoodEffectType.StatModifier(FoodStat.FUERZA, 0.10), durationTicks = 300),  // 15s
            FoodEffect(FoodEffectType.AffinityMark(Affinity.VENENO),       durationTicks = 600)   // 30s
        )
    )),

    COOKED_MEAT(FoodData(
        id          = "cooked_meat",
        displayName = "Carne Cocinada",
        category    = FoodCategory.CARNE,
        rarity      = Rarity.RARE,
        healAmount  = 25,
        material    = Material.COOKED_BEEF,
        effects     = listOf(
            FoodEffect(FoodEffectType.StatModifier(FoodStat.FUERZA, 0.15), durationTicks = 600)   // 30s
        )
    )),

    SWEET_CANDY(FoodData(
        id          = "sweet_candy",
        displayName = "Caramelo Dulce",
        category    = FoodCategory.CARAMELO,
        rarity      = Rarity.COMMON,
        healAmount  = 10,
        material    = Material.SUGAR,
        effects     = listOf(
            FoodEffect(FoodEffectType.StatModifier(FoodStat.MOVILIDAD, 0.10), durationTicks = 400) // 20s
        )
    )),

    APPLE(FoodData(
        id          = "apple",
        displayName = "Manzana",
        category    = FoodCategory.FRUTA,
        rarity      = Rarity.COMMON,
        healAmount  = 10,
        material    = Material.APPLE,
        effects     = listOf(
            FoodEffect(FoodEffectType.StatModifier(FoodStat.PRECISION, 0.08), durationTicks = 400) // 20s
        )
    )),

    WHEAT_BREAD(FoodData(
        id          = "wheat_bread",
        displayName = "Pan de Trigo",
        category    = FoodCategory.GRANO,
        rarity      = Rarity.RARE,
        healAmount  = 25,
        material    = Material.BREAD,
        effects     = listOf(
            FoodEffect(FoodEffectType.StatModifier(FoodStat.RESISTENCIA, 0.12), durationTicks = 600) // 30s
        )
    ));

    companion object {
        private val byId = entries.associateBy { it.data.id }
        fun fromId(id: String): FoodType? = byId[id]
    }
}
