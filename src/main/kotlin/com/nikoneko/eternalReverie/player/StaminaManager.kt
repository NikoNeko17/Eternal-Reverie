package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.affinities.MovementSpeedModifier
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.weapons.Affinity
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType

/**
 * Gasto, regeneración y estado de "Exhausto" de la Stamina.
 *
 * Regeneración: tras EXHAUSTION_DELAY_MILLIS sin gastar, regenera
 * REGEN_PCT_PER_TICK del máximo cada tick (20/seg).
 *
 * Exhausto: al llegar a 0, bloquea ataque/disparo/movimiento (reusando
 * MovementSpeedModifier con la misma "afinidad" que Atadura a nivel de Attribute,
 * pero el chequeo de bloqueo en combate es independiente de AffinityMarkManager)
 * hasta que la Stamina vuelva a subir a EXHAUSTION_RECOVERY_THRESHOLD del máximo.
 */
object StaminaManager {

    private const val EXHAUSTION_DELAY_MILLIS = 3000L
    private const val REGEN_PCT_PER_TICK = 0.0025 // 0.25%/tick = 5%/seg

    private const val ATTACK_BASE_COST_PCT = 0.03 // 3% del máximo, con speedMultiplier=1.0

    private const val SPRINT_ACTIVATE_COST_PCT = 0.05 // 5% al activar
    private const val SPRINT_TICK_COST_PCT = 0.001    // 0.1%/tick mientras corre

    private const val EXHAUSTION_RECOVERY_THRESHOLD_PCT = 0.20 // 20% del máximo

    // ============================================================
    //  GASTO
    // ============================================================

    /** @return true si el gasto se pudo realizar (había stamina suficiente). */
    fun tryConsumeForAttack(entity: LivingEntity, weaponFamily: WeaponFamily?): Boolean {
        val speedMultiplier = weaponFamily?.mobility?.takeIf { it > 0.0 } ?: 1.0
        val costPct = ATTACK_BASE_COST_PCT * (1.0 / speedMultiplier)
        return tryConsume(entity, costPct)
    }

    fun tryConsumeSprintActivation(entity: LivingEntity): Boolean =
        tryConsume(entity, SPRINT_ACTIVATE_COST_PCT)

    /** Llamar 1 vez por tick mientras el jugador esté sprintando. */
    fun tickSprintCost(entity: LivingEntity) {
        consume(entity, SPRINT_TICK_COST_PCT)
    }

    private fun tryConsume(entity: LivingEntity, costPct: Double): Boolean {
        if (isExhausted(entity)) return false

        val max = PlayerStats.getMaxStamina(entity)
        val current = PlayerStats.getCurrentStamina(entity)
        val cost = max * costPct

        if (current < cost) {
            // No alcanza: igual se gasta lo que queda, llevando a 0 (o negativo lógico,
            // clamp a 0) y entrando en Exhausto, en vez de bloquear silenciosamente.
            consume(entity, costPct)
            return false
        }

        consume(entity, costPct)
        return true
    }

    private fun consume(entity: LivingEntity, costPct: Double) {
        val max = PlayerStats.getMaxStamina(entity)
        val current = PlayerStats.getCurrentStamina(entity)
        val cost = max * costPct

        PlayerStats.setCurrentStamina(entity, (current - cost).coerceAtLeast(0.0))
        markLastSpend(entity)

        if (PlayerStats.getCurrentStamina(entity) <= 0.0) {
            enterExhaustion(entity)
        }
    }

    private fun markLastSpend(entity: LivingEntity) {
        entity.persistentDataContainer.set(
            Keys.STAMINA_LAST_SPEND_AT,
            PersistentDataType.LONG,
            System.currentTimeMillis()
        )
    }

    // ============================================================
    //  REGENERACIÓN (llamar 1 vez por tick desde un scheduler)
    // ============================================================

    fun tickRegen(entity: LivingEntity) {
        val pdc = entity.persistentDataContainer
        val lastSpend = pdc.get(Keys.STAMINA_LAST_SPEND_AT, PersistentDataType.LONG) ?: 0L
        val elapsed = System.currentTimeMillis() - lastSpend

        if (elapsed < EXHAUSTION_DELAY_MILLIS) return

        val max = PlayerStats.getMaxStamina(entity)
        val current = PlayerStats.getCurrentStamina(entity)
        if (current >= max) return

        val regenAmount = max * REGEN_PCT_PER_TICK
        PlayerStats.setCurrentStamina(entity, (current + regenAmount).coerceAtMost(max))

        checkExhaustionRecovery(entity)
    }

    // ============================================================
    //  EXHAUSTO
    // ============================================================

    fun isExhausted(entity: LivingEntity): Boolean =
        entity.persistentDataContainer.has(Keys.IS_EXHAUSTED, PersistentDataType.BYTE)

    private fun enterExhaustion(entity: LivingEntity) {
        if (isExhausted(entity)) return // ya estaba, no reaplicar

        entity.persistentDataContainer.set(Keys.IS_EXHAUSTED, PersistentDataType.BYTE, 1)
        MovementSpeedModifier.applyExhaustion(entity)
    }

    private fun checkExhaustionRecovery(entity: LivingEntity) {
        if (!isExhausted(entity)) return

        val max = PlayerStats.getMaxStamina(entity)
        val current = PlayerStats.getCurrentStamina(entity)

        if (current >= max * EXHAUSTION_RECOVERY_THRESHOLD_PCT) {
            entity.persistentDataContainer.remove(Keys.IS_EXHAUSTED)
            MovementSpeedModifier.restoreExhaustion(entity)
        }
    }
}
