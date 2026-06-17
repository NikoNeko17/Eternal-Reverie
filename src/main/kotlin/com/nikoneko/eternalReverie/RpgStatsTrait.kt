package com.nikoneko.eternalReverie

import net.citizensnpcs.api.persistence.Persist
import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity

// This annotation defines how Citizens will save this component in its config files
@TraitName("rpg_stats")
class RpgStatsTrait : Trait("rpg_stats") {

    // The @Persist annotation tells Citizens to automatically save these values during server reboots
    @Persist("max_health")
    var maxHealth: Double = 100.0

    @Persist("defense")
    var defense: Int = 10

    // Automatically runs when the NPC spawns into the world
    override fun onSpawn() {
        val livingEntity = npc.entity as? LivingEntity
        livingEntity?.let {
            // Here you can synchronize your RPG attributes with the physical Bukkit entity if needed
        }
    }
}
