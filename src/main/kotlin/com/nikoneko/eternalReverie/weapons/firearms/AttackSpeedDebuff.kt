package com.nikoneko.eternalReverie.weapons.firearms

import com.nikoneko.eternalReverie.items.Keys
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType

/**
 * Debuff temporal de velocidad de ataque (ej. aplicado por la Habilidad de Congelación
 * de la afinidad Hielo). Se guarda como un multiplicador (0.0-1.0, donde 1.0 = sin
 * debuff) + un timestamp de expiración en el PDC de la entidad afectada.
 * WeaponStateManager.getCooldownMillis() lo consulta para alargar el cooldown real.
 */
object AttackSpeedDebuff {

    fun apply(entity: LivingEntity, multiplier: Double, durationMillis: Long) {
        val pdc = entity.persistentDataContainer
        pdc.set(Keys.ATTACK_SPEED_DEBUFF_MULTIPLIER, PersistentDataType.DOUBLE, multiplier.coerceIn(0.05, 1.0))
        pdc.set(
            Keys.ATTACK_SPEED_DEBUFF_EXPIRES_AT,
            PersistentDataType.LONG,
            System.currentTimeMillis() + durationMillis
        )
    }

    /** Devuelve 1.0 (sin penalización) si no hay debuff activo o ya expiró. */
    fun getActiveMultiplier(entity: LivingEntity): Double {
        val pdc = entity.persistentDataContainer
        val expiresAt = pdc.get(Keys.ATTACK_SPEED_DEBUFF_EXPIRES_AT, PersistentDataType.LONG) ?: return 1.0

        if (System.currentTimeMillis() >= expiresAt) {
            pdc.remove(Keys.ATTACK_SPEED_DEBUFF_MULTIPLIER)
            pdc.remove(Keys.ATTACK_SPEED_DEBUFF_EXPIRES_AT)
            return 1.0
        }

        return pdc.get(Keys.ATTACK_SPEED_DEBUFF_MULTIPLIER, PersistentDataType.DOUBLE) ?: 1.0
    }
}
