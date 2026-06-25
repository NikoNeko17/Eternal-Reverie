package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.items.Rarity

/**
 * Categorías de Vestigio según Remnants.md, usadas para organizar efectos
 * y (a futuro) Transmutación.
 */
enum class RemnantCategory {
    ARMA,
    AFINIDAD,
    ESTADISTICA,
    ECONOMIA,
    UTILIDAD
}

/** Eterno = tiene Núcleo, persiste a la muerte. Efímero = sin Núcleo, se destruye al morir. */
enum class RemnantKind {
    ETERNO,
    EFIMERO
}

/**
 * El nivel máximo de CUALQUIER Vestigio es esta constante global, no derivado
 * de la rareza. La rareza solo afecta costo de mejora (Transmutación, a futuro),
 * nunca el techo de nivel.
 */
const val MAX_VESTIGIO_LEVEL = 5

data class RemnantData(
    val id: String,
    val name: String,
    val description: String,
    val category: RemnantCategory,
    val kind: RemnantKind,
    val rarity: Rarity,
    /** Valor del efecto en cada nivel (índice 0 = nivel I, ..., índice MAX_VESTIGIO_LEVEL-1 = nivel máximo). */
    val valuesByLevel: List<Double>
) {
    init {
        require(valuesByLevel.size == MAX_VESTIGIO_LEVEL) {
            "valuesByLevel debe tener exactamente $MAX_VESTIGIO_LEVEL entradas (una por nivel), tiene ${valuesByLevel.size}"
        }
    }

    fun valueAt(level: Int): Double {
        val clamped = level.coerceIn(1, MAX_VESTIGIO_LEVEL)
        return valuesByLevel[clamped - 1]
    }
}

/**
 * Registro de tipos de Vestigio. Enum (no YAML/JSON) siguiendo el mismo
 * criterio que MaterialType: el usuario recarga el servidor seguido al iterar,
 * así que el costo de "editar = recompilar" no pesa en la práctica.
 */
enum class RemnantType(val data: RemnantData) {

    // Primer Vestigio de prueba: Estadística, Vitalidad +10 por nivel, lineal 1:1.
    VITALIDAD_MENOR(
        RemnantData(
            id = "vitalidad_menor",
            name = "Vestigio de Vitalidad Menor",
            description = "Aumenta la Vitalidad máxima del portador.",
            category = RemnantCategory.ESTADISTICA,
            kind = RemnantKind.ETERNO,
            rarity = Rarity.COMMON,
            valuesByLevel = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        )
    )
}
