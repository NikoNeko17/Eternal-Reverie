package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.NamespacedKey

/**
 * Keys propias para Arrow reales (Arco Corto y Arco Largo), ya que Arrow no tiene
 * campos nativos para guardar el daño custom calculado al disparo ni las afinidades
 * del arma usada. El shooter SÍ es nativo (Arrow.getShooter()/setShooter()), no
 * necesita PDC propio.
 */
object RealArrowKeys {

    lateinit var ARROW_DAMAGE: NamespacedKey
    lateinit var ARROW_AFFINITIES: NamespacedKey
    lateinit var ARROW_MAX_DISTANCE: NamespacedKey

    fun init(plugin: EternalReverie) {
        ARROW_DAMAGE = NamespacedKey(plugin, "real_arrow_damage")
        ARROW_AFFINITIES = NamespacedKey(plugin, "real_arrow_affinities")
        ARROW_MAX_DISTANCE = NamespacedKey(plugin, "real_arrow_max_distance")
    }
}
