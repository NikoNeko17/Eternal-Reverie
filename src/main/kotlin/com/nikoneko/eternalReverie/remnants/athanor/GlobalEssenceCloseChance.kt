package com.nikoneko.eternalReverie.remnants.athanor

import com.nikoneko.eternalReverie.crafting.MaterialRarity

/**
 * Chance BASE de sellado por rango de rareza, global para todo el juego
 * (no varía por área). Representa la tendencia natural de un checkpoint a
 * cerrar la Celda al cruzar ese umbral, antes de aplicar la resistencia que
 * ofrece el área en la que se obtuvo la Esencia.
 */
object GlobalEssenceCloseChance {
    val CLOSE_CHANCE: Map<MaterialRarity, Double> = mapOf(
        MaterialRarity.COMMON to 0.9,
        MaterialRarity.RARE to 0.85,
        MaterialRarity.EPIC to 0.8
        // LEGENDARY/MYTHIC/ONIRIC reservados a Celdas de Ruinas con rarityCap mayor.
    )

    fun get(rarity: MaterialRarity): Double = CLOSE_CHANCE[rarity] ?: 1.0
}

/**
 * Perfil de checkpoint de Esencia para un Área — SOLO define ignoreChance,
 * la resistencia de esa área a que el checkpoint selle la Celda. La chance
 * de cierre base es GLOBAL (ver GlobalEssenceCloseChance), no depende del área.
 *
 * chance real de sellar en ese umbral =
 *     (GlobalEssenceCloseChance.get(rarity) - ignoreChance(rarity)).coerceIn(0.0, 1.0)
 *
 * Si el umbral evaluado == rarityCap de la celda, el sellado es SIEMPRE 100%
 * sin importar estos valores (ver EssenceCellManager.addEssenceAndCheck).
 */
data class AreaEssenceProfile(
    val areaId: String,
    val ignoreChance: Map<MaterialRarity, Double>
) {
    fun sealChanceAt(rarity: MaterialRarity): Double {
        val close = GlobalEssenceCloseChance.get(rarity)
        val ignore = ignoreChance[rarity] ?: 0.0
        return (close - ignore).coerceIn(0.0, 1.0)
    }
}

/**
 * Registro en memoria de perfiles por área. Se carga desde areas.json o un
 * archivo propio (essence_profiles.yml) — el loader concreto queda pendiente
 * de integrar con AreaLootRegistry existente.
 */
object AreaEssenceRegistry {

    private val profiles = mutableMapOf<String, AreaEssenceProfile>()

    fun register(profile: AreaEssenceProfile) {
        profiles[profile.areaId] = profile
    }

    fun get(areaId: String): AreaEssenceProfile? = profiles[areaId]

    fun clear() = profiles.clear()

    // Perfiles de ejemplo para no dejar el registro vacío en testing.
    // Reemplazar por carga real desde archivo cuando se integre con AreaLootRegistry.
    fun loadDefaults() {
        register(
            AreaEssenceProfile(
                areaId = "zone_1",
                ignoreChance = mapOf(
                    MaterialRarity.COMMON to 0.1,
                    MaterialRarity.RARE to 0.05,
                    MaterialRarity.EPIC to 0.02
                )
            )
        )
        register(
            AreaEssenceProfile(
                areaId = "zone_13",
                ignoreChance = mapOf(
                    MaterialRarity.COMMON to 0.45,
                    MaterialRarity.RARE to 0.45,
                    MaterialRarity.EPIC to 0.45
                )
            )
        )
    }
}