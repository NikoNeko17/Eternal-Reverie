package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

enum class Rarity(
    val stars: Int,
    val durability: Int
) {
    COMMON(1, 50),
    RARE(2, 75),
    EPIC(3, 110),
    LEGENDARY(4, 135),
    MYTHIC(5, 160),
    ONIRIC(6, 210),
    ASCENDED(7, 260)
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
            plugin.dataFolder.mkdirs()
            plugin.getResource("blueprints.yml")?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
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
                    val baseDamage = entry.getDouble("baseDamage", -1.0)
                    val family = familyStr?.let { runCatching { WeaponFamily.valueOf(it) }.getOrNull() }

                    if (family == null || baseDamage < 0.0) {
                        plugin.logger.warning(
                            "Blueprint de arma '$id' inválido (family=$familyStr, baseDamage=$baseDamage). Se omite."
                        )
                        null
                    } else {
                        BlueprintData(
                            id = id,
                            rarity = rarity,
                            material = material,
                            itemType = itemType,
                            family = family,
                            baseDamage = baseDamage
                        )
                    }
                }

                ItemType.ARMOR -> {
                    val pieceStr = entry.getString("armorPiece")
                    val baseDefense = entry.getDouble("baseDefense", -1.0)
                    val piece = pieceStr?.let { runCatching { ArmorPiece.valueOf(it) }.getOrNull() }

                    if (piece == null || baseDefense < 0.0) {
                        plugin.logger.warning(
                            "Blueprint de armadura '$id' inválido (armorPiece=$pieceStr, baseDefense=$baseDefense). Se omite."
                        )
                        null
                    } else {
                        BlueprintData(
                            id = id,
                            rarity = rarity,
                            material = material,
                            itemType = itemType,
                            armorPiece = piece,
                            baseDefense = baseDefense
                        )
                    }
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

    fun findWeaponBy(family: WeaponFamily, rarity: Rarity): BlueprintData? =
        byId.values.firstOrNull { it.itemType == ItemType.WEAPON && it.family == family && it.rarity == rarity }

    fun findArmorBy(piece: ArmorPiece, rarity: Rarity): BlueprintData? =
        byId.values.firstOrNull { it.itemType == ItemType.ARMOR && it.armorPiece == piece && it.rarity == rarity }
}
