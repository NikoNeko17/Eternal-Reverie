package com.nikoneko.eternalReverie.crafting

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class CraftingGuiHolder(val player: Player) : InventoryHolder {

    companion object {
        const val SIZE = 54
        val TITLE: Component = Component.text("Mesa de Fabricación", NamedTextColor.DARK_GRAY)

        const val SLOT_BLUEPRINT = 11
        const val SLOT_CATALYST = 43
        const val SLOT_PREVIEW = 13
        const val SLOT_CRAFT_BUTTON = 15

        val MATRIX_SLOTS = listOf(29, 30, 31, 32, 38, 39, 40, 41)

        // Slots puramente decorativos (rellenan el resto, bloqueados por defecto)
        val DECORATIVE_SLOTS: List<Int> by lazy {
            (0 until SIZE).filterNot {
                it == SLOT_BLUEPRINT || it == SLOT_CATALYST ||
                        it == SLOT_PREVIEW || it == SLOT_CRAFT_BUTTON ||
                        it in MATRIX_SLOTS
            }
        }
    }

    private lateinit var inventory: Inventory

    init {
        inventory = Bukkit.createInventory(this, SIZE, TITLE)
        setupDecorativeSlots()
        updateCraftButton()
        updatePreview(null)
    }

    override fun getInventory(): Inventory = inventory

    private fun setupDecorativeSlots() {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = filler.itemMeta
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
        filler.itemMeta = meta

        for (slot in DECORATIVE_SLOTS) {
            inventory.setItem(slot, filler)
        }
    }

    fun getBlueprintItem(): ItemStack? = inventory.getItem(SLOT_BLUEPRINT)
    fun getCatalystItem(): ItemStack? = inventory.getItem(SLOT_CATALYST)

    fun getMaterialItems(): List<ItemStack?> =
        MATRIX_SLOTS.map { inventory.getItem(it) }

    fun findEmptyMatrixSlot(): Int? =
        MATRIX_SLOTS.firstOrNull { inventory.getItem(it) == null }

    fun updateCraftButton() {
        val hasBlueprint = getBlueprintItem() != null
        val hasAnyMaterial = getMaterialItems().any { it != null }

        val canCraft = hasBlueprint && hasAnyMaterial

        val button = ItemStack(
            if (canCraft) Material.LIME_DYE else Material.GRAY_DYE
        )
        val meta = button.itemMeta
        meta.displayName(
            if (canCraft)
                Component.text("Craftear", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
            else
                Component.text("Craftear (falta plano o materiales)", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
        )
        button.itemMeta = meta

        inventory.setItem(SLOT_CRAFT_BUTTON, button)
    }

    fun updatePreview(previewItem: ItemStack?) {
        inventory.setItem(
            SLOT_PREVIEW,
            previewItem ?: run {
                val placeholder = ItemStack(Material.BARRIER)
                val meta = placeholder.itemMeta
                meta.displayName(
                    Component.text("Sin vista previa", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
                placeholder.itemMeta = meta
                placeholder
            }
        )
    }

    fun clearMatrix() {
        for (slot in MATRIX_SLOTS) {
            inventory.setItem(slot, null)
        }
    }

    fun clearAll() {
        clearMatrix()
        inventory.setItem(SLOT_BLUEPRINT, null)
        inventory.setItem(SLOT_CATALYST, null)
    }
}