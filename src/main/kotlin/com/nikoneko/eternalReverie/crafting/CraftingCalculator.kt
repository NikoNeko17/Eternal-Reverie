@file:Suppress("UnstableApiUsage")

package com.nikoneko.eternalReverie.crafting

import com.nikoneko.eternalReverie.items.ArmorPiece
import com.nikoneko.eternalReverie.items.BlueprintData
import com.nikoneko.eternalReverie.items.CatalystType
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.items.Rarity
import com.nikoneko.eternalReverie.items.TextFormat
import com.nikoneko.eternalReverie.items.WeaponNameBank
import com.nikoneko.eternalReverie.weapons.Affinity
import com.nikoneko.eternalReverie.weapons.WeaponClass
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object CraftingCalculator {

    // ============================================================
    //  ARMAS
    // ============================================================

    data class ComputedWeaponStats(
        val damage: Double,
        val attackSpeed: Double,
        val mobility: Double,
        val affinities: List<Pair<Affinity, Double>>
    )

    fun computeWeaponStatsPublic(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ComputedWeaponStats = computeWeapon(blueprint, materials)

    private fun computeWeapon(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ComputedWeaponStats {
        val family = requireNotNull(blueprint.family) {
            "computeWeapon llamado con un blueprint que no es de arma (${blueprint.id})"
        }
        val baseDamage = requireNotNull(blueprint.baseDamage)

        val totalDamageBonus = materials.sumOf { it.data.weaponDamageBonus }
        val totalSpeedBonus = materials.sumOf { it.data.weaponAttackSpeedBonus }
        val totalMobilityBonus = materials.sumOf { it.data.weaponMobilityBonus }

        val damage = baseDamage * (1 + totalDamageBonus) * family.damageMultiplier
        val attackSpeed = (1 + totalSpeedBonus) * family.attackSpeed
        val mobility = (1 + totalMobilityBonus) * family.mobility

        val affinities = computeAffinities(materials, weaponAffinityLimit(blueprint.rarity))

        return ComputedWeaponStats(damage, attackSpeed, mobility, affinities)
    }

    fun buildPreviewItem(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ItemStack {
        val stats = computeWeapon(blueprint, materials)
        val maxDurability = computeDurability(materials, blueprint)
        val family = requireNotNull(blueprint.family)

        val item = ItemStack(family.item)
        val meta = item.itemMeta
        meta.displayName(
            noItalic(Component.text("Vista Previa", NamedTextColor.YELLOW, TextDecoration.BOLD))
        )

        val lore = mutableListOf(
            noItalic(
                Component.text(
                    "Arma — ${TextFormat.capitalizeEnumName(family.name)}",
                    NamedTextColor.GRAY
                )
            ),
            noItalic(Component.text("★".repeat(blueprint.rarity.stars), NamedTextColor.GOLD)),
            noItalic(Component.text(" "))
        )
        lore.addAll(buildWeaponStatsLore(stats, maxDurability, maxDurability))
        lore.add(noItalic(Component.text("")))
        lore.add(noItalic(Component.text("Materiales: ${materials.size}/8", NamedTextColor.DARK_GRAY)))

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    fun buildFinalWeapon(
        blueprint: BlueprintData,
        materials: List<MaterialType>,
        catalystType: CatalystType?,
        instanceUuid: UUID
    ): ItemStack {
        val stats = computeWeapon(blueprint, materials)
        val family = requireNotNull(blueprint.family)
        val maxDurability = computeDurability(materials, blueprint)
        val weaponName = WeaponNameBank.random(blueprint.rarity, stats.affinities.map { it.first })

        // TODO: reemplazar por el Material/modelo visual real según WeaponFamily
        val item = ItemStack(family.item)
        val meta = item.itemMeta

        meta.displayName(
            noItalic(Component.text(weaponName, NamedTextColor.WHITE))
        )

        val lore = mutableListOf(
            noItalic(
                Component.text(
                    "Arma — ${TextFormat.capitalizeEnumName(family.name)}",
                    NamedTextColor.GRAY
                )
            ),
            noItalic(Component.text("★".repeat(blueprint.rarity.stars), NamedTextColor.GRAY)),
            noItalic(Component.text(" "))
        )
        lore.addAll(buildWeaponStatsLore(stats, maxDurability, maxDurability))

        meta.lore(lore)

        meta.addAttributeModifier(Attribute.ATTACK_SPEED, AttributeModifier(Keys.ATTACK_SPEED, stats.attackSpeed - 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, AttributeModifier(Keys.MOBILITY, stats.mobility, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.MAINHAND))
        val isFirearm = (listOf(WeaponClass.PISTOLA, WeaponClass.ESCOPETA, WeaponClass.RIFLE).contains(blueprint.family.weaponClass))
        if (!isFirearm) {
            meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, AttributeModifier(Keys.REACH, blueprint.family.reach - 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
        } else {
            meta.persistentDataContainer.set(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE, stats.attackSpeed)
            meta.persistentDataContainer.set(Keys.REACH, PersistentDataType.DOUBLE, blueprint.family.reach * 5)
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        val pdc = meta.persistentDataContainer
        pdc.set(Keys.INSTANCE_UUID, PersistentDataType.STRING, instanceUuid.toString())
        pdc.set(Keys.WEAPON_FAMILY, PersistentDataType.STRING, family.name)
        pdc.set(Keys.BLUEPRINT_ID, PersistentDataType.STRING, blueprint.id)
        pdc.set(Keys.DURABILITY, PersistentDataType.INTEGER, maxDurability)
        pdc.set(Keys.MAX_DURABILITY, PersistentDataType.INTEGER, maxDurability)
        pdc.set(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE, family.attackSpeed)

        val materialIds = materials.map { it.name }
        pdc.set(Keys.MATERIALS, PersistentDataType.LIST.strings(), materialIds)

        catalystType?.let {
            pdc.set(Keys.CATALYST_ID, PersistentDataType.STRING, it.name)
        }

        item.itemMeta = meta
        return item
    }

    private fun buildWeaponStatsLore(
        stats: ComputedWeaponStats,
        durability: Int?,
        maxDurability: Int?
    ): List<Component> {
        val lines = mutableListOf<Component>()

        lines.add(
            noItalic(
                Component.text("Daño: ", NamedTextColor.GRAY)
                    .append(Component.text("%.1f".format(stats.damage), NamedTextColor.RED))
            )
        )
        lines.add(
            noItalic(
                Component.text("Velocidad de Ataque: ", NamedTextColor.GRAY)
                    .append(Component.text("%.1f".format(stats.attackSpeed), NamedTextColor.WHITE))
            )
        )

        if (durability != null && maxDurability != null) {
            lines.add(
                noItalic(
                    Component.text("Durabilidad: ", NamedTextColor.GRAY)
                        .append(Component.text("$durability/$maxDurability", NamedTextColor.WHITE))
                )
            )
        }

        if (stats.affinities.isNotEmpty()) {
            lines.add(noItalic(Component.text(" ")))
            lines.add(noItalic(Component.text("Afinidades", NamedTextColor.WHITE)))
            for ((affinity, pct) in stats.affinities) {
                lines.add(
                    noItalic(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(affinityDisplayName(affinity), affinityColor(affinity)))
                            .append(Component.text(" %.1f%%".format(pct), NamedTextColor.WHITE))
                    )
                )
            }
        }

        return lines
    }

    // ============================================================
    //  ARMADURAS
    // ============================================================

    data class ComputedArmorStats(
        val defense: Double,
        val primaryAttributeName: String,
        val primaryAttributeValue: Double,
        val secondaryAttributeName: String,
        val secondaryAttributeValue: Double,
        val mobilityBonus: Double = 0.0,
        val affinities: List<Pair<Affinity, Double>> = emptyList()
    )

    // Cada pieza solo "lee" 2 de los 6 atributos de MaterialData; el resto se ignora
    // por completo aunque el material tenga valores ahí.
    // Pública para que PlayerStats pueda recalcular stats de equipo en tiempo real
    // sin duplicar la lógica de cómputo por pieza.
    fun computeArmorStatsPublic(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ComputedArmorStats = computeArmor(blueprint, materials)

    private fun computeArmor(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ComputedArmorStats {
        val piece = requireNotNull(blueprint.armorPiece) {
            "computeArmor llamado con un blueprint que no es de armadura (${blueprint.id})"
        }
        val baseDefense = requireNotNull(blueprint.baseDefense)

        val totalDefenseBonus = materials.sumOf { it.data.armorDefenseBonus }
        val defense = baseDefense * (1 + totalDefenseBonus) * piece.pieceMultiplier
        val affinities = computeAffinities(materials, armorAffinityLimit(blueprint.rarity))

        return when (piece) {
            ArmorPiece.CASCO -> ComputedArmorStats(
                defense = defense,
                primaryAttributeName = "Precisión",
                primaryAttributeValue = materials.sumOf { it.data.precision },
                secondaryAttributeName = "Destreza",
                secondaryAttributeValue = materials.sumOf { it.data.dexterity },
                affinities = affinities
            )

            ArmorPiece.PECHERA -> ComputedArmorStats(
                defense = defense,
                primaryAttributeName = "Vitalidad",
                primaryAttributeValue = materials.sumOf { it.data.vitality },
                secondaryAttributeName = "Fuerza",
                secondaryAttributeValue = materials.sumOf { it.data.strength },
                affinities = affinities
            )

            ArmorPiece.GREBAS -> ComputedArmorStats(
                defense = defense,
                primaryAttributeName = "Resistencia",
                primaryAttributeValue = materials.sumOf { it.data.resistance },
                secondaryAttributeName = "Suerte",
                secondaryAttributeValue = materials.sumOf { it.data.luck },
                affinities = affinities
            )

            ArmorPiece.BOTAS -> ComputedArmorStats(
                defense = defense,
                primaryAttributeName = "Movilidad",
                primaryAttributeValue = materials.sumOf { it.data.armorMobilityBonus } * 100.0,
                secondaryAttributeName = "",
                secondaryAttributeValue = 0.0,
                mobilityBonus = materials.sumOf { it.data.armorMobilityBonus },
                affinities = affinities
            )
        }
    }

    fun buildArmorPreviewItem(
        blueprint: BlueprintData,
        materials: List<MaterialType>
    ): ItemStack {
        val stats = computeArmor(blueprint, materials)
        val piece = requireNotNull(blueprint.armorPiece)
        val maxDurability = computeDurability(materials, blueprint)

        val visualMaterial = when (piece) {
            ArmorPiece.CASCO -> org.bukkit.Material.IRON_HELMET
            ArmorPiece.PECHERA -> org.bukkit.Material.IRON_CHESTPLATE
            ArmorPiece.GREBAS -> org.bukkit.Material.IRON_LEGGINGS
            ArmorPiece.BOTAS -> org.bukkit.Material.IRON_BOOTS
        }
        val item = ItemStack(visualMaterial)
        val meta = item.itemMeta
        meta.displayName(
            noItalic(Component.text("Vista Previa", NamedTextColor.YELLOW, TextDecoration.BOLD))
        )

        val lore = mutableListOf(
            noItalic(
                Component.text(
                    "Armadura — ${TextFormat.capitalizeEnumName(piece.name)}",
                    NamedTextColor.GRAY
                )
            ),
            noItalic(Component.text("★".repeat(blueprint.rarity.stars), NamedTextColor.GRAY)),
            noItalic(Component.text(" "))
        )
        lore.addAll(buildArmorStatsLore(stats, maxDurability, maxDurability))
        lore.add(noItalic(Component.text(" ")))
        lore.add(noItalic(Component.text("Materiales: ${materials.size}/8", NamedTextColor.DARK_GRAY)))

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    fun buildFinalArmor(
        blueprint: BlueprintData,
        materials: List<MaterialType>,
        catalystType: CatalystType?,
        instanceUuid: UUID
    ): ItemStack {
        val stats = computeArmor(blueprint, materials)
        val piece = requireNotNull(blueprint.armorPiece)
        val maxDurability = computeDurability(materials, blueprint)
        val armorName = WeaponNameBank.random(blueprint.rarity, stats.affinities.map { it.first })

        // TODO: reemplazar por el Material/modelo visual real según ArmorPiece
        val visualMaterial = when (piece) {
            ArmorPiece.CASCO -> org.bukkit.Material.IRON_HELMET
            ArmorPiece.PECHERA -> org.bukkit.Material.IRON_CHESTPLATE
            ArmorPiece.GREBAS -> org.bukkit.Material.IRON_LEGGINGS
            ArmorPiece.BOTAS -> org.bukkit.Material.IRON_BOOTS
        }
        val item = ItemStack(visualMaterial)
        val meta = item.itemMeta

        meta.displayName(
            noItalic(Component.text(armorName, NamedTextColor.WHITE))
        )

        val lore = mutableListOf(
            noItalic(
                Component.text(
                    "Armadura — ${TextFormat.capitalizeEnumName(piece.name)}",
                    NamedTextColor.GRAY
                )
            ),
            noItalic(Component.text("★".repeat(blueprint.rarity.stars), NamedTextColor.GRAY)),
            noItalic(Component.text(" "))
        )
        lore.addAll(buildArmorStatsLore(stats, maxDurability, maxDurability))

        meta.lore(lore)

        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, AttributeModifier(Keys.ARMOR_MOBILITY, stats.mobilityBonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.FEET))

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        val pdc = meta.persistentDataContainer
        pdc.set(Keys.INSTANCE_UUID, PersistentDataType.STRING, instanceUuid.toString())
        pdc.set(Keys.BLUEPRINT_ID, PersistentDataType.STRING, blueprint.id)
        pdc.set(Keys.DURABILITY, PersistentDataType.INTEGER, maxDurability)
        pdc.set(Keys.MAX_DURABILITY, PersistentDataType.INTEGER, maxDurability)

        val materialIds = materials.map { it.name }
        pdc.set(Keys.MATERIALS, PersistentDataType.LIST.strings(), materialIds)

        catalystType?.let {
            pdc.set(Keys.CATALYST_ID, PersistentDataType.STRING, it.name)
        }

        item.itemMeta = meta
        return item
    }

    private fun buildArmorStatsLore(
        stats: ComputedArmorStats,
        durability: Int?,
        maxDurability: Int?
    ): List<Component> {
        val lines = mutableListOf<Component>()

        lines.add(
            noItalic(
                Component.text("Defensa: ", NamedTextColor.GRAY)
                    .append(Component.text("%.1f".format(stats.defense), NamedTextColor.WHITE))
            )
        )

        if (stats.primaryAttributeValue !in 0.0..0.09) {
            lines.add(
                noItalic(
                    Component.text("${stats.primaryAttributeName}: ", NamedTextColor.GRAY)
                        .append(Component.text("%.1f".format(stats.primaryAttributeValue), NamedTextColor.WHITE))
                )
            )
        }

        if (stats.secondaryAttributeName.isNotEmpty() && stats.secondaryAttributeValue !in 0.0..0.09) {
            lines.add(
                noItalic(
                    Component.text("${stats.secondaryAttributeName}: ", NamedTextColor.GRAY)
                        .append(Component.text("%.1f".format(stats.secondaryAttributeValue), NamedTextColor.WHITE))
                )
            )
        }

        if (durability != null && maxDurability != null) {
            lines.add(
                noItalic(
                    Component.text("Durabilidad: ", NamedTextColor.GRAY)
                        .append(Component.text("$durability/$maxDurability", NamedTextColor.WHITE))
                )
            )
        }

        if (stats.affinities.isNotEmpty()) {
            lines.add(noItalic(Component.text(" ")))
            lines.add(noItalic(Component.text("Afinidades", NamedTextColor.WHITE)))
            for ((affinity, pct) in stats.affinities) {
                lines.add(
                    noItalic(
                        Component.text("• ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(affinityDisplayName(affinity), affinityColor(affinity)))
                            .append(Component.text(" %.1f%%".format(pct), NamedTextColor.WHITE))
                    )
                )
            }
        }

        return lines
    }

    // ============================================================
    //  COMPARTIDO
    // ============================================================

    private fun computeAffinities(
        materials: List<MaterialType>,
        maxAffinities: Int
    ): List<Pair<Affinity, Double>> {
        val weights = mutableMapOf<Affinity, Int>()

        for (material in materials) {
            val affinity = material.data.affinity ?: continue
            val weight = material.data.affinityWeight
            weights[affinity] = (weights[affinity] ?: 0) + weight
        }

        if (weights.isEmpty()) return emptyList()

        // Solo se quedan las N afinidades de mayor peso permitidas por la rareza;
        // el resto se ignora (no se renormaliza con ellas, no cuentan para nada).
        val topN = weights.entries
            .sortedByDescending { it.value }
            .take(maxAffinities)

        val totalWeight = topN.sumOf { it.value }
        if (totalWeight <= 0) return emptyList()

        return topN
            .map { (affinity, w) -> affinity to (w.toDouble() / totalWeight) * 100.0 }
            .sortedByDescending { it.second }
    }

    // Máximo de afinidades distintas mostradas en un arma, según rareza del Blueprint.
    private fun weaponAffinityLimit(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON, Rarity.RARE -> 0
        Rarity.EPIC, Rarity.LEGENDARY, Rarity.MYTHIC -> 1
        Rarity.ONIRIC, Rarity.ASCENDED -> 2
    }

    // Máximo de afinidades distintas mostradas en una armadura, según rareza del Blueprint.
    private fun armorAffinityLimit(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> 0
        Rarity.RARE, Rarity.EPIC -> 1
        Rarity.LEGENDARY, Rarity.MYTHIC -> 2
        Rarity.ONIRIC, Rarity.ASCENDED -> 3
    }

    private fun noItalic(c: Component) = c.decoration(TextDecoration.ITALIC, false)

    private fun affinityColor(affinity: Affinity): NamedTextColor = when (affinity) {
        Affinity.SANGRE -> NamedTextColor.DARK_RED
        Affinity.FUEGO -> NamedTextColor.GOLD
        Affinity.HIELO -> NamedTextColor.AQUA
        Affinity.ELECTRICIDAD -> NamedTextColor.DARK_PURPLE
        Affinity.VENENO -> NamedTextColor.DARK_GREEN
        Affinity.ATADURA -> NamedTextColor.GREEN
        Affinity.FRAGILIDAD -> NamedTextColor.GRAY
    }

    private fun affinityDisplayName(affinity: Affinity): String =
        TextFormat.capitalizeEnumName(affinity.name)

    private fun computeDurability(materials: List<MaterialType>, blueprint: BlueprintData): Int {
        val base = when (blueprint.armorPiece) {
                ArmorPiece.CASCO -> blueprint.rarity.durability * ArmorPiece.CASCO.pieceMultiplier
                ArmorPiece.PECHERA -> blueprint.rarity.durability * ArmorPiece.PECHERA.pieceMultiplier
                ArmorPiece.GREBAS -> blueprint.rarity.durability * ArmorPiece.GREBAS.pieceMultiplier
                ArmorPiece.BOTAS -> blueprint.rarity.durability * ArmorPiece.BOTAS.pieceMultiplier
                null -> blueprint.rarity.durability
            }
        val bonus = materials.sumOf { it.data.durabilityModifier }
        return (base.toInt() + bonus).coerceAtLeast(100)
    }
}