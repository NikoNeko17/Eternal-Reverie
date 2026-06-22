package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.weapons.Affinity

enum class MaterialRarity(
    val stars: Int
) {
    COMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4),
    MYTHIC(5),
    ONIRIC(6)
}

data class MaterialData(
    val id: String,
    val name: String,
    val rarity: MaterialRarity,

    // Modificadores de arma (% aditivo)
    val weaponDamageBonus: Double,
    val weaponAttackSpeedBonus: Double,
    val weaponMobilityBonus: Double,

    // Modificadores de armadura (% aditivo)
    val armorDefenseBonus: Double,
    val armorMobilityBonus: Double,

    // Atributos (puntos planos, aditivo)
    val vitality: Double,
    val resistance: Double,
    val strength: Double,
    val precision: Double,
    val dexterity: Double,
    val luck: Double,

    // Crafteo
    val durabilityModifier: Int,
    val fabricationCost: Int,

    // Afinidad (null si es material físico puro)
    val affinity: Affinity? = null,
    val affinityWeight: Int = 0
)