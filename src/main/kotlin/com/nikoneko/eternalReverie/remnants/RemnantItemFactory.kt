package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.food.FoodStat
import com.nikoneko.eternalReverie.items.TextFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

// RemnantKeys se mantiene en este archivo por proximidad (mismo criterio que antes)
object RemnantKeys {
    lateinit var VESTIGIO_ID: NamespacedKey
    lateinit var VESTIGIO_LEVEL: NamespacedKey
    lateinit var VESTIGIO_INSTANCE_UUID: NamespacedKey
    /** true = Eterno (tiene Núcleo), false = Efímero (se destruye al morir). */
    lateinit var VESTIGIO_IS_ETERNAL: NamespacedKey

    fun init(plugin: com.nikoneko.eternalReverie.EternalReverie) {
        VESTIGIO_ID             = NamespacedKey(plugin, "vestigio_id")
        VESTIGIO_LEVEL          = NamespacedKey(plugin, "vestigio_level")
        VESTIGIO_INSTANCE_UUID  = NamespacedKey(plugin, "vestigio_instance_uuid")
        VESTIGIO_IS_ETERNAL     = NamespacedKey(plugin, "vestigio_is_eternal")
    }
}

object RemnantItemFactory {

    private fun toRoman(level: Int): String = when (level) {
        1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
        else -> level.toString()
    }

    /**
     * Crea el ItemStack físico de un Vestigio.
     *
     * @param eternal  false por defecto (Efímero). Los comandos de admin
     *                 pasan true para entregar Vestigios Eternos.
     */
    fun create(type: RemnantType, level: Int = 1, eternal: Boolean = false): ItemStack {
        val data = type.data
        val clampedLevel = level.coerceIn(1, MAX_VESTIGIO_LEVEL)

        val item = ItemStack(Material.PRISMARINE_SHARD)
        val meta = item.itemMeta

        meta.displayName(
            Component.text(data.name, NamedTextColor.WHITE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(buildLore(data, clampedLevel, eternal))

        val pdc = meta.persistentDataContainer
        pdc.set(RemnantKeys.VESTIGIO_ID,            PersistentDataType.STRING,  type.name)
        pdc.set(RemnantKeys.VESTIGIO_LEVEL,         PersistentDataType.INTEGER, clampedLevel)
        pdc.set(RemnantKeys.VESTIGIO_INSTANCE_UUID, PersistentDataType.STRING,  java.util.UUID.randomUUID().toString())
        pdc.set(RemnantKeys.VESTIGIO_IS_ETERNAL,    PersistentDataType.BYTE,    if (eternal) 1.toByte() else 0.toByte())

        item.itemMeta = meta
        return item
    }

    private fun buildLore(data: RemnantData, level: Int, eternal: Boolean): List<Component> {
        val lore = mutableListOf<Component>()

        // Eterno / Efímero
        val kindLabel = if (eternal) "Eterno" else "Efímero"
        val kindColor = if (eternal) NamedTextColor.AQUA else NamedTextColor.LIGHT_PURPLE
        lore += Component.text(kindLabel, kindColor)
            .decoration(TextDecoration.ITALIC, false)

        // Categoría y rareza
        lore += Component.text(TextFormat.capitalizeEnumName(data.category.name), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
        lore += Component.text("★".repeat(data.rarity.stars), NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)

        lore += Component.empty()

        // Descripción
        lore += Component.text(data.description, NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, true)

        lore += Component.empty()

        // Nivel
        lore += Component.text("Nivel ", NamedTextColor.GRAY)
            .append(Component.text(toRoman(level), NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text(" / ${toRoman(MAX_VESTIGIO_LEVEL)}", NamedTextColor.GRAY))
            .decoration(TextDecoration.ITALIC, false)

        // Efectos — genérico, itera data.effects en vez de hardcodear Vitalidad
        for (effect in data.effects) {
            lore += Component.text("${statDisplayName(effect.stat)}: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        formatValue(effect.stat, effect.valueAt(data, level)),
                        NamedTextColor.GREEN,
                        TextDecoration.BOLD
                    )
                )
                .decoration(TextDecoration.ITALIC, false)
        }

        return lore
    }

    // Formatea el valor según si la stat es porcentual o absoluta.
    // Vitalidad y Resistencia son pools absolutos (+10 HP); el resto son multipliers.
    private fun formatValue(stat: FoodStat, value: Double): String = when (stat) {
        FoodStat.VITALIDAD,
        FoodStat.RESISTENCIA -> "+%.0f".format(value)
        else                 -> "+%.0f%%".format(value * 100)
    }

    private fun statDisplayName(stat: FoodStat) = when (stat) {
        FoodStat.FUERZA           -> "Fuerza"
        FoodStat.DEFENSA          -> "Defensa"
        FoodStat.RESISTENCIA      -> "Resistencia"
        FoodStat.PRECISION        -> "Precisión"
        FoodStat.DESTREZA         -> "Destreza"
        FoodStat.VITALIDAD        -> "Vitalidad"
        FoodStat.MOVILIDAD        -> "Movilidad"
        FoodStat.SUERTE           -> "Suerte"
        FoodStat.VELOCIDAD_ATAQUE -> "Vel. Ataque"
    }

    // ── Lectura ────────────────────────────────────────────────────────────────

    fun readType(item: ItemStack?): RemnantType? {
        val meta = item?.itemMeta ?: return null
        val idStr = meta.persistentDataContainer
            .get(RemnantKeys.VESTIGIO_ID, PersistentDataType.STRING) ?: return null
        return runCatching { RemnantType.valueOf(idStr) }.getOrNull()
    }

    fun readLevel(item: ItemStack?): Int? {
        val meta = item?.itemMeta ?: return null
        return meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_LEVEL, PersistentDataType.INTEGER)
    }

    fun readIsEternal(item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer
            .get(RemnantKeys.VESTIGIO_IS_ETERNAL, PersistentDataType.BYTE) == 1.toByte()
    }

    fun isVestigio(item: ItemStack?): Boolean = readType(item) != null
}
