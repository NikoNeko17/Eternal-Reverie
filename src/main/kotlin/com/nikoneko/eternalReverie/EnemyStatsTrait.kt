package com.nikoneko.eternalReverie

import net.citizensnpcs.api.trait.Trait

class EnemyStatsTrait : Trait("enemy_stats") {

    var currentHp = 100.0
    var maxHp = 100.0
    var attack = 8.0
    var attackSpeed = 4.0
    var rarity = "COMMON"
    override fun onSpawn() {}
    override fun onDespawn() {}
}