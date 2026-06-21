package com.nikoneko.eternalReverie.loot

/**
 * Estructura de datos para una tabla de loot de Área/Instancia, serializable
 * 1:1 a/desde JSON vía Gson (simetría completa para poder REGENERAR tablas
 * programáticamente a futuro, ej. al agregar materiales de Vestigios).
 *
 * Los 4 rolls (materials/blueprints/catalysts/currency) son independientes
 * entre sí: cada uno tira su propio chance por separado, pudiendo no caer
 * ninguno, todos, o cualquier combinación.
 */
data class AreaLootData(
    val displayName: String,
    val level: Int,
    val materials: MaterialRollConfig,
    val blueprints: BlueprintRollConfig,
    val catalysts: CatalystRollConfig,
    val currency: CurrencyRollConfig
)

data class MaterialRollConfig(
    val minRarity: String,
    val maxRarity: String,
    val minAmount: Int,
    val maxAmount: Int,
    val chance: Double
)

data class BlueprintRollConfig(
    val minRarity: String,
    val maxRarity: String,
    val chance: Double
)

data class CatalystRollConfig(
    val chance: Double
)

data class CurrencyRollConfig(
    val minAmount: Int,
    val maxAmount: Int,
    val chance: Double
)
