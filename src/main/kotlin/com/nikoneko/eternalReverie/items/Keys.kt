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

    // Identifica qué MaterialType representa un ItemStack de material en bruto
    lateinit var MATERIAL_ID: NamespacedKey

    // Stats del jugador/NPC (guardadas en PDC de la entidad)
    lateinit var MAX_HP: NamespacedKey
    lateinit var CURRENT_HP: NamespacedKey
    lateinit var MAX_STAMINA: NamespacedKey
    lateinit var CURRENT_STAMINA: NamespacedKey
    lateinit var ATTACK_SPEED: NamespacedKey
    lateinit var REACH: NamespacedKey
    lateinit var MOBILITY: NamespacedKey
    lateinit var ARMOR_MOBILITY: NamespacedKey

    // Debuff temporal de velocidad de ataque (Habilidad de Congelación, Affinity HIELO)
    lateinit var ATTACK_SPEED_DEBUFF_MULTIPLIER: NamespacedKey
    lateinit var ATTACK_SPEED_DEBUFF_EXPIRES_AT: NamespacedKey

    // Valor original de Attribute.MOVEMENT_SPEED antes de aplicar debuff de Hielo/Veneno
    lateinit var MOVEMENT_SPEED_ORIGINAL: NamespacedKey

    // StaminaManager: timestamp del último gasto (para el delay de regen) y flag de Exhausto
    lateinit var STAMINA_LAST_SPEND_AT: NamespacedKey
    lateinit var IS_EXHAUSTED: NamespacedKey

    // Economía: balance de Chatarra del jugador, y flag del ítem físico de Chatarra
    lateinit var CURRENCY_BALANCE: NamespacedKey
    lateinit var IS_CURRENCY: NamespacedKey

    // Vestigios: espacios desbloqueados y lista de equipados ("TIPO:nivel" por entrada)
    lateinit var VESTIGIO_UNLOCKED_SLOTS: NamespacedKey
    lateinit var VESTIGIO_EQUIPPED: NamespacedKey

    fun init(plugin: EternalReverie) {

        INSTANCE_UUID = NamespacedKey(plugin, "instance_uuid")
        DURABILITY = NamespacedKey(plugin, "durability")
        MAX_DURABILITY = NamespacedKey(plugin, "max_durability")
        WEAPON_FAMILY = NamespacedKey(plugin, "weapon_type")
        BLUEPRINT_ID = NamespacedKey(plugin, "blueprint_id")
        MATERIALS = NamespacedKey(plugin, "materials")
        CATALYST_ID = NamespacedKey(plugin, "catalyst_id")
        MATERIAL_ID = NamespacedKey(plugin, "material_id")

        MAX_HP = NamespacedKey(plugin, "max_hp")
        CURRENT_HP = NamespacedKey(plugin, "current_hp")
        MAX_STAMINA = NamespacedKey(plugin, "max_stamina")
        CURRENT_STAMINA = NamespacedKey(plugin, "current_stamina")

        ATTACK_SPEED_DEBUFF_MULTIPLIER = NamespacedKey(plugin, "attack_speed_debuff_multiplier")
        ATTACK_SPEED_DEBUFF_EXPIRES_AT = NamespacedKey(plugin, "attack_speed_debuff_expires_at")

        MOVEMENT_SPEED_ORIGINAL = NamespacedKey(plugin, "movement_speed_original")

        STAMINA_LAST_SPEND_AT = NamespacedKey(plugin, "stamina_last_spend_at")
        IS_EXHAUSTED = NamespacedKey(plugin, "is_exhausted")

        CURRENCY_BALANCE = NamespacedKey(plugin, "currency_balance")
        IS_CURRENCY = NamespacedKey(plugin, "is_currency")

        VESTIGIO_UNLOCKED_SLOTS = NamespacedKey(plugin, "vestigio_unlocked_slots")
        VESTIGIO_EQUIPPED = NamespacedKey(plugin, "vestigio_equipped")
    }
}
