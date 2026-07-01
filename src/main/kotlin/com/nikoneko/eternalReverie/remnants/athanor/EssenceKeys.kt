package com.nikoneko.eternalReverie.remnants.athanor

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.NamespacedKey

object EssenceKeys {

    lateinit var ESSENCE_STORED: NamespacedKey       // Double — cantidad de Esencia acumulada
    lateinit var ESSENCE_RARITY_CAP: NamespacedKey    // String (MaterialRarity.name) — techo de la celda
    lateinit var ESSENCE_SEALED_RARITY: NamespacedKey // String (MaterialRarity.name) — rareza sellada, si ya se selló
    lateinit var ESSENCE_IS_SEALED: NamespacedKey     // Byte 0/1
    lateinit var ESSENCE_CLEARED_THRESHOLDS: NamespacedKey // List<String> — umbrales de rareza ya chequeados (evita re-roll)

    fun init(plugin: EternalReverie) {
        ESSENCE_STORED = NamespacedKey(plugin, "essence_stored")
        ESSENCE_RARITY_CAP = NamespacedKey(plugin, "essence_rarity_cap")
        ESSENCE_SEALED_RARITY = NamespacedKey(plugin, "essence_sealed_rarity")
        ESSENCE_IS_SEALED = NamespacedKey(plugin, "essence_is_sealed")
        ESSENCE_CLEARED_THRESHOLDS = NamespacedKey(plugin, "essence_cleared_thresholds")
    }
}