package com.nikoneko.eternalReverie.loot

import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.crafting.MaterialRarity
import com.nikoneko.eternalReverie.crafting.MaterialType
import com.nikoneko.eternalReverie.economy.CurrencyItem
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.ItemFactory
import com.nikoneko.eternalReverie.items.Rarity
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Genera el contenido de un cofre según la tabla de loot del Área/Instancia en
 * la que se encuentra, leída desde AreaLootRegistry (areas.json).
 *
 * Los 4 rolls (materials/blueprints/catalysts/currency) son independientes:
 * cada uno tira su propio chance, pudiendo no caer ninguno o todos a la vez.
 */
object ChestLootGenerator {

    fun generate(areaId: String): List<ItemStack> {
        val area = AreaLootRegistry.get(areaId) ?: return emptyList()
        val drops = mutableListOf<ItemStack>()

        rollMaterials(area.materials)?.let { drops.addAll(it) }
        rollBlueprint(area.blueprints)?.let { drops.add(it) }
        rollCatalyst(area.catalysts)?.let { drops.add(it) }
        rollCurrency(area.currency)?.let { drops.add(it) }

        return drops
    }

    private fun rollMaterials(config: MaterialRollConfig): List<ItemStack>? {
        if (Random.nextDouble() > config.chance) return null

        val minRarity = runCatching { MaterialRarity.valueOf(config.minRarity) }.getOrNull() ?: return null
        val maxRarity = runCatching { MaterialRarity.valueOf(config.maxRarity) }.getOrNull() ?: return null

        val pool = MaterialType.entries.filter {
            it.data.rarity.stars in minRarity.stars..maxRarity.stars
        }
        if (pool.isEmpty()) return null

        val amount = Random.nextInt(config.minAmount, config.maxAmount + 1)
        return (1..amount).map { ItemFactory.createMaterialItem(pool.random()) }
    }

    private fun rollBlueprint(config: BlueprintRollConfig): ItemStack? {
        if (Random.nextDouble() > config.chance) return null

        val minRarity = runCatching { Rarity.valueOf(config.minRarity) }.getOrNull() ?: return null
        val maxRarity = runCatching { Rarity.valueOf(config.maxRarity) }.getOrNull() ?: return null

        val candidates = BlueprintRegistry.all().filter {
            it.rarity.stars in minRarity.stars..maxRarity.stars
        }
        if (candidates.isEmpty()) return null

        val blueprint = candidates.random()
        return ItemFactory.createBlueprintItem(blueprint.id)
    }

    private fun rollCatalyst(config: CatalystRollConfig): ItemStack? {
        if (Random.nextDouble() > config.chance) return null

        val catalysts = com.nikoneko.eternalReverie.items.CatalystType.entries
        if (catalysts.isEmpty()) return null

        return ItemFactory.createCatalystItem(catalysts.random())
    }

    private fun rollCurrency(config: CurrencyRollConfig): ItemStack? {
        if (Random.nextDouble() > config.chance) return null

        val amount = Random.nextInt(config.minAmount, config.maxAmount + 1)
        if (amount <= 0) return null

        return CurrencyItem.create(amount)
    }
}
