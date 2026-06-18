package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

enum class Rarity(
    val stars: Int,
    val durability: Int,
    val defense: Double,
    val damage: Double,
    val template: Material
) {
    COMMON(1, 50, 20.0, 12.0, Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE),
    RARE(2, 75, 40.0, 26.0, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
    EPIC(3, 110, 70.0, 44.0, Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE),
    LEGENDARY(4, 135, 110.0, 62.0, Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE),
    MYTHIC(5, 160, 160.0, 88.0, Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE),
    ONIRIC(6, 210, 220.0, 122.0, Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE),
    ASCENDED(7, 260, 300.0, 152.0, Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE)
}

enum class ItemType {
    WEAPON,
    ARMOR
}

data class BlueprintData(
    val id: String,
    val rarity: Rarity,
    val material: Material,
    val itemType: ItemType,

    // --- Solo aplican si itemType == WEAPON ---
    val family: WeaponFamily? = null,
    val baseDamage: Double? = null,

    // --- Solo aplica si itemType == ARMOR ---
    val armorPiece: ArmorPiece? = null,
    val baseDefense: Double? = null
)

/**
 * Registro de Blueprints cargado desde resources/blueprints.yml (no es un enum,
 * ya que con cientos de entradas (armas + armaduras) conviene mantenerlas editables
 * sin recompilar cada vez que se ajusta un baseDamage/baseDefense.
 *
 * Llamar BlueprintRegistry.load(plugin) una vez en onEnable, después de Keys.init().
 */
object BlueprintRegistry {

    private val byId: MutableMap<String, BlueprintData> = mutableMapOf()

    fun load(plugin: EternalReverie) {
        byId.clear()

        // Copia blueprints.yml desde el jar a la carpeta de datos si no existe aún,
        // así el usuario puede editarlo en el server sin recompilar.
        val file = File(plugin.dataFolder, "blueprints.yml")
        if (!file.exists()) {
            generateDefaults(plugin)
        }

        val config = YamlConfiguration.loadConfiguration(
            InputStreamReader(file.inputStream(), Charsets.UTF_8)
        )

        val section = config.getConfigurationSection("blueprints")
        if (section == null) {
            plugin.logger.warning("blueprints.yml no tiene la sección 'blueprints', no se cargó ningún plano.")
            return
        }

        for (id in section.getKeys(false)) {
            val entry = section.getConfigurationSection(id) ?: continue

            val itemTypeStr = entry.getString("itemType", "WEAPON")!!
            val itemType = runCatching { ItemType.valueOf(itemTypeStr) }.getOrNull()
            val rarityStr = entry.getString("rarity")
            val materialStr = entry.getString("material")

            val rarity = rarityStr?.let { runCatching { Rarity.valueOf(it) }.getOrNull() }
            val material = materialStr?.let { runCatching { Material.valueOf(it) }.getOrNull() }

            if (itemType == null || rarity == null || material == null) {
                plugin.logger.warning(
                    "Blueprint '$id' inválido en blueprints.yml (itemType=$itemTypeStr, " +
                        "rarity=$rarityStr, material=$materialStr). Se omite."
                )
                continue
            }

            val data: BlueprintData? = when (itemType) {
                ItemType.WEAPON -> {
                    val familyStr = entry.getString("family")
                    val family = familyStr?.let { runCatching { WeaponFamily.valueOf(it) }.getOrNull() }

                    if (family != null) {
                        BlueprintData(
                            id = id,
                            rarity = rarity,
                            material = rarity.template,
                            itemType = itemType,
                            family = family,
                            baseDamage = rarity.damage
                        )
                    } else null
                }
                ItemType.ARMOR -> {
                    val pieceStr = entry.getString("armorPiece")

                    val piece = pieceStr?.let { runCatching { ArmorPiece.valueOf(it) }.getOrNull() }

                    if (piece != null) {
                        BlueprintData(
                            id = id,
                            rarity = rarity,
                            material = rarity.template,
                            itemType = itemType,
                            armorPiece = piece,
                            baseDefense = rarity.defense
                        )
                    } else null
                }
            }

            if (data != null) {
                byId[id] = data
            }
        }

        plugin.logger.info("Cargados ${byId.size} blueprints desde blueprints.yml")
    }

    fun get(id: String): BlueprintData? = byId[id]

    fun all(): Collection<BlueprintData> = byId.values

    fun generateDefaults(plugin: EternalReverie) {

        val file = File(
            plugin.dataFolder,
            "blueprints.yml"
        )

        plugin.dataFolder.mkdirs()

        // Backup

        if (file.exists()) {
            file.copyTo(
                File(
                    plugin.dataFolder,
                    "blueprints.yml.bak"
                ),
                overwrite = true
            )
        }

        val yaml = YamlConfiguration()
        val root = yaml.createSection(
            "blueprints"
        )

        WeaponFamily.entries.forEach { family ->
            Rarity.entries.forEach { rarity ->
                val id = "${family.name.uppercase()}_${rarity.name.uppercase()}"
                val section = root.createSection(id)
                section["itemType"] = ItemType.WEAPON.name
                section["family"] = family.name
                section["rarity"] = rarity.name
                section["material"] = rarity.template.name
            }
        }

        ArmorPiece.entries.forEach { piece ->
            Rarity.entries.forEach { rarity ->
                val id = "ARMOR_${piece.name.uppercase()}_${rarity.name.uppercase()}"
                val section = root.createSection(id)
                section["itemType"] = ItemType.ARMOR.name
                section["armorPiece"] = piece.name
                section["rarity"] = rarity.name
                section["material"] = rarity.template.name
            }
        }

        yaml.save(file)
        plugin.logger.info(
            "blueprints.yml generado."
        )
    }

    fun findWeaponBy(family: WeaponFamily, rarity: Rarity): BlueprintData? =
        byId.values.firstOrNull { it.itemType == ItemType.WEAPON && it.family == family && it.rarity == rarity }

    fun findArmorBy(piece: ArmorPiece, rarity: Rarity): BlueprintData? =
        byId.values.firstOrNull { it.itemType == ItemType.ARMOR && it.armorPiece == piece && it.rarity == rarity }
}
