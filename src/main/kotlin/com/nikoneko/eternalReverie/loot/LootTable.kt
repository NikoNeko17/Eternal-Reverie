package com.nikoneko.eternalReverie.loot

import com.nikoneko.eternalReverie.items.Rarity

/**
 * Rango de rareza de loot permitido por cada Rarity de NPC, con pesos relativos
 * dentro de ese rango (mismo-tier = más común, -1 = intermedio, +1 = más raro).
 * Generado en código, no desde un YAML/JSON estático, para que escale
 * automáticamente con cualquier material/blueprint nuevo que se agregue.
 */
object LootRarityRanges {

    data class RarityWeight(val rarity: Rarity, val weight: Int)

    // Rango exacto por tier de NPC, según especificación del diseño.
    private val rangeByNpcRarity: Map<Rarity, List<Rarity>> = mapOf(
        Rarity.COMMON to listOf(Rarity.COMMON, Rarity.RARE),
        Rarity.RARE to listOf(Rarity.COMMON, Rarity.RARE, Rarity.EPIC),
        Rarity.EPIC to listOf(Rarity.COMMON, Rarity.RARE, Rarity.EPIC),
        Rarity.LEGENDARY to listOf(Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY),
        Rarity.MYTHIC to listOf(Rarity.EPIC, Rarity.LEGENDARY, Rarity.MYTHIC),
        Rarity.ONIRIC to listOf(Rarity.LEGENDARY, Rarity.MYTHIC, Rarity.ONIRIC),
        Rarity.ASCENDED to listOf(Rarity.ONIRIC, Rarity.ASCENDED)
    )

    /**
     * Devuelve el rango de rarezas posibles para el loot de un NPC de la rareza dada,
     * con pesos: igual al NPC = 50 (más común), un escalón por debajo = 30 (intermedio),
     * un escalón por encima = 15 (más raro). Si el rango tiene un solo escalón de
     * diferencia hacia un lado (ej. COMMON solo tiene COMMON/RARE), se ajusta sin un
     * tercer peso inexistente.
     */
    fun weightsFor(npcRarity: Rarity): List<RarityWeight> {
        val range = rangeByNpcRarity[npcRarity] ?: listOf(npcRarity)

        return range.map { rarity ->
            val weight = when {
                rarity == npcRarity -> 50
                rarity.stars < npcRarity.stars -> 30
                else -> 15 // rarity.stars > npcRarity.stars
            }
            RarityWeight(rarity, weight)
        }
    }

    fun rollRarity(npcRarity: Rarity): Rarity {
        val weights = weightsFor(npcRarity)
        val total = weights.sumOf { it.weight }
        var roll = (0 until total).random()

        for (w in weights) {
            if (roll < w.weight) return w.rarity
            roll -= w.weight
        }
        return weights.last().rarity
    }
}
