package com.nikoneko.eternalReverie.remnants

import com.nikoneko.eternalReverie.crafting.MaterialRarity
import com.nikoneko.eternalReverie.items.TextFormat
import com.nikoneko.eternalReverie.remnants.athanor.RemnantSynthesizer
import com.nikoneko.eternalReverie.remnants.athanor.SynthesizedRemnant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.EnumMap

// RemnantKeys se mantiene en este archivo por proximidad (mismo criterio que antes)
object RemnantKeys {
    lateinit var VESTIGIO_LEVEL: NamespacedKey
    lateinit var VESTIGIO_INSTANCE_UUID: NamespacedKey
    /** true = Eterno (tiene Núcleo), false = Efímero (se destruye al morir). */
    lateinit var VESTIGIO_IS_ETERNAL: NamespacedKey
    lateinit var VESTIGIO_SYNTHESIS_MATERIALS: NamespacedKey
    /** Rareza sellada de la Celda usada en la síntesis original — necesaria
     *  para poder recomputar el Vestigio (ver RemnantSynthesizer.recompute),
     *  ya que synthesize() requiere ambos: materiales Y rareza de la Celda. */
    lateinit var VESTIGIO_SEALED_CELL_RARITY: NamespacedKey

    fun init(plugin: com.nikoneko.eternalReverie.EternalReverie) {
        VESTIGIO_LEVEL          = NamespacedKey(plugin, "vestigio_level")
        VESTIGIO_INSTANCE_UUID  = NamespacedKey(plugin, "vestigio_instance_uuid")
        VESTIGIO_IS_ETERNAL     = NamespacedKey(plugin, "vestigio_is_eternal")
        VESTIGIO_SYNTHESIS_MATERIALS = NamespacedKey(plugin, "vestigio_synthesis_materials")
        VESTIGIO_SEALED_CELL_RARITY  = NamespacedKey(plugin, "vestigio_sealed_cell_rarity")
    }
}

object RemnantItemFactory {

    /**
     * Crea el ItemStack físico de un Vestigio.
     *
     * @param sealedCellRarity la rareza que tenía sellada la Celda usada en
     *   esta síntesis — se persiste junto a los materiales para que el
     *   Vestigio sea recomputable más adelante (rebalanceo de RemnantEffect).
     * @param eternal  false por defecto (Efímero). Los comandos de admin
     *                 pasan true para entregar Vestigios Eternos.
     */
    fun create(remnant: SynthesizedRemnant, sealedCellRarity: MaterialRarity, eternal: Boolean = false): ItemStack {
        val item = ItemStack(Material.PRISMARINE_SHARD)
        val meta = item.itemMeta

        meta.displayName(
            Component.text(
                remnant.primaryEffect?.displayName ?: "Vestigio Neutro",
                if (remnant.isNeutral) NamedTextColor.GRAY else remnant.rarity.color,
            ).decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(buildLore(remnant, eternal))

        val pdc = meta.persistentDataContainer
        pdc.set(RemnantKeys.VESTIGIO_LEVEL, PersistentDataType.INTEGER, remnant.level)
        pdc.set(RemnantKeys.VESTIGIO_INSTANCE_UUID, PersistentDataType.STRING, java.util.UUID.randomUUID().toString())
        pdc.set(RemnantKeys.VESTIGIO_IS_ETERNAL, PersistentDataType.BYTE, if (eternal) 1.toByte() else 0.toByte())
        pdc.set(
            RemnantKeys.VESTIGIO_SYNTHESIS_MATERIALS,
            PersistentDataType.LIST.strings(),
            remnant.materials.map { it.name }
        )
        pdc.set(RemnantKeys.VESTIGIO_SEALED_CELL_RARITY, PersistentDataType.STRING, sealedCellRarity.name)

        meta.setMaxStackSize(1)
        item.itemMeta = meta
        return item
    }

    private fun buildLore(remnant: SynthesizedRemnant, eternal: Boolean): List<Component> {
        val lore = mutableListOf<Component>()

        val kindLabel = if (eternal) "Eterno" else "Efímero"
        val kindColor = if (eternal) NamedTextColor.AQUA else NamedTextColor.LIGHT_PURPLE
        lore += Component.text(kindLabel, kindColor)
            .decoration(TextDecoration.ITALIC, false)

        lore += Component.text(
            "${TextFormat.capitalizeEnumName(remnant.primaryEffect?.type?.name ?: "Nulo")} - ${remnant.primaryEffect?.subtype ?: "Vacío"}",
            NamedTextColor.GRAY
        ).decoration(TextDecoration.ITALIC, false)

        lore += Component.text("★".repeat(remnant.rarity.stars), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

        lore += Component.empty()

        lore += Component.text("Nivel ${remnant.level}", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

        lore += Component.text("Efectos:", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)

        if (remnant.effects.isEmpty()) {
            lore += Component.text("  (ninguno)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
        }

        for (effect in remnant.effects) {
            lore += Component.text("+ ", NamedTextColor.GREEN)
                .append(
                    Component.text(effect.renderLore(), NamedTextColor.GREEN)
                )
                .decoration(TextDecoration.ITALIC, false)
        }

        return lore
    }

    // ── Lectura ────────────────────────────────────────────────────────────────

    fun readLevel(item: ItemStack?): Int? {
        val meta = item?.itemMeta ?: return null
        return meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_LEVEL, PersistentDataType.INTEGER)
    }

    fun readIsEternal(item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer
            .get(RemnantKeys.VESTIGIO_IS_ETERNAL, PersistentDataType.BYTE) == 1.toByte()
    }

    fun readInstanceUuid(item: ItemStack?): java.util.UUID? {
        val meta = item?.itemMeta ?: return null
        val raw = meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_INSTANCE_UUID, PersistentDataType.STRING) ?: return null
        return runCatching { java.util.UUID.fromString(raw) }.getOrNull()
    }

    fun readSealedCellRarity(item: ItemStack?): MaterialRarity? {
        val meta = item?.itemMeta ?: return null
        val raw = meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_SEALED_CELL_RARITY, PersistentDataType.STRING) ?: return null
        return runCatching { MaterialRarity.valueOf(raw) }.getOrNull()
    }

    fun readSynthesisMaterialIds(item: ItemStack?): List<String>? {
        val meta = item?.itemMeta ?: return null
        return meta.persistentDataContainer.get(RemnantKeys.VESTIGIO_SYNTHESIS_MATERIALS, PersistentDataType.LIST.strings())
    }

    /**
     * Un ItemStack "es" un Vestigio si tiene VESTIGIO_INSTANCE_UUID en su PDC
     * — ya no hay un RemnantType fijo contra el cual comparar, así que la
     * identidad de "esto es un Vestigio" pasa a ser estructural, no por tipo.
     */
    fun isVestigio(item: ItemStack?): Boolean = readInstanceUuid(item) != null

    /**
     * Recalcula el SynthesizedRemnant completo de un Vestigio existente,
     * leyendo materiales + rareza de Celda sellada desde su propio PDC.
     * Punto de entrada para forzar un rebalanceo (ver "El Sueño aprende").
     */
    fun recompute(item: ItemStack?): SynthesizedRemnant {
        val materialIds = readSynthesisMaterialIds(item) ?: return RemnantSynthesizer.DEFAULT_REMNANT
        val sealedRarity = readSealedCellRarity(item) ?: return RemnantSynthesizer.DEFAULT_REMNANT
        return RemnantSynthesizer.recompute(materialIds, sealedRarity)
    }
}