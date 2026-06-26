package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.weapons.Affinity

/**
 * Un efecto que puede tener un alimento al consumirse.
 *
 * StatModifier  — modifica una stat del jugador por un porcentaje
 *                 (positivo = buff, negativo = debuff).
 * AffinityMark  — aplica una marca de afinidad directamente al jugador
 *                 consumidor (ej. Veneno de Carne Cruda).
 */
sealed class FoodEffectType {
    /** multiplier: 0.10 = +10%, -0.20 = -20% */
    data class StatModifier(val stat: FoodStat, val multiplier: Double) : FoodEffectType()
    data class AffinityMark(val affinity: Affinity) : FoodEffectType()
}

/** Un efecto concreto con su duración en ticks (20 ticks = 1 segundo). */
data class FoodEffect(
    val type: FoodEffectType,
    val durationTicks: Int
)

/** Categoría visual mostrada en el lore del ítem. */
enum class FoodCategory(val displayName: String) {
    CARNE("Carne"),
    CARAMELO("Caramelo"),
    FRUTA("Fruta"),
    VERDURA("Verdura"),
    GRANO("Grano")
}

/**
 * Definición estática completa de un tipo de alimento.
 *
 * @param id            Identificador interno único (guardado en PDC del ítem).
 * @param displayName   Nombre mostrado en el ítem.
 * @param category      Categoría para el lore.
 * @param rarity        Rareza (1★–4★).
 * @param healAmount    HP que cura al consumirse (determinado por rareza).
 * @param material      Material de Bukkit para representar el ítem visualmente.
 * @param effects       Lista de efectos que aplica al consumirse.
 */
data class FoodData(
    val id: String,
    val displayName: String,
    val category: FoodCategory,
    val rarity: com.nikoneko.eternalReverie.items.Rarity,
    val healAmount: Int,
    val material: org.bukkit.Material,
    val effects: List<FoodEffect>
)
