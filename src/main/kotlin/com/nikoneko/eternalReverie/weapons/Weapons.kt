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
    val item: Material,
    val displayName: String,
    val magazineSize: Int = 0,
    val reloadTicks: Int = 0,
    val isSemiAuto: Boolean = false
) {

    // ── Espadas ───────────────────────────────────────────────────────────────────

    ESPADA_RECTA(
        WeaponClass.ESPADA, 1.0, 1.6, 0.0, 3.0,
        Material.IRON_SWORD, "Espada Recta"
    ),
    SABLE(
        WeaponClass.ESPADA, 1.15, 1.6, 0.0, 2.8,
        Material.GOLDEN_SWORD, "Sable"
    ),
    MANDOBLE(
        WeaponClass.ESPADA, 1.6, 0.6, -0.40, 4.8,
        Material.STONE_SWORD, "Mandoble"
    ),

    // ── Lanzas ────────────────────────────────────────────────────────────────────

    LANZA_MILITAR(
        WeaponClass.LANZA, 0.9, 2.0, 0.0, 4.0,
        Material.GOLDEN_HOE, "Lanza Militar"
    ),
    PICA(
        WeaponClass.LANZA, 0.8, 3.4, -0.10, 3.0,
        Material.GOLDEN_HOE, "Pica"
    ),
    ALABARDA(
        WeaponClass.LANZA, 1.35, 0.8, -0.25, 4.0,
        Material.GOLDEN_AXE, "Alabarda"
    ),

    // ── Guantes ───────────────────────────────────────────────────────────────────

    NUDILLERAS(
        WeaponClass.GUANTES, 0.8, 2.6, 0.4, 2.0,
        Material.PLAYER_HEAD, "Nudilleras"
    ),
    GUANTELETES(
        WeaponClass.GUANTES, 0.9, 2.0, 0.2, 2.5,
        Material.PLAYER_HEAD, "Guanteletes"
    ),
    GARRAS(
        WeaponClass.GUANTES, 0.75, 3.4, 0.4, 2.0,
        Material.GHAST_TEAR, "Garras"
    ),

    // ── Hachas ────────────────────────────────────────────────────────────────────

    HACHA_DE_MANO(
        WeaponClass.HACHA, 1.15, 0.6, 0.0, 2.8,
        Material.STONE_AXE, "Hacha de Mano"
    ),
    HACHA_DE_GUERRA(
        WeaponClass.HACHA, 1.35, 0.8, -0.1, 3.5,
        Material.IRON_AXE, "Hacha de Guerra"
    ),
    HACHA_BIPENNE(
        WeaponClass.HACHA, 1.6, 0.6, -0.25, 4.0,
        Material.IRON_AXE, "Hacha Bipenne"
    ),

    // ── Arcos ─────────────────────────────────────────────────────────────────────

    ARCO_CORTO(
        WeaponClass.ARCO, 0.9, 2.6, 0.1, 2.5,
        Material.BOW, "Arco Corto"
    ),
    ARCO_LARGO(
        WeaponClass.ARCO, 1.0, 1.2, -0.1, 4.0,
        Material.BOW, "Arco Largo"
    ),
    ARCO_COMPUESTO(
        WeaponClass.ARCO, 1.15, 1.6, -0.25, 3.5,
        Material.BOW, "Arco Compuesto"
    ),

    // ── Pistolas ──────────────────────────────────────────────────────────────────

    PISTOLA(
        WeaponClass.PISTOLA, 1.0, 2.0, 0.1, 2.5,
        Material.IRON_HOE, "Pistola",
        magazineSize = 12, reloadTicks = 40, isSemiAuto = true
    ),
    REVOLVER(
        WeaponClass.PISTOLA, 1.15, 1.2, 0.0, 3.0,
        Material.STONE_HOE, "Revolver",
        magazineSize = 6, reloadTicks = 60, isSemiAuto = true
    ),
    CANON_DE_MANO(
        WeaponClass.PISTOLA, 1.35, 0.8, -0.1, 3.5,
        Material.IRON_HORSE_ARMOR, "Cañón de Mano",
        magazineSize = 3, reloadTicks = 80, isSemiAuto = false
    ),

    // ── Escopetas ─────────────────────────────────────────────────────────────────

    ESCOPETA_DE_CORREDERA(
        WeaponClass.ESCOPETA, 1.35, 0.8, -0.1, 3.5,
        Material.IRON_HORSE_ARMOR, "Escopeta de Corredera",
        magazineSize = 5, reloadTicks = 70, isSemiAuto = true
    ),
    ESCOPETA_AUTOMATICA(
        WeaponClass.ESCOPETA, 1.0, 2.0, 0.0, 2.8,
        Material.DIAMOND_HORSE_ARMOR, "Escopeta Automática",
        magazineSize = 8, reloadTicks = 50, isSemiAuto = false
    ),
    ESCOPETA_DE_COMBATE(
        WeaponClass.ESCOPETA, 1.15, 1.6, -0.1, 3.0,
        Material.GOLDEN_HORSE_ARMOR, "Escopeta de Combate",
        magazineSize = 6, reloadTicks = 55, isSemiAuto = true
    ),

    // ── Rifles ────────────────────────────────────────────────────────────────────

    RIFLE_DE_COMBATE(
        WeaponClass.RIFLE, 1.0, 1.6, 0.0, 3.0,
        Material.IRON_HORSE_ARMOR, "Rifle de Combate",
        magazineSize = 20, reloadTicks = 60, isSemiAuto = false
    ),
    CARABINA_DE_ASALTO(
        WeaponClass.RIFLE, 0.9, 2.0, -0.1, 2.8,
        Material.IRON_HORSE_ARMOR, "Carabina de Asalto",
        magazineSize = 25, reloadTicks = 55, isSemiAuto = true
    ),
    RIFLE_FRANCOTIRADOR(
        WeaponClass.RIFLE, 1.6, 0.6, -0.4, 4.8,
        Material.IRON_HORSE_ARMOR, "Francotirador",
        magazineSize = 5, reloadTicks = 80, isSemiAuto = false
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