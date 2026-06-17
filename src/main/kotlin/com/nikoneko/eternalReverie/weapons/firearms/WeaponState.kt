package com.nikoneko.eternalReverie.weapons.firearms

data class WeaponState(
    var lastShot: Long = 0L,
    var ammo: Int = 0,
    var chambered: Boolean = true,
    var isReloading: Boolean = false
)