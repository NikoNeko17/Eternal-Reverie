package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.weapons.Affinity

/**
 * Una instancia activa de Marca sobre una entidad (jugador o NPC).
 * `stacks` no tiene tope (el doc descarta el límite de niveles); lo que limita
 * la Marca es exclusivamente `durationTicks`, que se resetea a 0 cuando expira.
 */
data class AffinityMark(
    val affinity: Affinity,
    var stacks: Int,
    var durationTicks: Int
) {
    companion object {
        const val MAX_DURATION_TICKS = 30 * 20    // 30s tope
        const val REAPPLY_BONUS_TICKS = 3 * 20     // +3s por cada aplicación nueva
    }
}

/** Configuración base de cada Marca: duración inicial y efecto por stack. */
data class MarkConfig(
    val affinity: Affinity,
    val baseDurationTicks: Int,
    val effectPerStack: Double, // significado depende de la Marca (ver MarkEffects)
    val tickIntervalTicks: Int = 20 // cada cuánto se aplica el efecto periódico (1s por defecto)
)

object MarkRegistry {

    // Valores de partida; ajustables sin tocar el resto del sistema.
    val configs: Map<Affinity, MarkConfig> = mapOf(
        Affinity.SANGRE to MarkConfig(
            affinity = Affinity.SANGRE,
            baseDurationTicks = 8 * 20,
            effectPerStack = 0.02 // 2% del HP máximo por stack, por tick de aplicación
        ),
        Affinity.FUEGO to MarkConfig(
            affinity = Affinity.FUEGO,
            baseDurationTicks = 8 * 20,
            effectPerStack = 1.5 // daño plano por stack, por tick de aplicación
        ),
        Affinity.HIELO to MarkConfig(
            affinity = Affinity.HIELO,
            baseDurationTicks = 10 * 20,
            effectPerStack = 0.08 // -8% velocidad de movimiento por stack
        ),
        Affinity.ELECTRICIDAD to MarkConfig(
            affinity = Affinity.ELECTRICIDAD,
            baseDurationTicks = 10 * 20,
            effectPerStack = 5.0 // -5 stamina máxima temporal por stack
        ),
        Affinity.VENENO to MarkConfig(
            affinity = Affinity.VENENO,
            baseDurationTicks = 12 * 20,
            effectPerStack = 0.10 // -10% regeneración HP/Stamina por stack
        ),
        Affinity.ATADURA to MarkConfig(
            affinity = Affinity.ATADURA,
            baseDurationTicks = 8 * 20,
            effectPerStack = 0.10 // -10% movilidad por stack
        ),
        Affinity.FRAGILIDAD to MarkConfig(
            affinity = Affinity.FRAGILIDAD,
            baseDurationTicks = 10 * 20,
            effectPerStack = 0.03 // +3% daño recibido por stack
        )
    )
}
