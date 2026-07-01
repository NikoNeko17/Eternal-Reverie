package com.nikoneko.eternalReverie.remnants.athanor

import com.nikoneko.eternalReverie.crafting.MaterialRarity
import com.nikoneko.eternalReverie.crafting.MaterialType
import com.nikoneko.eternalReverie.remnants.EffectOperation
import com.nikoneko.eternalReverie.remnants.RemnantEffect
import kotlin.math.abs

/**
 * Un efecto ya resuelto para un Vestigio concreto: el RemnantEffect elegido
 * junto con su valor numérico final, ya calculado para esta síntesis
 * particular (ver bellCurveValue). No se persiste — se recalcula siempre
 * desde los materiales guardados en PDC (ver RemnantKeys_ADDITION).
 */
data class ResolvedRemnantEffect(
    val effect: RemnantEffect,
    val accumulatedWeight: Int,
    val value: Double
) {
    /** Lore final, con el placeholder {value} ya reemplazado y formateado. */
    fun renderLore(): String {
        val formatted = "%.1f".format(if (effect.operation == EffectOperation.PERCENTAGE)
        value * 100 else value)
        return effect.loreTemplate.replace("{value}", formatted)
    }
}

/**
 * Resultado completo de una síntesis/recálculo. Si effects está vacío,
 * corresponde a un Vestigio Neutro (sin materiales con remnantEffects, o
 * fallback tras un error de recálculo real).
 */
data class SynthesizedRemnant(
    val effects: List<ResolvedRemnantEffect>,
    val rarity: MaterialRarity,
    val materials: List<MaterialType>,
    val level: Int = 1,
    val cellRarity: MaterialRarity
) {
    val isNeutral: Boolean get() = effects.isEmpty()

    /** El primer efecto (índice 0, el más cercano a su centro) nombra al Vestigio. */
    val primaryEffect: RemnantEffect? get() = effects.firstOrNull()?.effect
}

/**
 * Límite de efectos que un Vestigio puede portar según su rareza final.
 * Ajustar libremente según balance — valores de partida razonables.
 */
object RemnantEffectSlotLimits {
    val LIMITS: Map<MaterialRarity, Int> = mapOf(
        MaterialRarity.COMMON to 2,
        MaterialRarity.RARE to 3,
        MaterialRarity.EPIC to 4,
        MaterialRarity.LEGENDARY to 5,
        MaterialRarity.MYTHIC to 6,
        MaterialRarity.ONIRIC to 7
    )

    fun forRarity(rarity: MaterialRarity): Int = LIMITS[rarity] ?: 2
}

object RemnantSynthesizer {

    val DEFAULT_REMNANT = SynthesizedRemnant(emptyList(), MaterialRarity.COMMON, emptyList(), 1, MaterialRarity.COMMON)

    /**
     * Ejecuta la síntesis completa a partir de los 4 materiales (slots de la
     * Estrella) y la rareza sellada de la Celda de Esencia (slot central).
     *
     * sealedCellRarity establece el MÍNIMO asegurado de rareza final —
     * ver TRANSMUTATION_DESIGN.md, sección "La Celda establece el mínimo".
     */
    fun synthesize(materials: List<MaterialType>, sealedCellRarity: MaterialRarity): SynthesizedRemnant {
        require(materials.size == 5) { "La síntesis requiere exactamente 5 materiales." }

        val materialAverage = resolveAverageRarity(materials)
        val finalRarity = maxOf(materialAverage.stars, sealedCellRarity.stars)
            .let { stars -> MaterialRarity.entries.first { it.stars == stars } }

        val effects = resolveEffects(materials, finalRarity)

        return SynthesizedRemnant(effects, finalRarity, materials, 1, sealedCellRarity)
    }

    /**
     * Recalcula un Vestigio ya existente a partir de los materiales guardados
     * en su PDC (ver VESTIGIO_SYNTHESIS_MATERIALS). Este es el punto de
     * entrada para el rebalanceo dinámico ("El Sueño aprende del jugador").
     *
     * Cualquier excepción real durante el recálculo (no un simple resultado
     * vacío) da como resultado un Vestigio Neutro — nunca debe romper el
     * ítem del jugador.
     */
    fun recompute(
        materialIds: List<String>,
        sealedCellRarity: MaterialRarity
    ): SynthesizedRemnant {
        return try {
            val materials = materialIds.map { MaterialType.valueOf(it) }
            synthesize(materials, sealedCellRarity)
        } catch (_: Exception) {
            SynthesizedRemnant(effects = emptyList(), rarity = MaterialRarity.COMMON, materials = emptyList(), 1, MaterialRarity.COMMON)
        }
    }

    // ── Selección de efectos ────────────────────────────────────────────────

    private fun resolveEffects(materials: List<MaterialType>, finalRarity: MaterialRarity): List<ResolvedRemnantEffect> {
        val accumulated = mutableMapOf<RemnantEffect, Int>()

        for (material in materials) {
            for ((effect, weight) in material.data.remnantEffects) {
                accumulated[effect] = (accumulated[effect] ?: 0) + weight
            }
        }

        if (accumulated.isEmpty()) return emptyList() // Vestigio Neutro

        val limit = RemnantEffectSlotLimits.forRarity(finalRarity)

        // Ordenar por cercanía al centro (menor distancia = mejor candidato).
        // Empate exacto de distancia se resuelve por orden de declaración
        // del enum RemnantEffect (determinista).
        val ranked = accumulated.entries
            .sortedWith(
                compareBy(
                    { abs(it.key.weightCenter - it.value) },
                    { it.key.ordinal }
                )
            )
            .take(limit)

        return ranked.map { (effect, weight) ->
            ResolvedRemnantEffect(
                effect = effect,
                accumulatedWeight = weight,
                value = bellCurveValue(effect, weight) * finalRarity.stars
            )
        }
    }

    /**
     * Calcula el valor final de un efecto según qué tan cerca cayó el peso
     * acumulado respecto al centro de su rango.
     *
     * Función campana simétrica: en el centro exacto, valor = valueMax.
     * Alejarse hacia CUALQUIER lado (menos peso o más peso) decae
     * linealmente hacia valueMin, llegando a valueMin en los extremos
     * (weightMin o weightMax) o más allá.
     *
     * Esto premia la COMPRENSIÓN del material (acertar el peso justo) por
     * sobre simplemente acumular más peso posible.
     */
    fun bellCurveValue(effect: RemnantEffect, accumulatedWeight: Int): Double {
        val center = effect.weightCenter
        val halfRange = (effect.weightMax - effect.weightMin) / 2.0

        if (halfRange <= 0.0) return effect.valueMax // rango degenerado, evita división por 0

        val distance = abs(accumulatedWeight - center)
        val proximity = (1.0 - (distance / halfRange)).coerceIn(0.0, 1.0) // 1.0 = en el centro, 0.0 = en el borde o más allá

        return effect.valueMin + (effect.valueMax - effect.valueMin) * proximity
    }

    // ── Rareza promedio ──────────────────────────────────────────────────────

    // Promedio de rareza (stars) de los 4 materiales, redondeado hacia abajo.
    private fun resolveAverageRarity(materials: List<MaterialType>): MaterialRarity {
        val avgStars = materials.map { it.data.rarity.stars }.average()
        val flooredStars = avgStars.toInt().coerceIn(1, MaterialRarity.entries.maxOf { it.stars })
        return MaterialRarity.entries.first { it.stars == flooredStars }
    }
}