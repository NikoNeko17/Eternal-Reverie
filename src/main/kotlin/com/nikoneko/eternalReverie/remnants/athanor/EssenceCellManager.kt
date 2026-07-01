package com.nikoneko.eternalReverie.remnants.athanor

import com.destroystokyo.paper.profile.PlayerProfile
import com.nikoneko.eternalReverie.crafting.MaterialRarity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.profile.PlayerTextures
import java.net.URI
import java.net.URL
import java.util.UUID
import kotlin.random.Random

/**
 * Umbrales de Esencia acumulada requeridos para cruzar a cada rango de
 * rareza. Ajustar libremente segun balance — estos son valores de partida.
 */
object EssenceThresholds {
    val THRESHOLDS: Map<MaterialRarity, Int> = mapOf(
        MaterialRarity.COMMON to 0,
        MaterialRarity.RARE to 100,
        MaterialRarity.EPIC to 300
        // LEGENDARY/MYTHIC/ONIRIC quedan reservados a Celdas de Ruinas con rarityCap mayor;
        // agregar acá sus umbrales cuando ese contenido esté diseñado.
    )

    // Orden ascendente de rareza, usado para saber "el siguiente rango" y
    // para determinar si un rango es el rarityCap de la celda.
    val ORDER: List<MaterialRarity> = listOf(
        MaterialRarity.COMMON,
        MaterialRarity.RARE,
        MaterialRarity.EPIC,
        MaterialRarity.LEGENDARY,
        MaterialRarity.MYTHIC,
        MaterialRarity.ONIRIC
    )
}

object EssenceCellManager {

    /** Crea una Celda de Esencia Vacía nueva con el rarityCap indicado (3★ = EPIC por defecto). */
    fun createEmptyCell(rarityCap: MaterialRarity = MaterialRarity.EPIC): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.displayName(Component.text("Celda de Esencia", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        val pdc = meta.persistentDataContainer

        val playerProfile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID())
        val textures: PlayerTextures = playerProfile.textures

        textures.skin = URL("http://textures.minecraft.net/texture/ccb41186d3f56315ddf368dad4aee09699ba9f7793c89d2a9ce081a847ccf411")
        playerProfile.setTextures(textures)

        pdc.set(EssenceKeys.ESSENCE_STORED, PersistentDataType.INTEGER, 0)
        pdc.set(EssenceKeys.ESSENCE_RARITY_CAP, PersistentDataType.STRING, rarityCap.name)
        pdc.set(EssenceKeys.ESSENCE_IS_SEALED, PersistentDataType.BYTE, 0)
        pdc.set(EssenceKeys.ESSENCE_CLEARED_THRESHOLDS, PersistentDataType.LIST.strings(), emptyList())

        meta.playerProfile = playerProfile
        meta.setMaxStackSize(1)
        item.itemMeta = meta
        return item
    }

    fun isSealed(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.get(EssenceKeys.ESSENCE_IS_SEALED, PersistentDataType.BYTE) == 1.toByte()
    }

    fun getSealedRarity(item: ItemStack): MaterialRarity? {
        val pdc = item.itemMeta?.persistentDataContainer ?: return null
        val raw = pdc.get(EssenceKeys.ESSENCE_SEALED_RARITY, PersistentDataType.STRING) ?: return null
        return runCatching { MaterialRarity.valueOf(raw) }.getOrNull()
    }

    fun getStored(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        return pdc.get(EssenceKeys.ESSENCE_STORED, PersistentDataType.INTEGER) ?: 0
    }

    private fun getRarityCap(item: ItemStack): MaterialRarity {
        val pdc = item.itemMeta?.persistentDataContainer ?: return MaterialRarity.EPIC
        val raw = pdc.get(EssenceKeys.ESSENCE_RARITY_CAP, PersistentDataType.STRING) ?: return MaterialRarity.EPIC
        return runCatching { MaterialRarity.valueOf(raw) }.getOrNull() ?: MaterialRarity.EPIC
    }

    private fun getClearedThresholds(item: ItemStack): Set<MaterialRarity> {
        val pdc = item.itemMeta?.persistentDataContainer ?: return emptySet()
        val raw = pdc.get(EssenceKeys.ESSENCE_CLEARED_THRESHOLDS, PersistentDataType.LIST.strings()) ?: emptyList()
        return raw.mapNotNull { runCatching { MaterialRarity.valueOf(it) }.getOrNull() }.toSet()
    }

    /**
     * Agrega Esencia a la celda (ej. al lootearla en el mundo) y evalúa todos
     * los checkpoints de rareza recién cruzados, en orden. Si algún checkpoint
     * sella la celda, deja de evaluar los siguientes.
     *
     * areaId: área/instancia en la que se obtuvo la Esencia — determina qué
     * AreaEssenceProfile se usa para el chance de sellado.
     *
     * Devuelve la rareza sellada si esta llamada selló la celda, o null si
     * sigue abierta (o ya estaba sellada de antes).
     */
    fun addEssenceAndCheck(item: ItemStack, amount: Int, areaId: String): MaterialRarity? {
        if (isSealed(item)) return null

        val meta = item.itemMeta as SkullMeta
        val pdc = meta.persistentDataContainer

        val currentStored = pdc.get(EssenceKeys.ESSENCE_STORED, PersistentDataType.INTEGER) ?: 0
        val newStored = currentStored + amount
        pdc.set(EssenceKeys.ESSENCE_STORED, PersistentDataType.INTEGER, newStored)

        val cap = getRarityCap(item)
        val cleared = getClearedThresholds(item).toMutableSet()
        val profile = AreaEssenceRegistry.get(areaId)

        val capIndex = EssenceThresholds.ORDER.indexOf(cap)
        var sealedRarity: MaterialRarity? = null

        for (rarity in EssenceThresholds.ORDER) {
            val rarityIndex = EssenceThresholds.ORDER.indexOf(rarity)
            if (rarityIndex > capIndex) break // no evaluamos rangos por encima del cap de esta celda

            val threshold = EssenceThresholds.THRESHOLDS[rarity] ?: continue
            if (newStored < threshold) break // todavía no llegó a este rango
            if (rarity in cleared) continue   // ya se evaluó este umbral antes, no se re-rolea

            cleared.add(rarity)

            val isCapThreshold = rarity == cap
            val sealChance = if (isCapThreshold) {
                1.0
            } else {
                profile?.sealChanceAt(rarity) ?: GlobalEssenceCloseChance.get(rarity)
            }

            if (Random.nextDouble() < sealChance) {
                sealedRarity = rarity
                val playerProfile = Bukkit.createProfile(UUID.randomUUID())
                val textures = playerProfile.textures
                textures.skin = URL("http://textures.minecraft.net/texture/61170964ce0da8fecbe9acc95f491df21277be77fe1a4ef081887d16f63f79dd")
                meta.playerProfile = playerProfile
                break // se selló acá, no seguimos evaluando rangos más altos
            }
        }

        pdc.set(EssenceKeys.ESSENCE_CLEARED_THRESHOLDS, PersistentDataType.LIST.strings(), cleared.map { it.name })

        if (sealedRarity != null) {
            pdc.set(EssenceKeys.ESSENCE_SEALED_RARITY, PersistentDataType.STRING, sealedRarity.name)
            pdc.set(EssenceKeys.ESSENCE_IS_SEALED, PersistentDataType.BYTE, 1)
        }

        meta.displayName(Component.text("Celda de Esencia Sellada", sealedRarity?.color ?: NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))

        item.itemMeta = meta
        return sealedRarity
    }
}
