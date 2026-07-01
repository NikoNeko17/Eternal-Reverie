package com.nikoneko.eternalReverie.remnants

import org.bukkit.entity.Player

object RemnantEffects {

    fun bloodAffinityWeaponDamage(player: Player) : Double {
        val finalValue = effectValue(player, RemnantEffect.BLOOD_AFFINITY_WEAPON_DAMAGE)
        return finalValue
    }

    // -- Utilidades para otros ámbitos del juego --

    fun getEffects(player: Player): HashMap<RemnantEffect, Double> {
        val remnants = RemnantSlotManager.getEquipped(player).map { RemnantItemFactory.recompute(it) }
        val effectMap = hashMapOf<RemnantEffect, Double>()
        for (data in remnants) {
            for (effect in data.effects) {
                if (effect.effect in effectMap) {
                    effectMap[effect.effect]?.plus(effect.value)
                } else {
                    effectMap.putIfAbsent(effect.effect, effect.value)
                }
            }
        }
        return effectMap
    }

    fun hasEffect(player: Player, effect: RemnantEffect): Boolean {
        return effect in getEffects(player).keys
    }

    fun effectValue(player: Player, effect: RemnantEffect): Double {
        val effectMap = getEffects(player)
        return if (hasEffect(player, effect)) {
            effectMap[effect] ?: 0.0
        } else 0.0
    }
}