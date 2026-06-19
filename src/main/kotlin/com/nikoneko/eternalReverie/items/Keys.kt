package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.NamespacedKey

object Keys {

    lateinit var INSTANCE_UUID: NamespacedKey
    lateinit var DURABILITY: NamespacedKey
    lateinit var MAX_DURABILITY: NamespacedKey
    lateinit var WEAPON_FAMILY: NamespacedKey
    lateinit var BLUEPRINT_ID: NamespacedKey
    lateinit var MATERIALS: NamespacedKey
    lateinit var CATALYST_ID: NamespacedKey
    lateinit var ATTACK_SPEED: NamespacedKey
    lateinit var MOBILITY: NamespacedKey
    lateinit var ARMOR_MOBILITY: NamespacedKey
    lateinit var REACH: NamespacedKey

    // Identifica qué MaterialType representa un ItemStack de material en bruto
    lateinit var MATERIAL_ID: NamespacedKey
    
    lateinit var MAX_HP: NamespacedKey
    lateinit var CURRENT_HP: NamespacedKey
    lateinit var MAX_STAMINA: NamespacedKey
    lateinit var CURRENT_STAMINA: NamespacedKey

    fun init(plugin: EternalReverie) {

        INSTANCE_UUID = NamespacedKey(plugin, "instance_uuid")
        DURABILITY = NamespacedKey(plugin, "durability")
        MAX_DURABILITY = NamespacedKey(plugin, "max_durability")
        WEAPON_FAMILY = NamespacedKey(plugin, "weapon_type")
        BLUEPRINT_ID = NamespacedKey(plugin, "blueprint_id")
        MATERIALS = NamespacedKey(plugin, "materials")
        CATALYST_ID = NamespacedKey(plugin, "catalyst_id")
        MATERIAL_ID = NamespacedKey(plugin, "material_id")
        ATTACK_SPEED = NamespacedKey(plugin, "attack_speed")
        MOBILITY = NamespacedKey(plugin, "mobility")
        ARMOR_MOBILITY = NamespacedKey(plugin, "armor_mobility")
        REACH = NamespacedKey(plugin, "reach")
        MAX_HP = NamespacedKey(plugin, "max_hp")
        CURRENT_HP = NamespacedKey(plugin, "current_hp")
        MAX_STAMINA = NamespacedKey(plugin, "max_stamina")
        CURRENT_STAMINA = NamespacedKey(plugin, "current_stamina")
    }
}