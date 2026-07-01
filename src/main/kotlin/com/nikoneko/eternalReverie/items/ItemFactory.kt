package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.crafting.MaterialType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ItemFactory {

    fun createMaterialItem(type: MaterialType, amount: Int = 1): ItemStack {
        val item = ItemStack(Material.IRON_NUGGET, amount.coerceIn(1, 99)) // placeholder visual, ajustar con resource pack/CMD
        val meta = item.itemMeta

        meta.displayName(
            Component.text(type.data.name)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" ${"★".repeat(type.data.rarity.stars)}", NamedTextColor.GRAY))
        )
        meta.persistentDataContainer.set(
            Keys.MATERIAL_ID,
            PersistentDataType.STRING,
            type.name
        )

        meta.setMaxStackSize(99)

        item.itemMeta = meta
        return item
    }

    // Ahora recibe el id (String) del blueprint cargado vía BlueprintRegistry,
    // ya que dejó de ser un enum para poder editarse desde blueprints.yml sin recompilar.
    fun createBlueprintItem(blueprintId: String): ItemStack? {
        val data = BlueprintRegistry.get(blueprintId) ?: return null

        val item = ItemStack(data.material)
        val meta = item.itemMeta

        val subtitle = when (data.itemType) {
            ItemType.WEAPON -> data.family!!.displayName
            ItemType.ARMOR -> TextFormat.capitalizeEnumName(data.armorPiece!!.name)
        }

        meta.displayName(
            Component.text("Plano: $subtitle", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" (${"★".repeat(data.rarity.stars)})", NamedTextColor.GRAY))
        )
        meta.persistentDataContainer.set(
            Keys.BLUEPRINT_ID,
            PersistentDataType.STRING,
            data.id
        )

        meta.setMaxStackSize(1)

        item.itemMeta = meta
        return item
    }

    fun createCatalystItem(type: CatalystType, amount: Int = 1): ItemStack {
        val item = ItemStack(Material.NETHER_STAR, amount.coerceIn(1, 64)) // placeholder visual
        val meta = item.itemMeta

        meta.displayName(
            Component.text(type.data.name, NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.persistentDataContainer.set(
            Keys.CATALYST_ID,
            PersistentDataType.STRING,
            type.name
        )

        meta.setMaxStackSize(1)

        item.itemMeta = meta
        return item
    }

    // --- Lectura inversa: ItemStack -> Type/Data ---

    fun readMaterialType(item: ItemStack?): MaterialType? {
        val meta = item?.itemMeta ?: return null
        val idStr = meta.persistentDataContainer.get(Keys.MATERIAL_ID, PersistentDataType.STRING)
            ?: return null
        return try {
            MaterialType.valueOf(idStr)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // Devuelve el BlueprintData leído desde el registro cargado por YAML,
    // usando el ID (String) guardado en el PDC del ítem.
    fun readBlueprintData(item: ItemStack?): BlueprintData? {
        val meta = item?.itemMeta ?: return null
        val idStr = meta.persistentDataContainer.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
            ?: return null
        return BlueprintRegistry.get(idStr)
    }

    fun readCatalystType(item: ItemStack?): CatalystType? {
        val meta = item?.itemMeta ?: return null
        val idStr = meta.persistentDataContainer.get(Keys.CATALYST_ID, PersistentDataType.STRING)
            ?: return null
        return try {
            CatalystType.valueOf(idStr)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
