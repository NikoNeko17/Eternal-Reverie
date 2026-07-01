package com.nikoneko.eternalReverie.remnants.athanor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class AthanorGuiHolder(val player: Player) : InventoryHolder {

    companion object {
        const val SIZE = 54
        val TITLE: Component = Component.text("Athanor", NamedTextColor.DARK_PURPLE)

        // Las 5 puntas de la estrella, en orden de llenado por defecto.
        val MATERIAL_SLOTS = listOf(13, 29, 33, 48, 50)

        const val SLOT_ESSENCE_CELL = 31
        const val SLOT_SYNTHESIZE_BUTTON = 15

        // Slots puramente decorativos (rellenan el resto, bloqueados por defecto)
        val DECORATIVE_SLOTS: List<Int> by lazy {
            (0 until SIZE).filterNot {
                it == SLOT_ESSENCE_CELL || it == SLOT_SYNTHESIZE_BUTTON || it in MATERIAL_SLOTS
            }
        }
    }

    private var inventory: Inventory = Bukkit.createInventory(this, SIZE, TITLE)

    init {
        setupDecorativeSlots()
        updateSynthesizeButton()
    }

    override fun getInventory(): Inventory = inventory

    private fun setupDecorativeSlots() {
        val filler = ItemStack(Material.PURPLE_STAINED_GLASS_PANE)
        val meta = filler.itemMeta
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
        filler.itemMeta = meta

        for (slot in DECORATIVE_SLOTS) {
            inventory.setItem(slot, filler)
        }
    }

    fun getEssenceCellItem(): ItemStack? = inventory.getItem(SLOT_ESSENCE_CELL)

    fun getMaterialItems(): List<ItemStack?> =
        MATERIAL_SLOTS.map { inventory.getItem(it) }

    // Slot vacío siguiente, respetando el orden de llenado por defecto
    // (MATERIAL_SLOTS ya está en ese orden: arriba, izq-media, der-media,
    // abajo-izq, abajo-der).
    fun findEmptyMaterialSlot(): Int? =
        MATERIAL_SLOTS.firstOrNull { inventory.getItem(it) == null }

    fun isReadyToSynthesize(): Boolean {
        val materialsFull = MATERIAL_SLOTS.all { inventory.getItem(it) != null }
        val cell = getEssenceCellItem()
        val cellSealed = cell != null && EssenceCellManager.isSealed(cell)
        return materialsFull && cellSealed
    }

    fun updateSynthesizeButton() {
        val ready = isReadyToSynthesize()
        val cell = getEssenceCellItem()

        val button = ItemStack(if (ready) Material.LIME_DYE else Material.GRAY_DYE)
        val meta = button.itemMeta

        val title = when {
            cell == null -> Component.text(
                "Transmutar (falta Celda de Esencia)", NamedTextColor.GRAY, TextDecoration.BOLD
            )
            !EssenceCellManager.isSealed(cell) -> Component.text(
                "Transmutar (la Celda no está sellada)", NamedTextColor.GRAY, TextDecoration.BOLD
            )
            !MATERIAL_SLOTS.all { inventory.getItem(it) != null } -> Component.text(
                "Transmutar (faltan materiales)", NamedTextColor.GRAY, TextDecoration.BOLD
            )
            else -> Component.text("Transmutar", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
        }
        meta.displayName(title.decoration(TextDecoration.ITALIC, false))

        button.itemMeta = meta
        inventory.setItem(SLOT_SYNTHESIZE_BUTTON, button)
    }

    fun clearMaterials() {
        for (slot in MATERIAL_SLOTS) {
            inventory.setItem(slot, null)
        }
    }

    fun clearAll() {
        clearMaterials()
        inventory.setItem(SLOT_ESSENCE_CELL, null)
    }
}