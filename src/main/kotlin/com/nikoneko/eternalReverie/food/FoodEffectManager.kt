package com.nikoneko.eternalReverie.food

import com.nikoneko.eternalReverie.weapons.Affinity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Número máximo de efectos de comida activos simultáneamente por jugador. */
const val MAX_ACTIVE_FOOD_EFFECTS = 3

/**
 * Un efecto de comida activo en un jugador en este momento.
 *
 * @param stat           Stat modificada (null si es AffinityMark).
 * @param affinity       Afinidad aplicada (null si es StatModifier).
 * @param multiplier     Valor porcentual del modificador (0.10 = +10%).
 * @param remainingTicks Ticks restantes; decrementado cada tick por FoodEffectScheduler.
 * @param sourceFood     ID del alimento origen, para identificar duplicados.
 */
data class ActiveFoodEffect(
    val stat: FoodStat?,
    val affinity: Affinity?,
    val multiplier: Double,
    var remainingTicks: Int,
    val sourceFood: String
)

/**
 * Fuente de verdad de los efectos de comida activos.
 * Vive completamente en memoria — no necesita PDC porque los efectos son
 * temporales y se pierden al reiniciar el servidor (comportamiento aceptable).
 *
 * Reglas:
 * - Máximo MAX_ACTIVE_FOOD_EFFECTS efectos activos por jugador.
 * - Si el jugador ya tiene el efecto del mismo alimento, solo reinicia duración.
 * - Si llega al límite, reemplaza el más antiguo (índice 0 de la deque).
 * - Las AffinityMark se aplican via AffinityMarkManager y no ocupan slot propio.
 */
object FoodEffectManager {

    private val activeEffects = ConcurrentHashMap<UUID, ArrayDeque<ActiveFoodEffect>>()

    // ── Aplicar efectos de un alimento consumido ─────────────────────────────

    /**
     * Aplica todos los efectos de [food] al jugador [uuid].
     * Llama a esto desde FoodListener al consumir.
     * Las AffinityMark se devuelven para que el listener las aplique
     * vía AffinityMarkManager (evita dependencia circular).
     *
     * @return Lista de afinidades a aplicar externamente.
     */
    fun apply(uuid: UUID, food: FoodData): List<Affinity> {
        val deque = activeEffects.getOrPut(uuid) { ArrayDeque() }
        val pendingAffinities = mutableListOf<Affinity>()

        for (effect in food.effects) {
            when (val type = effect.type) {

                is FoodEffectType.StatModifier -> {
                    // ¿Ya hay un efecto del mismo alimento con la misma stat?
                    val existing = deque.firstOrNull {
                        it.sourceFood == food.id && it.stat == type.stat
                    }
                    if (existing != null) {
                        // Solo reiniciar duración
                        existing.remainingTicks = effect.durationTicks
                    } else {
                        // ¿Lleno? Remover el más antiguo (índice 0)
                        if (deque.size >= MAX_ACTIVE_FOOD_EFFECTS) {
                            deque.removeFirst()
                        }
                        deque.addLast(ActiveFoodEffect(
                            stat           = type.stat,
                            affinity       = null,
                            multiplier     = type.multiplier,
                            remainingTicks = effect.durationTicks,
                            sourceFood     = food.id
                        ))
                    }
                }

                is FoodEffectType.AffinityMark -> {
                    // Las marcas de afinidad las maneja AffinityMarkManager;
                    // solo las encolamos para devolverlas al caller.
                    pendingAffinities.add(type.affinity)
                }
            }
        }

        return pendingAffinities
    }

    // ── Tick (llamado por FoodEffectScheduler cada 20 ticks) ─────────────────

    /**
     * Decrementa remainingTicks de todos los efectos activos del jugador.
     * Remueve los que llegaron a 0. Devuelve true si alguno fue removido
     * (señal para recalcular stats).
     */
    fun tick(uuid: UUID, deltaTicks: Int = 20): Boolean {
        val deque = activeEffects[uuid] ?: return false
        val before = deque.size
        deque.forEach { it.remainingTicks -= deltaTicks }
        deque.removeAll { it.remainingTicks <= 0 }
        return deque.size < before
    }

    // ── Consulta para PlayerStats ─────────────────────────────────────────────

    /**
     * Suma de todos los multipliers activos agrupados por stat.
     * PlayerStats.computeEquipmentStats los aplica sobre los valores base.
     *
     * Ejemplo: si hay dos efectos de FUERZA (+10% y +15%) → mapOf(FUERZA to 0.25)
     */
    fun getStatMultipliers(uuid: UUID): Map<FoodStat, Double> {
        val deque = activeEffects[uuid] ?: return emptyMap()
        return deque
            .filter { it.stat != null }
            .groupBy { it.stat!! }
            .mapValues { (_, effects) -> effects.sumOf { it.multiplier } }
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    fun clear(uuid: UUID) {
        activeEffects.remove(uuid)
    }

    /** Devuelve los efectos activos para mostrar en HUD/GUI (lectura). */
    fun getActive(uuid: UUID): List<ActiveFoodEffect> =
        activeEffects[uuid]?.toList() ?: emptyList()
}
