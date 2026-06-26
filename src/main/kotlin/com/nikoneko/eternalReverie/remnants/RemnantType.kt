package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.food.FoodStat
import com.nikoneko.eternalReverie.items.Rarity
import com.nikoneko.eternalReverie.weapons.Affinity

// ── Categoría ─────────────────────────────────────────────────────────────────

enum class RemnantCategory {
    ARMA,
    AFINIDAD,
    ESTADISTICA,
    ECONOMIA,
    UTILIDAD
}

// ── Constante global ───────────────────────────────────────────────────────────

/** Techo de nivel para Vestigios que escalan normalmente. */
const val MAX_VESTIGIO_LEVEL = 5

// ── Efecto de stat (simple, porcentual o absoluto) ────────────────────────────

/**
 * Modifica una stat del jugador en función del nivel equipado.
 *
 * [valuesByLevel] puede tener entre 1 y [RemnantData.maxLevel] entradas:
 *   - Si tiene menos que maxLevel, el último valor se repite para los niveles
 *     superiores (listOf(0.10) = mismo valor en todos los niveles).
 *   - Si tiene exactamente maxLevel entradas, escala normalmente.
 *
 * Convención de valores:
 *   VITALIDAD / RESISTENCIA → valor absoluto (suma al pool, ej. +30 HP)
 *   Resto                   → multiplier porcentual (0.10 = +10%)
 */
data class RemnantEffect(
    val stat: FoodStat,
    val valuesByLevel: List<Double>
) {
    fun valueAt(level: Int, maxLevel: Int): Double {
        val clamped = level.coerceIn(1, maxLevel)
        return valuesByLevel.getOrElse(clamped - 1) { valuesByLevel.last() }
    }
}

// ── Efectos especiales (lógica compleja, no reducible a una stat) ─────────────

/**
 * Efectos que requieren hooks en sistemas externos (CombatResolver, MarkEffects, etc.).
 * Los valores que escalan por nivel usan el mismo patrón de getOrElse que RemnantEffect.
 */
sealed class RemnantSpecialEffect {

    /**
     * Chance de ignorar completamente un hit entrante (daño = 0).
     * Consultado en CombatResolver.resolveHit() antes de aplicar daño.
     * [chanceByLevel]: 0.0–1.0 (ej. 0.10 = 10% de evasión).
     */
    data class AttackEvasion(val chanceByLevel: List<Double>) : RemnantSpecialEffect() {
        fun chanceAt(level: Int, maxLevel: Int) =
            chanceByLevel.getOrElse(level.coerceIn(1, maxLevel) - 1) { chanceByLevel.last() }
    }

    /**
     * Inmunidad total a una marca de afinidad específica.
     * Consultado en MarkEffects / AffinityMarkManager antes de aplicar la marca.
     * No escala por nivel — es binario (equipado = inmune).
     */
    data class AffinityImmunity(val affinity: Affinity) : RemnantSpecialEffect()

    /**
     * Bonus de daño cuando el arma tiene [affinity].
     * Fórmula: bonusDamage = rawDamage × multiplierAt(level) × affinityWeight
     * donde affinityWeight es el peso normalizado (0.0–1.0) de esa afinidad en el arma.
     * Consultado en CombatResolver.resolveHit() tras calcular finalDamage.
     * [multiplierByLevel]: ej. listOf(0.50) = +50% × peso de afinidad, nivel único.
     */
    data class AffinityWeaponDamageBonus(
        val affinity: Affinity,
        val multiplierByLevel: List<Double>
    ) : RemnantSpecialEffect() {
        fun multiplierAt(level: Int, maxLevel: Int) =
            multiplierByLevel.getOrElse(level.coerceIn(1, maxLevel) - 1) { multiplierByLevel.last() }
    }
}

// ── RemnantData ────────────────────────────────────────────────────────────────

/**
 * Definición estática de un tipo de Vestigio.
 *
 * [maxLevel] — techo de nivel de este Vestigio concreto (default: MAX_VESTIGIO_LEVEL).
 *              Vestigios únicos usan maxLevel = 1.
 * [kind]     — ya no es parte del tipo: es una propiedad de la instancia física
 *              del ítem (Eterno/Efímero), asignada al craftear o dar via comando.
 */
data class RemnantData(
    val id: String,
    val name: String,
    val description: String,
    val category: RemnantCategory,
    val rarity: Rarity,
    val maxLevel: Int = MAX_VESTIGIO_LEVEL,
    val effects: List<RemnantEffect> = emptyList(),
    val specialEffects: List<RemnantSpecialEffect> = emptyList()
) {
    init {
        require(maxLevel in 1..MAX_VESTIGIO_LEVEL) {
            "maxLevel debe estar entre 1 y $MAX_VESTIGIO_LEVEL, es $maxLevel"
        }
        require(effects.isNotEmpty() || specialEffects.isNotEmpty()) {
            "Un Vestigio debe tener al menos un efecto"
        }
        effects.forEach { effect ->
            require(effect.valuesByLevel.isNotEmpty()) {
                "valuesByLevel no puede estar vacío en ${id}"
            }
            require(effect.valuesByLevel.size <= maxLevel) {
                "valuesByLevel tiene ${effect.valuesByLevel.size} entradas pero maxLevel es $maxLevel en ${id}"
            }
        }
    }

    fun valueAt(effect: RemnantEffect, level: Int) = effect.valueAt(level, maxLevel)
}

// ── RemnantType ────────────────────────────────────────────────────────────────

enum class RemnantType(val data: RemnantData) {

    // ── Estadística ───────────────────────────────────────────────────────────

    VITALIDAD_MENOR(RemnantData(
        id          = "vitalidad_menor",
        name        = "Vestigio de Vitalidad Menor",
        description = "Aumenta la Vitalidad máxima del portador.",
        category    = RemnantCategory.ESTADISTICA,
        rarity      = Rarity.COMMON,
        // maxLevel = 5 por defecto
        effects     = listOf(
            RemnantEffect(FoodStat.VITALIDAD, listOf(10.0, 20.0, 30.0, 40.0, 50.0))
        )
    )),

    // ── Afinidad ──────────────────────────────────────────────────────────────

    /**
     * Poison Tamer — Vestigio de nivel único (★★★★★).
     *
     * • 10% de chance de ignorar un ataque entrante.
     * • Inmunidad a la marca de Veneno.
     * • +50% de daño con armas de Veneno, escalado por el peso de afinidad del arma.
     *   (ej: arma con 60% Veneno → +30% de daño real)
     */
    POISON_TAMER(RemnantData(
        id             = "poison_tamer",
        name           = "Poison Tamer",
        description    = "El veneno que no te mata te hace más peligroso.",
        category       = RemnantCategory.AFINIDAD,
        rarity         = Rarity.ASCENDED,  // 5★ — ajustar si Rarity tiene otro nombre para el tier más alto
        maxLevel       = 1,
        specialEffects = listOf(
            RemnantSpecialEffect.AttackEvasion(listOf(0.10)),
            RemnantSpecialEffect.AffinityImmunity(Affinity.VENENO),
            RemnantSpecialEffect.AffinityWeaponDamageBonus(Affinity.VENENO, listOf(0.50))
        )
    ));

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.data.id == id }
    }
}
