package com.nikoneko.eternalReverie.loot

/**
 * Genera/regenera las 13 entradas zone_1..zone_13 en AreaLootRegistry desde
 * código (no un JSON escrito a mano), siguiendo el mapeo de Areas.md:
 *   zone_1..7   = Distritos (Avaricia, Soberbia, Envidia, Ira, Pereza, Gula, Lujuria)
 *   zone_8..10  = Interpolación Efímera I/II/III
 *   zone_11..12 = Paraíso Onírico I/II
 *   zone_13     = Paraíso Onírico: Desafío
 *
 * Todos los valores son de partida (placeholder razonable) EXCEPTO
 * materials.minRarity/maxRarity, que quedan como "TODO" explícito: es el único
 * campo que el usuario quiere revisar y ajustar zona por zona a mano.
 *
 * Llamar AreaLootGenerator.generateIfMissing() en onEnable, DESPUÉS de
 * AreaLootRegistry.load(plugin). Por defecto NO pisa entradas ya existentes
 * (para no perder ediciones manuales en cada restart); usar generateAll(force=true)
 * si en algún momento se quiere forzar la regeneración completa.
 */
object AreaLootGenerator {

    private val rarityOrder = listOf(
        "COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "ONIRIC", "ASCENDED"
    )

    private val zoneNames: Map<Int, String> = mapOf(
        1 to "Distrito de la Pereza",
        2 to "Distrito de la Gula",
        3 to "Distrito de la Avaricia",
        4 to "Distrito de la Ira",
        5 to "Distrito de la Envidia",
        6 to "Distrito de la Lujuria",
        7 to "Distrito de la Soberbia",
        8 to "Interpolación Efímera I",
        9 to "Interpolación Efímera II",
        10 to "Interpolación Efímera III",
        11 to "Paraíso Onírico I",
        12 to "Paraíso Onírico II",
        13 to "Paraíso Onírico: Desafío"
    )

    /** Genera solo las zonas que todavía no existen en el registro (no pisa ediciones manuales). */
    fun generateIfMissing() {
        for (level in 1..13) {
            val id = "zone_$level"
            if (AreaLootRegistry.get(id) != null) continue
            AreaLootRegistry.put(id, buildZone(level))
        }
    }

    /** Regenera TODAS las zonas, pisando cualquier edición manual existente. Usar con cuidado. */
    fun generateAll(force: Boolean = false) {
        if (!force) return
        for (level in 1..13) {
            AreaLootRegistry.put("zone_$level", buildZone(level))
        }
    }

    private fun rarityForLevel(level: Int, offset: Int = 0): String {
        val idx = (((level - 1) / 2) + offset).coerceIn(0, rarityOrder.lastIndex)
        return rarityOrder[idx]
    }

    private fun buildZone(level: Int): AreaLootData {
        val name = zoneNames[level] ?: "Zona $level"

        val minAmount = 3 + (level - 1) / 5
        val maxAmount = 5 + (level - 1) / 4

        val blueprintChance = (0.20 + (level - 4) * 0.01).coerceIn(0.0, 0.45)
        val catalystChance = 0.01 + maxOf(0, level - 7) * 0.01

        val currencyMin = 3 + (level - 1)
        val currencyMax = 7 + (level - 1) * 2

        val bpMax = rarityForLevel(level)
        val bpMin = rarityForLevel(level, offset = -1)

        return AreaLootData(
            displayName = name,
            level = level,
            materials = MaterialRollConfig(
                minRarity = "TODO",
                maxRarity = "TODO",
                minAmount = minAmount,
                maxAmount = maxAmount,
                chance = 1.0
            ),
            blueprints = BlueprintRollConfig(
                minRarity = bpMin,
                maxRarity = bpMax,
                chance = roundTo2(blueprintChance)
            ),
            catalysts = CatalystRollConfig(
                chance = roundTo2(catalystChance)
            ),
            currency = CurrencyRollConfig(
                minAmount = currencyMin,
                maxAmount = currencyMax,
                chance = 1.0
            )
        )
    }

    private fun roundTo2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
