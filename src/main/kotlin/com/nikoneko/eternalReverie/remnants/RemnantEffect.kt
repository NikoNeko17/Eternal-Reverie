package com.nikoneko.eternalReverie.remnants

/**
 * Tipo general de un efecto (a qué aspecto del juego pertenece).
 * El Subtipo describe algo más específico dentro de ese Tipo — por ejemplo
 * Tipo=EQUIPMENT, Subtipo="Rifle de Combate"; Tipo=AFFINITY, Subtipo="Sangre".
 *
 * El Subtipo se deja como String libre (no enum anidado) porque su universo
 * de valores posibles depende del Tipo y de contenido que crece con el
 * tiempo (nuevas familias de arma, nuevas afinidades) — un enum anidado por
 * Tipo obligaría a tocar código Kotlin por cada combinación nueva.
 */
enum class RemnantEffectType {
    EQUIPMENT,
    AFFINITY,
    STAT,
    ECONOMY,
    UTILITY
}

/**
 * Un efecto posible de Vestigio. NO almacena su valor final calculado —
 * eso se deriva en runtime (ver RemnantSynthesizer) a partir del peso
 * acumulado que recibió durante la síntesis. Esto permite rebalancear
 * pesos/rangos sin migrar Vestigios ya creados: el próximo recálculo
 * (ver "El Sueño aprende", sección de Reciclaje/Recalculo) usa los valores
 * actuales del enum automáticamente.
 *
 * center = (weightMax - weightMin) / 2.0 + weightMin
 * El peso acumulado que caiga exactamente en `center` da el valor MÁXIMO
 * del efecto (Mx). Alejarse del centro hacia cualquier lado (más o menos
 * peso) decae el valor hacia el mínimo (Mn), siguiendo una función campana
 * simétrica — ver RemnantSynthesizer.bellCurveValue().
 *
 * loreTemplate usa "{value}" como placeholder, reemplazado al generar el
 * lore final del ítem con el valor ya calculado y formateado.
 */

enum class EffectOperation{
    ADDITION, PERCENTAGE
}

enum class RemnantEffect(
    val displayName: String,
    val subtype: String,
    val type: RemnantEffectType,
    val weightMin: Int,
    val weightMax: Int,
    val valueMin: Double,
    val valueMax: Double,
    val loreTemplate: String,
    val operation: EffectOperation
) {
    // ── Ejemplos de referencia — completar el catálogo real por separado ──

    HEALING_IN_COMBAT_BOOST(
        displayName = "Curación en Combate",
        subtype = "Vitalidad",
        type = RemnantEffectType.STAT,
        weightMin = 2,
        weightMax = 10,
        valueMin = 0.1,
        valueMax = 0.5,
        loreTemplate = "Aumenta la curación en combate en un {value}%",
        EffectOperation.PERCENTAGE
    ),

    BLOOD_AFFINITY_WEAPON_DAMAGE(
        displayName = "Filo Sangriento",
        subtype = "Sangre",
        type = RemnantEffectType.AFFINITY,
        weightMin = 2,
        weightMax = 10,
        valueMin = 0.05,
        valueMax = 0.2,
        loreTemplate = "Aumenta el daño de armas con afinidad Sangre en un {value}%",
        EffectOperation.PERCENTAGE
    ),

    RIFLE_RELOAD_SPEED(
        displayName = "Recarga Táctica",
        subtype = "Rifle de Combate",
        type = RemnantEffectType.EQUIPMENT,
        weightMin = 5,
        weightMax = 17,
        valueMin = 0.15,
        valueMax = 0.5, // "hasta la mitad" del tiempo de recarga base
        loreTemplate = "Disminuye el tiempo de recarga del Rifle de Combate en un {value}%",
        EffectOperation.PERCENTAGE
    );

    /** Punto medio del rango de peso — donde este efecto alcanza su valor máximo (Mx). */
    val weightCenter: Double
        get() = (weightMax - weightMin) / 2.0 + weightMin
}
