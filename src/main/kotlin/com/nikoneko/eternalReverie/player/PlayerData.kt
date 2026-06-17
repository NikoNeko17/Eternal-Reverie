package com.nikoneko.eternalReverie.player

import org.bukkit.inventory.ItemStack

class Player {
    val stats = PlayerStats()
}

data class PlayerStats(
    var vitality: Double = 100.0,
    var defense: Double = 0.0,
    var resistance: Double = 100.0,
    var strength: Double = 0.0,
    var speed: Double = 0.0,
    var heldItem: ItemStack? = null
)