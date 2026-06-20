package com.nikoneko.eternalReverie.affinities

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.weapons.Affinity
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity

/**
 * Modifica Attribute.MOVEMENT_SPEED vía AttributeModifier independientes por
 * cada Affinity (Hielo, Veneno, Atadura), en vez de mutar baseValue directamente.
 *
 * Esto permite que Hielo + Veneno + Atadura estén activos a la vez sin pisarse:
 * cada uno es un modificador con su propio NamespacedKey, removible individualmente
 * al expirar SU Marca específica sin afectar a los demás ni al baseValue original
 * (que puede ya incluir el bonus/penalización de la stat Movilidad de las Botas).
 *
 * Operation.MULTIPLY_SCALAR_1: amount=-0.40 da baseValue × (1 + (-0.40)) = ×0.60.
 * Varios MULTIPLY_SCALAR_1 activos a la vez se SUMAN entre sí antes de aplicarse
 * una sola vez sobre baseValue (comportamiento nativo de Minecraft), por eso no
 * hace falta guardar ni restaurar un "valor original" manualmente.
 */
object MovementSpeedModifier {

    private lateinit var keysByAffinity: Map<Affinity, NamespacedKey>

    fun init(plugin: EternalReverie) {
        keysByAffinity = mapOf(
            Affinity.HIELO to NamespacedKey(plugin, "movement_mod_hielo"),
            Affinity.VENENO to NamespacedKey(plugin, "movement_mod_veneno"),
            Affinity.ATADURA to NamespacedKey(plugin, "movement_mod_atadura")
        )
    }

    /** @param reductionPct 0.0-1.0, ej. 0.40 = -40% de velocidad de movimiento. */
    fun applyReduction(entity: LivingEntity, affinity: Affinity, reductionPct: Double) {
        val attr = entity.getAttribute(Attribute.MOVEMENT_SPEED) ?: return
        val key = keysByAffinity[affinity] ?: return

        // Remover el modificador anterior de ESTA afinidad si ya existía (reaplicación),
        // así no se acumulan duplicados con el mismo key.
        attr.modifiers.firstOrNull { it.key == key }?.let { attr.removeModifier(it) }

        val modifier = AttributeModifier(
            key,
            -reductionPct,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        )
        attr.addModifier(modifier)
    }

    fun restore(entity: LivingEntity, affinity: Affinity) {
        val attr = entity.getAttribute(Attribute.MOVEMENT_SPEED) ?: return
        val key = keysByAffinity[affinity] ?: return

        attr.modifiers.firstOrNull { it.key == key }?.let { attr.removeModifier(it) }
    }

    /** Limpia los 3 modificadores posibles, por si hace falta un reset total (ej. al morir). */
    fun restoreAll(entity: LivingEntity) {
        for (affinity in keysByAffinity.keys) {
            restore(entity, affinity)
        }
    }
}
