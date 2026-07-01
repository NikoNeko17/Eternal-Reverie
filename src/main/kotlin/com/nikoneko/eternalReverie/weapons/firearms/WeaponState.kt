package com.nikoneko.eternalReverie.weapons.firearms

data class WeaponState(
    var lastShot: Long = 0L,
    var chambered: Boolean = false,
    var ammo: Int = 0,
    var isReloading: Boolean = false,
    var lastInteractEventAt: Long = 0L
)