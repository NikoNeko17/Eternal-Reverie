package com.nikoneko.eternalReverie.loot

import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.items.ArmorPiece
import com.nikoneko.eternalReverie.items.BlueprintData
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.ItemFactory
import com.nikoneko.eternalReverie.items.ItemType
import com.nikoneko.eternalReverie.items.Rarity
import com.nikoneko.eternalReverie.crafting.MaterialType
import com.nikoneko.eternalReverie.crafting.MaterialRarity
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.random.Random

/**
 * Genera loot aleatorio (materiales sueltos o ítems ya crafteados) sin pasar por
 * la mesa de fabricación, usado por NPCs al morir y cofres en el mundo.
 *
 * Las tablas NO son YAML/JSON estático: se generan en tiempo de ejecución a partir
 * de MaterialType.entries y BlueprintRegistry.all(), filtrando por rareza. Así
 * cualquier material/blueprint nuevo agregado al enum/YAML entra automáticamente
 * sin tocar este archivo.
 */
object LootGenerator {

    // Pesos del resultado general de un drop: nada / materiales sueltos / ítem crafteado.
    // Valores de partida, ajustables.
    private const val WEIGHT_NOTHING = 50
    private const val WEIGHT_MATERIALS = 35
    private const val WEIGHT_CRAFTED_ITEM = 15

    private const val MIN_MATERIALS_DROP = 1
    private const val MAX_MATERIALS_DROP = 3

    sealed class LootResult {
        data object Nothing : LootResult()
        data class Materials(val items: List<ItemStack>) : LootResult()
        data class CraftedItem(val item: ItemStack) : LootResult()
    }

    // Chatarra: drop garantizado en CADA muerte, independiente del resultado de
    // rollLoot() (nada/materiales/ítem). Rango: 1-3 unidades POR estrella de rareza
    // del NPC (ej. ★ = 1-3, ★★★★ = 4-12, ★★★★★★★ = 7-21).
    fun rollCurrency(npcRarity: Rarity): Int {
        val perStar = Random.nextInt(1, 4) // 1-3
        return perStar * npcRarity.stars
    }

    fun rollLoot(npcRarity: Rarity): LootResult {
        val totalWeight = WEIGHT_NOTHING + WEIGHT_MATERIALS + WEIGHT_CRAFTED_ITEM
        val roll = Random.nextInt(totalWeight)

        return when {
            roll < WEIGHT_NOTHING -> LootResult.Nothing
            roll < WEIGHT_NOTHING + WEIGHT_MATERIALS -> rollMaterials(npcRarity)
            else -> rollCraftedItem(npcRarity) ?: rollMaterials(npcRarity) // fallback si no hay blueprints disponibles
        }
    }

    // ============================================================
    //  MATERIALES SUELTOS
    // ============================================================

    private fun rollMaterials(npcRarity: Rarity): LootResult.Materials {
        val count = Random.nextInt(MIN_MATERIALS_DROP, MAX_MATERIALS_DROP + 1)
        val items = (1..count).mapNotNull { rollSingleMaterial(npcRarity) }
        return LootResult.Materials(items)
    }

    private fun rollSingleMaterial(npcRarity: Rarity): ItemStack? {
        val targetRarity = LootRarityRanges.rollRarity(npcRarity)
        val materialRarity = mapToMaterialRarity(targetRarity) ?: return null

        val pool = MaterialType.entries.filter { it.data.rarity == materialRarity }
        if (pool.isEmpty()) return null

        val chosen = pool.random()
        return ItemFactory.createMaterialItem(chosen)
    }

    // MaterialRarity solo llega hasta ONIRIC (6 niveles); ASCENDED (7★, vía catalizador)
    // no tiene materiales propios, así que cualquier roll en ASCENDED cae a ONIRIC.
    private fun mapToMaterialRarity(rarity: Rarity): MaterialRarity? {
        return when (rarity) {
            Rarity.COMMON -> MaterialRarity.COMMON
            Rarity.RARE -> MaterialRarity.RARE
            Rarity.EPIC -> MaterialRarity.EPIC
            Rarity.LEGENDARY -> MaterialRarity.LEGENDARY
            Rarity.MYTHIC -> MaterialRarity.MYTHIC
            Rarity.ONIRIC, Rarity.ASCENDED -> MaterialRarity.ONIRIC
        }
    }

    // ============================================================
    //  ÍTEM CRAFTEADO (arma o armadura completa, con materiales random)
    // ============================================================

    private fun rollCraftedItem(npcRarity: Rarity): LootResult.CraftedItem? {
        val targetRarity = LootRarityRanges.rollRarity(npcRarity)

        val candidates = BlueprintRegistry.all().filter { it.rarity == targetRarity }
        if (candidates.isEmpty()) return null

        val blueprint = candidates.random()
        val materials = rollRandomMaterialsForCraft(blueprint)

        val item: ItemStack = when (blueprint.itemType) {
            ItemType.WEAPON -> CraftingCalculator.buildFinalWeapon(
                blueprint = blueprint,
                materials = materials,
                catalystType = null,
                instanceUuid = UUID.randomUUID()
            )
            ItemType.ARMOR -> CraftingCalculator.buildFinalArmor(
                blueprint = blueprint,
                materials = materials,
                catalystType = null,
                instanceUuid = UUID.randomUUID()
            )
        }

        return LootResult.CraftedItem(item)
    }

    // Genera entre 2 y 6 materiales aleatorios de rareza igual o menor al blueprint,
    // simulando un crafteo "encontrado ya hecho" con calidad variable, nunca el máximo
    // teórico de 8 slots perfectos (eso queda reservado para crafteo manual del jugador).
    private fun rollRandomMaterialsForCraft(blueprint: BlueprintData): List<MaterialType> {
        val maxMaterialRarity = mapToMaterialRarity(blueprint.rarity) ?: return emptyList()
        val allowedRarities = MaterialRarity.entries
            .filter { it.stars <= maxMaterialRarity.stars }

        val pool = MaterialType.entries.filter { it.data.rarity in allowedRarities }
        if (pool.isEmpty()) return emptyList()

        val slotCount = Random.nextInt(2, 7)
        return (1..slotCount).map { pool.random() }
    }
}
