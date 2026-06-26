package com.nikoneko.eternalReverie.food

/**
 * Stats del jugador que pueden ser modificadas por efectos de alimentos.
 * Cada valor mapea directamente a un campo de PlayerStats.EquipmentStats
 * o a una stat especial manejada por separado (MOVILIDAD, VELOCIDAD_ATAQUE).
 */
enum class FoodStat {
    FUERZA,
    DEFENSA,
    RESISTENCIA,
    PRECISION,
    DESTREZA,
    VITALIDAD,
    MOVILIDAD,
    SUERTE,
    VELOCIDAD_ATAQUE
}
