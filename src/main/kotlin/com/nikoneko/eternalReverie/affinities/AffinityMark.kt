package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.weapons.Affinity

/**
 * Una instancia activa de Marca sobre una entidad (jugador o NPC).
 * Sin stacks: la Marca está activa o no, su efecto es fijo mientras dure.
 * `durationTicks` es lo único que varía (se extiende con cada reaplicación,
 * capeado en MAX_DURATION_TICKS).
 */
data class AffinityMark(
    val affinity: Affinity,
    var durationTicks: Int,
    // Para Fuego: el daño del golpe que generó/refrescó la Marca, usado por su DoT
    // proporcional. Para las demás afinidades no se usa.
    var sourceHitDamage: Double = 0.0
) {
    companion object {
        const val MAX_DURATION_TICKS = 15 * 20   // 15s tope (bajado de 30s)
        const val REAPPLY_BONUS_TICKS = 3 * 20    // +3s por cada reaplicación
    }
}

/** Configuración base de cada Marca: duración inicial y parámetros de su efecto. */
data class MarkConfig(
    val affinity: Affinity,
    val baseDurationTicks: Int,
    val effectValue: Double,         // significado depende de la Marca (ver MarkEffects)
    val tickIntervalTicks: Int = 20  // cada cuánto se aplica el efecto periódico (1s por defecto)
)

object MarkRegistry {

    val configs: Map<Affinity, MarkConfig> = mapOf(
        Affinity.SANGRE to MarkConfig(
            affinity = Affinity.SANGRE,
            baseDurationTicks = 8 * 20,
            effectValue = 0.15 // 15% del daño infligido por el portador se cura, mientras esté activa
        ),
        Affinity.FUEGO to MarkConfig(
            affinity = Affinity.FUEGO,
            baseDurationTicks = 6 * 20,
            effectValue = 0.10 // 10% del daño del golpe que generó la Marca, por segundo (DoT)
        ),
        Affinity.HIELO to MarkConfig(
            affinity = Affinity.HIELO,
            baseDurationTicks = 3 * 20, // corta y fuerte
            effectValue = 0.40 // -40% velocidad de movimiento Y -30% velocidad de ataque (ver MarkEffects)
        ),
        Affinity.ELECTRICIDAD to MarkConfig(
            affinity = Affinity.ELECTRICIDAD,
            baseDurationTicks = 5 * 20,
            effectValue = 0.10 // 10% de la regeneración de Stamina del enemigo es robada hacia el atacante
        ),
        Affinity.VENENO to MarkConfig(
            affinity = Affinity.VENENO,
            baseDurationTicks = 10 * 20, // progresiva y prolongada
            effectValue = 0.15 // -15% velocidad mov. + -25% regen. HP/Stamina (ver MarkEffects)
        ),
        Affinity.ATADURA to MarkConfig(
            affinity = Affinity.ATADURA,
            baseDurationTicks = 2 * 20,
            effectValue = 1.0 // inmoviliza + deshabilita habilidades/ítems (binario, sin escala)
        ),
        Affinity.FRAGILIDAD to MarkConfig(
            affinity = Affinity.FRAGILIDAD,
            baseDurationTicks = 6 * 20,
            effectValue = 0.25 // +25% daño final recibido por el portador de la Marca
        )
    )
}
