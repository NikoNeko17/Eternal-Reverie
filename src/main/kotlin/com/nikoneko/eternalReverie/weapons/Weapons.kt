package com.nikoneko.eternalReverie.weapons

import org.bukkit.Material

data class WeaponData(
    val id: String,

    // Información básica
    val name: String,
    val description: String,

    // Clasificación
    val weaponClass: WeaponClass,
    val family: WeaponFamily,
    val rarity: WeaponRarity,

    // Apariencia
    val material: Material,

    // Combate
    val damage: Double,
    val attackSpeed: Double,

    // Durabilidad
    val maxDurability: Int,

    // Habilidad exclusiva
    val skill: WeaponSkill,

    // Afinidades
    val affinities: List<AffinityWeight>
)

enum class WeaponRarity(
    val stars: Int,
    val durability: Int
) {
    COMMON(1, 50),
    RARE(2, 75),
    EPIC(3, 110),
    LEGENDARY(4, 135),
    MYTHIC(5, 160),
    ONIRIC(6, 210),
    ASCENDED(7, 262)
}

enum class WeaponClass {
    ESPADA,
    LANZA,
    GUANTES,
    HACHA,
    ARCO,
    PISTOLA,
    ESCOPETA,
    RIFLE
}

enum class WeaponFamily(
    val weaponClass: WeaponClass,
    val damageMultiplier: Double,
    val attackSpeed: Double,
    val mobility: Double,
    val reach: Double,
    val item: Material
) {

    ESPADA_RECTA(
        WeaponClass.ESPADA,
        1.0,
        1.6,
        0.0,
        3.0,
        Material.IRON_SWORD
    ),

    SABLE(
        WeaponClass.ESPADA,
        1.15,
        1.6,
        0.0,
        2.8,
        Material.GOLDEN_SWORD
    ),

    MANDOBLE(
        WeaponClass.ESPADA,
        1.6,
        0.8,
        -0.25,
        3.5,
        Material.STONE_SWORD
    ),

    PISTOLA(
        WeaponClass.PISTOLA,
        1.0,
        2.0,
        0.1,
        2.5,
        Material.IRON_HOE
    ),

    RIFLE_FRANCOTIRADOR(
        WeaponClass.RIFLE,
        1.6,
        0.6,
        -0.4,
        4.8,
        Material.IRON_HOE
    )
}

enum class Affinity {
    SANGRE,
    FUEGO,
    HIELO,
    ELECTRICIDAD,
    VENENO,
    ATADURA,
    FRAGILIDAD
}

data class AffinityWeight(
    val affinity: Affinity,
    val weight: Double
)

enum class WeaponSkill {
    PARRY,
    ESTOCADA,
    REMATE,
    BERSERK,
    CONCENTRACION,
    MUNICION_INFINITA
}