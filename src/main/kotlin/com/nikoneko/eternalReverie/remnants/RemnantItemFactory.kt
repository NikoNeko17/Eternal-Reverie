package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.items.TextFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object RemnantKeys {
    lateinit var VESTIGIO_ID: NamespacedKey
    lateinit var VESTIGIO_LEVEL: NamespacedKey
    lateinit var VESTIGIO_INSTANCE_UUID: NamespacedKey

    fun init(plugin: com.nikoneko.eternalReverie.EternalReverie) {
        VESTIGIO_ID = NamespacedKey(plugin, "vestigio_id")
        VESTIGIO_LEVEL = NamespacedKey(plugin, "vestigio_level")
        VESTIGIO_INSTANCE_UUID = NamespacedKey(plugin, "vestigio_instance_uuid")
    }
}

object RemnantItemFactory {

    private fun kindColor(kind: RemnantKind): NamedTextColor =
        if (kind == RemnantKind.ETERNO) NamedTextColor.AQUA else NamedTextColor.LIGHT_PURPLE

    private fun toRoman(level: Int): String = when (level) {
        1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
        else -> level.toString()
    }

    fun create(type: RemnantType, level: Int = 1): ItemStack {
        val data = type.data
        val clampedLevel = level.coerceIn(1, MAX_VESTIGIO_LEVEL)

        val item = ItemStack(Material.PRISMARINE_SHARD) // placeholder visual
        val meta = item.itemMeta

        meta.displayName(
            Component.text(data.name, NamedTextColor.WHITE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        val lore = mutableListOf(
            Component.text(TextFormat.capitalizeEnumName(data.kind.name), kindColor(data.kind))
                .decoration(TextDecoration.ITALIC, false),
            Component.text(TextFormat.capitalizeEnumName(data.category.name), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("★".repeat(data.rarity.stars), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(""),
            Component.text(data.description, NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, true),
            Component.text("")
        )

        // El nivel actual y su valor van resaltados (color distinto) respecto
        // a los demás niveles, tal como pediste: "se marca la misma [línea] y
        // ese número se cambia".
        lore.add(
            Component.text("Nivel ", NamedTextColor.GRAY)
                .append(Component.text(toRoman(clampedLevel), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" / ${toRoman(MAX_VESTIGIO_LEVEL)}", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false)
        )
        lore.add(
            Component.text("Vitalidad: ", NamedTextColor.GRAY)
                .append(Component.text("+%.0f".format(data.valueAt(clampedLevel)), NamedTextColor.GREEN, TextDecoration.BOLD))
                .decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(lore)

        val pdc = meta.persistentDataContainer
        pdc.set(RemnantKeys.VESTIGIO_ID, PersistentDataType.STRING, type.name)
        pdc.set(RemnantKeys.VESTIGIO_LEVEL, PersistentDataType.INTEGER, clampedLevel)
        pdc.set(RemnantKeys.VESTIGIO_INSTANCE_UUID, PersistentDataType.STRING, java.util.UUID.randomUUID().toString())

        item.itemMeta = meta
        return item
    }

    fun readType(item: ItemStack?): RemnantType? {
        val meta = item?.itemMeta ?: return null
        val idStr = meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_ID, PersistentDataType.STRING)
            ?: return null
        return runCatching { RemnantType.valueOf(idStr) }.getOrNull()
    }

    fun readLevel(item: ItemStack?): Int? {
        val meta = item?.itemMeta ?: return null
        return meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_LEVEL, PersistentDataType.INTEGER)
    }

    fun isVestigio(item: ItemStack?): Boolean = readType(item) != null
}
