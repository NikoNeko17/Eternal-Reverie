package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.crafting.MaterialType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Stats totales calculadas en tiempo real para un LivingEntity (jugador o NPC
 * de Citizens tipo PLAYER, ambos comparten esta misma API).
 *
 * Vitalidad y Resistencia (Stamina) tienen un componente custom persistido en PDC
 * (currentHp/maxHp, currentStamina/maxStamina) porque representan un "pool" que se
 * gasta y regenera con el tiempo, no algo que se recalcula puro desde el equipo en
 * cada instante. El resto (defense, precision, dexterity, strength, luck) se
 * recalcula 100% desde el equipamiento cada vez que se piden, sin caché.
 */
object PlayerStats {

    // ============================================================
    //  CONSTANTES BASE
    // ============================================================

    private const val BASE_MAX_HP = 100.0
    private const val BASE_MAX_STAMINA = 100.0

    private const val PRECISION_INTERNAL_BONUS = 0.05  // +5% crit chance base
    private const val DEXTERITY_INTERNAL_BONUS = 0.5   // +50% crit damage base

    private const val PRECISION_RATIO = 0.01
    private const val DEXTERITY_RATIO = 0.01
    private const val STRENGTH_RATIO = 0.01
    private const val LUCK_RATIO = 0.003

    // ============================================================
    //  STATS DERIVADAS DEL EQUIPO (recalculadas en tiempo real)
    // ============================================================

    data class EquipmentStats(
        val defense: Double,
        val critChance: Double,      // 0.0 - 1.0 (ej. 0.05 = 5%)
        val critDamageMultiplier: Double,
        val strengthMultiplier: Double, // multiplicador de daño global (1.0 = sin bonus)
        val luckAttributeValue: Double, // valor final para generic.luck
        val maxHpFromGear: Double,      // suma de Vitalidad de las piezas equipadas
        val maxStaminaFromGear: Double  // suma de Resistencia de las piezas equipadas
    )

    /**
     * Recorre las 4 piezas de armadura equipadas (casco/pechera/grebas/botas),
     * lee sus materiales desde el PDC, y suma los atributos relevantes de cada uno
     * (mismo criterio que CraftingCalculator: cada pieza solo aporta sus 2 atributos
     * propios + defensa, no hay mezcla cruzada entre piezas).
     */
    fun computeEquipmentStats(entity: LivingEntity): EquipmentStats {
        val equipment = entity.equipment

        val helmet = equipment?.helmet
        val chestplate = equipment?.chestplate
        val leggings = equipment?.leggings
        val boots = equipment?.boots

        var totalDefense = 0.0
        var totalPrecision = 0.0
        var totalDexterity = 0.0
        var totalVitality = 0.0
        var totalStrength = 0.0
        var totalResistance = 0.0
        var totalLuck = 0.0

        for (piece in listOf(helmet, chestplate, leggings, boots)) {
            val pieceStats = readArmorPieceContribution(piece) ?: continue
            totalDefense += pieceStats.defense
            totalPrecision += pieceStats.precision
            totalDexterity += pieceStats.dexterity
            totalVitality += pieceStats.vitality
            totalStrength += pieceStats.strength
            totalResistance += pieceStats.resistance
            totalLuck += pieceStats.luck
        }

        val critChance = (totalPrecision * PRECISION_RATIO) + PRECISION_INTERNAL_BONUS
        val critDamageMultiplier = totalDexterity * DEXTERITY_RATIO + DEXTERITY_INTERNAL_BONUS
        val strengthMultiplier = totalStrength * STRENGTH_RATIO
        val luckValue = totalLuck * LUCK_RATIO

        return EquipmentStats(
            defense = totalDefense,
            critChance = critChance,
            critDamageMultiplier = critDamageMultiplier,
            strengthMultiplier = strengthMultiplier,
            luckAttributeValue = luckValue,
            maxHpFromGear = totalVitality,
            maxStaminaFromGear = totalResistance
        )
    }

    // Stats que aporta UNA pieza de armadura puesta (defensa + sus 2 atributos propios).
    private data class ArmorPieceContribution(
        val defense: Double,
        val precision: Double,
        val dexterity: Double,
        val vitality: Double,
        val strength: Double,
        val resistance: Double,
        val luck: Double
    )

    // Lee el blueprint + materiales del PDC de la pieza y reusa CraftingCalculator
    // para obtener exactamente los mismos números que se mostraron en el lore al craftear.
    private fun readArmorPieceContribution(item: ItemStack?): ArmorPieceContribution? {
        if (item == null) return null
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer

        val blueprintId = pdc.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING) ?: return null
        val blueprint = BlueprintRegistry.get(blueprintId) ?: return null
        if (blueprint.armorPiece == null) return null // no es una pieza de armadura custom

        val materialIds = pdc.get(Keys.MATERIALS, PersistentDataType.LIST.strings()) ?: emptyList()
        val materials: List<MaterialType> = materialIds.mapNotNull {
            runCatching { MaterialType.valueOf(it) }.getOrNull()
        }

        val stats = CraftingCalculator.computeArmorStatsPublic(blueprint, materials)

        // Cada pieza solo "tiene" 2 atributos relevantes (primary/secondary);
        // los otros quedan en 0 para esta pieza, tal como se definió al craftear.
        var precision = 0.0
        var dexterity = 0.0
        var vitality = 0.0
        var strength = 0.0
        var resistance = 0.0
        var luck = 0.0

        when (stats.primaryAttributeName) {
            "Precisión" -> precision = stats.primaryAttributeValue
            "Vitalidad" -> vitality = stats.primaryAttributeValue
            "Resistencia" -> resistance = stats.primaryAttributeValue
            "Movilidad" -> { /* no aporta a ningún stat de PlayerStats directamente */ }
        }
        when (stats.secondaryAttributeName) {
            "Destreza" -> dexterity = stats.secondaryAttributeValue
            "Fuerza" -> strength = stats.secondaryAttributeValue
            "Suerte" -> luck = stats.secondaryAttributeValue
        }

        return ArmorPieceContribution(
            defense = stats.defense,
            precision = precision,
            dexterity = dexterity,
            vitality = vitality,
            strength = strength,
            resistance = resistance,
            luck = luck
        )
    }

    // Lee el blueprint + materiales del PDC de la pieza y devuelve sus afinidades
    // normalizadas (0-100%), tal cual se calcularon al craftear. Usado por
    // AffinityMarkManager para calcular la mitigación de Marcas vía afinidad de set.
    fun readArmorPieceAffinities(item: ItemStack?): List<Pair<com.nikoneko.eternalReverie.weapons.Affinity, Double>>? {
        if (item == null) return null
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer

        val blueprintId = pdc.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING) ?: return null
        val blueprint = BlueprintRegistry.get(blueprintId) ?: return null
        if (blueprint.armorPiece == null) return null

        val materialIds = pdc.get(Keys.MATERIALS, PersistentDataType.LIST.strings()) ?: emptyList()
        val materials: List<MaterialType> = materialIds.mapNotNull {
            runCatching { MaterialType.valueOf(it) }.getOrNull()
        }

        return CraftingCalculator.computeArmorStatsPublic(blueprint, materials).affinities
    }

    // ============================================================
    //  VITALIDAD / STAMINA (custom, persistido en PDC de la entidad)
    // ============================================================

    /** Llamar una vez cuando la entidad aparece por primera vez (join/spawn de NPC). */
    fun initializeIfAbsent(entity: LivingEntity) {
        val pdc = entity.persistentDataContainer
        if (!pdc.has(Keys.MAX_HP, PersistentDataType.DOUBLE)) {
            pdc.set(Keys.MAX_HP, PersistentDataType.DOUBLE, BASE_MAX_HP)
            pdc.set(Keys.CURRENT_HP, PersistentDataType.DOUBLE, BASE_MAX_HP)
        }
        if (!pdc.has(Keys.MAX_STAMINA, PersistentDataType.DOUBLE)) {
            pdc.set(Keys.MAX_STAMINA, PersistentDataType.DOUBLE, BASE_MAX_STAMINA)
            pdc.set(Keys.CURRENT_STAMINA, PersistentDataType.DOUBLE, BASE_MAX_STAMINA)
        }
    }

    fun getCurrentHp(entity: LivingEntity): Double =
        entity.persistentDataContainer.get(Keys.CURRENT_HP, PersistentDataType.DOUBLE) ?: BASE_MAX_HP

    fun getMaxHp(entity: LivingEntity): Double =
        entity.persistentDataContainer.get(Keys.MAX_HP, PersistentDataType.DOUBLE) ?: BASE_MAX_HP

    fun getCurrentStamina(entity: LivingEntity): Double =
        entity.persistentDataContainer.get(Keys.CURRENT_STAMINA, PersistentDataType.DOUBLE) ?: BASE_MAX_STAMINA

    fun getMaxStamina(entity: LivingEntity): Double =
        entity.persistentDataContainer.get(Keys.MAX_STAMINA, PersistentDataType.DOUBLE) ?: BASE_MAX_STAMINA

    /**
     * Recalcula MAX_HP = 100 + Vitalidad del equipo, sin tocar CURRENT_HP
     * (confirmado: la salud actual no se escala al cambiar de equipo).
     * Si el nuevo máximo es menor al HP actual, lo recorta para no dejar HP "fantasma".
     */
    /**
     * Recalcula MAX_HP = 100 + Vitalidad del equipo + extraBonus (ej. Vestigios).
     * extraBonus se pasa desde afuera para evitar que este paquete (player)
     * dependa del paquete vestigios; quien llame con un bonus debe sumarlo él mismo.
     */
    fun recalculateMaxHp(entity: LivingEntity, extraVitalityBonus: Double = 0.0) {
        val gearStats = computeEquipmentStats(entity)
        val newMax = BASE_MAX_HP + gearStats.maxHpFromGear + extraVitalityBonus

        val pdc = entity.persistentDataContainer
        pdc.set(Keys.MAX_HP, PersistentDataType.DOUBLE, newMax)

        val current = getCurrentHp(entity)
        if (current > newMax) {
            pdc.set(Keys.CURRENT_HP, PersistentDataType.DOUBLE, newMax)
        }

        syncVanillaHealthBar(entity)
    }

    /** Igual que recalculateMaxHp, pero para Stamina (Resistencia). */
    fun recalculateMaxStamina(entity: LivingEntity) {
        val gearStats = computeEquipmentStats(entity)
        val newMax = BASE_MAX_STAMINA + gearStats.maxStaminaFromGear

        val pdc = entity.persistentDataContainer
        pdc.set(Keys.MAX_STAMINA, PersistentDataType.DOUBLE, newMax)

        val current = getCurrentStamina(entity)
        if (current > newMax) {
            pdc.set(Keys.CURRENT_STAMINA, PersistentDataType.DOUBLE, newMax)
        }
    }

    fun setCurrentHp(entity: LivingEntity, value: Double) {
        val max = getMaxHp(entity)
        val clamped = value.coerceIn(0.0, max)
        entity.persistentDataContainer.set(Keys.CURRENT_HP, PersistentDataType.DOUBLE, clamped)
        syncVanillaHealthBar(entity)
    }

    fun setCurrentStamina(entity: LivingEntity, value: Double) {
        val max = getMaxStamina(entity)
        val clamped = value.coerceIn(0.0, max)
        entity.persistentDataContainer.set(Keys.CURRENT_STAMINA, PersistentDataType.DOUBLE, clamped)
    }

    // Sincroniza la barra de corazones vanilla con el % real de HP custom,
    // igual criterio que la barra de durabilidad: (current/max) × escalaVanilla.
    private fun syncVanillaHealthBar(entity: LivingEntity) {
        val maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH) ?: return
        val vanillaMax = maxHealthAttr.baseValue // normalmente 20.0

        val current = getCurrentHp(entity)
        val max = getMaxHp(entity)
        if (max <= 0) return

        val pct = (current / max).coerceIn(0.0, 1.0)
        if (current > 0) entity.health = (pct * vanillaMax).coerceIn(0.0, vanillaMax)

    }
}