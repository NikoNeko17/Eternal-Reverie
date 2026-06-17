package com.nikoneko.eternalReverie.materials

import com.nikoneko.eternalReverie.weapons.Affinity

enum class MaterialRarity(
    val stars: Int
) {
    COMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4),
    MYTHIC(5),
    ONIRIC(6)
}

data class MaterialData(
    val id: String,
    val name: String,
    val rarity: MaterialRarity,

    // Modificadores de arma (% aditivo)
    val weaponDamageBonus: Double,
    val weaponAttackSpeedBonus: Double,
    val weaponMobilityBonus: Double,

    // Modificadores de armadura (% aditivo)
    val armorDefenseBonus: Double,
    val armorMobilityBonus: Double,

    // Atributos (puntos planos, aditivo)
    val vitality: Double,
    val resistance: Double,
    val strength: Double,
    val precision: Double,
    val dexterity: Double,
    val luck: Double,

    // Crafteo
    val durabilityModifier: Int,
    val fabricationCost: Int,

    // Afinidad (null si es material físico puro)
    val affinity: Affinity? = null,
    val affinityWeight: Int = 0
)

enum class MaterialType(
    val data: MaterialData
) {

    CHATARRA_REFORZADA(
        MaterialData(
            id = "chatarra_reforzada",
            name = "Chatarra Reforzada",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.010,
            weaponAttackSpeedBonus = 0.000,
            weaponMobilityBonus = -0.010,
            armorDefenseBonus = 0.015,
            armorMobilityBonus = -0.005,
            vitality = 5.9,
            resistance = -1.5,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 10,
            fabricationCost = 5
        )
    ),

    ACERO_RECUPERADO(
        MaterialData(
            id = "acero_recuperado",
            name = "Acero Recuperado",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.020,
            weaponAttackSpeedBonus = 0.000,
            weaponMobilityBonus = -0.010,
            armorDefenseBonus = 0.010,
            armorMobilityBonus = -0.005,
            vitality = 3.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 1.5,
            luck = 0.0,
            durabilityModifier = 15,
            fabricationCost = 8
        )
    ),

    MECANISMO_GASTADO(
        MaterialData(
            id = "mecanismo_gastado",
            name = "Mecanismo Gastado",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = -0.010,
            weaponAttackSpeedBonus = 0.020,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.000,
            armorMobilityBonus = 0.005,
            vitality = 0.0,
            resistance = 3.8,
            strength = 0.0,
            precision = 0.8,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = -5,
            fabricationCost = 6
        )
    ),

    FIBRA_FLEXIBLE(
        MaterialData(
            id = "fibra_flexible",
            name = "Fibra Flexible",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = -0.010,
            weaponAttackSpeedBonus = 0.010,
            weaponMobilityBonus = 0.020,
            armorDefenseBonus = -0.005,
            armorMobilityBonus = 0.020,
            vitality = 0.0,
            resistance = 6.0,
            strength = 0.0,
            precision = -0.8,
            dexterity = 0.0,
            luck = 1.8,
            durabilityModifier = -5,
            fabricationCost = 7
        )
    ),

    PLACA_MILITAR(
        MaterialData(
            id = "placa_militar",
            name = "Placa Militar",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.020,
            weaponAttackSpeedBonus = -0.010,
            weaponMobilityBonus = -0.010,
            armorDefenseBonus = 0.020,
            armorMobilityBonus = -0.005,
            vitality = 8.9,
            resistance = -3.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 20,
            fabricationCost = 10
        )
    ),

    ACERO_MILITAR(
        MaterialData(
            id = "acero_militar",
            name = "Acero Militar",
            rarity = MaterialRarity.RARE,
            weaponDamageBonus = 0.030,
            weaponAttackSpeedBonus = -0.010,
            weaponMobilityBonus = -0.010,
            armorDefenseBonus = 0.025,
            armorMobilityBonus = -0.008,
            vitality = 13.3,
            resistance = -4.5,
            strength = 0.0,
            precision = 0.0,
            dexterity = 1.5,
            luck = 0.0,
            durabilityModifier = 25,
            fabricationCost = 15
        )
    ),

    COMPONENTES_AJUSTADOS(
        MaterialData(
            id = "componentes_ajustados",
            name = "Componentes Ajustados",
            rarity = MaterialRarity.RARE,
            weaponDamageBonus = 0.010,
            weaponAttackSpeedBonus = 0.030,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.000,
            armorMobilityBonus = 0.008,
            vitality = 0.0,
            resistance = 5.6,
            strength = 0.0,
            precision = 2.2,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = -5,
            fabricationCost = 14
        )
    ),

    NUCLEO_COMPACTO(
        MaterialData(
            id = "nucleo_compacto",
            name = "Núcleo Compacto",
            rarity = MaterialRarity.RARE,
            weaponDamageBonus = 0.020,
            weaponAttackSpeedBonus = 0.010,
            weaponMobilityBonus = 0.000,
            armorDefenseBonus = 0.015,
            armorMobilityBonus = 0.000,
            vitality = 5.9,
            resistance = 2.2,
            strength = 0.0,
            precision = 0.0,
            dexterity = 1.5,
            luck = 0.0,
            durabilityModifier = 10,
            fabricationCost = 16
        )
    ),

    ARMAZON_LIVIANO(
        MaterialData(
            id = "armazon_liviano",
            name = "Armazón Liviano",
            rarity = MaterialRarity.RARE,
            weaponDamageBonus = -0.010,
            weaponAttackSpeedBonus = 0.020,
            weaponMobilityBonus = 0.030,
            armorDefenseBonus = -0.010,
            armorMobilityBonus = 0.030,
            vitality = -4.4,
            resistance = 11.2,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 4.7,
            durabilityModifier = -10,
            fabricationCost = 15
        )
    ),

    BLINDAJE_MODULAR(
        MaterialData(
            id = "blindaje_modular",
            name = "Blindaje Modular",
            rarity = MaterialRarity.RARE,
            weaponDamageBonus = 0.030,
            weaponAttackSpeedBonus = -0.020,
            weaponMobilityBonus = -0.010,
            armorDefenseBonus = 0.030,
            armorMobilityBonus = -0.010,
            vitality = 14.8,
            resistance = -5.6,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 35,
            fabricationCost = 18
        )
    ),

    ALEACION_REFINADA(
        MaterialData(
            id = "aleacion_refinada",
            name = "Aleación Refinada",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.030,
            weaponAttackSpeedBonus = 0.010,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.030,
            armorMobilityBonus = 0.012,
            vitality = 13.3,
            resistance = 6.8,
            strength = 1.0,
            precision = 2.5,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 15,
            fabricationCost = 25
        )
    ),

    RESORTE_EXPERIMENTAL(
        MaterialData(
            id = "resorte_experimental",
            name = "Resorte Experimental",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.000,
            weaponAttackSpeedBonus = 0.040,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.000,
            armorMobilityBonus = 0.012,
            vitality = 0.0,
            resistance = 13.5,
            strength = 0.0,
            precision = 3.4,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = -10,
            fabricationCost = 24
        )
    ),

    NUCLEO_CINETICO(
        MaterialData(
            id = "nucleo_cinetico",
            name = "Núcleo Cinético",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.010,
            weaponAttackSpeedBonus = 0.040,
            weaponMobilityBonus = 0.030,
            armorDefenseBonus = -0.005,
            armorMobilityBonus = 0.025,
            vitality = -4.4,
            resistance = 15.2,
            strength = 0.0,
            precision = 4.2,
            dexterity = 0.0,
            luck = 4.2,
            durabilityModifier = -20,
            fabricationCost = 28
        )
    ),

    CHASIS_INDUSTRIAL(
        MaterialData(
            id = "chasis_industrial",
            name = "Chasis Industrial",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.040,
            weaponAttackSpeedBonus = -0.020,
            weaponMobilityBonus = -0.020,
            armorDefenseBonus = 0.040,
            armorMobilityBonus = -0.015,
            vitality = 22.2,
            resistance = -11.8,
            strength = 0.0,
            precision = 0.0,
            dexterity = 4.5,
            luck = 0.0,
            durabilityModifier = 60,
            fabricationCost = 30
        )
    ),

    ACERO_DE_ASEDIO(
        MaterialData(
            id = "acero_de_asedio",
            name = "Acero de Asedio",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.050,
            weaponAttackSpeedBonus = -0.030,
            weaponMobilityBonus = -0.020,
            armorDefenseBonus = 0.050,
            armorMobilityBonus = -0.020,
            vitality = 17.8,
            resistance = -15.2,
            strength = 2.4,
            precision = 5.9,
            dexterity = 9.0,
            luck = 0.0,
            durabilityModifier = 50,
            fabricationCost = 32
        )
    ),

    PLACA_INTERPOLADA(
        MaterialData(
            id = "placa_interpolada",
            name = "Placa Interpolada",
            rarity = MaterialRarity.LEGENDARY,
            weaponDamageBonus = 0.050,
            weaponAttackSpeedBonus = 0.010,
            weaponMobilityBonus = -0.020,
            armorDefenseBonus = 0.060,
            armorMobilityBonus = -0.020,
            vitality = 29.6,
            resistance = -11.2,
            strength = 1.8,
            precision = 0.0,
            dexterity = 7.5,
            luck = 0.0,
            durabilityModifier = 50,
            fabricationCost = 45
        )
    ),

    NUCLEO_RESONANTE(
        MaterialData(
            id = "nucleo_resonante",
            name = "Núcleo Resonante",
            rarity = MaterialRarity.LEGENDARY,
            weaponDamageBonus = 0.030,
            weaponAttackSpeedBonus = 0.040,
            weaponMobilityBonus = 0.030,
            armorDefenseBonus = 0.010,
            armorMobilityBonus = 0.025,
            vitality = 8.9,
            resistance = 18.0,
            strength = 0.0,
            precision = 6.8,
            dexterity = 0.0,
            luck = 5.6,
            durabilityModifier = 10,
            fabricationCost = 48
        )
    ),

    ALEACION_EFIMERA(
        MaterialData(
            id = "aleacion_efimera",
            name = "Aleación Efímera",
            rarity = MaterialRarity.LEGENDARY,
            weaponDamageBonus = 0.040,
            weaponAttackSpeedBonus = 0.030,
            weaponMobilityBonus = 0.040,
            armorDefenseBonus = -0.010,
            armorMobilityBonus = 0.038,
            vitality = -8.9,
            resistance = 22.5,
            strength = 0.0,
            precision = 3.4,
            dexterity = 0.0,
            luck = 11.2,
            durabilityModifier = -20,
            fabricationCost = 50
        )
    ),

    FRAGMENTO_ESTABILIZADO(
        MaterialData(
            id = "fragmento_estabilizado",
            name = "Fragmento Estabilizado",
            rarity = MaterialRarity.LEGENDARY,
            weaponDamageBonus = 0.020,
            weaponAttackSpeedBonus = 0.020,
            weaponMobilityBonus = 0.020,
            armorDefenseBonus = 0.030,
            armorMobilityBonus = 0.020,
            vitality = 17.8,
            resistance = 13.5,
            strength = 1.8,
            precision = 4.5,
            dexterity = 6.0,
            luck = 0.0,
            durabilityModifier = 40,
            fabricationCost = 46
        )
    ),

    ACERO_ASCENDENTE(
        MaterialData(
            id = "acero_ascendente",
            name = "Acero Ascendente",
            rarity = MaterialRarity.MYTHIC,
            weaponDamageBonus = 0.060,
            weaponAttackSpeedBonus = 0.020,
            weaponMobilityBonus = 0.000,
            armorDefenseBonus = 0.070,
            armorMobilityBonus = 0.000,
            vitality = 39.5,
            resistance = -18.0,
            strength = 4.8,
            precision = 12.0,
            dexterity = 18.0,
            luck = 0.0,
            durabilityModifier = 50,
            fabricationCost = 70
        )
    ),

    NUCLEO_FRACTAL(
        MaterialData(
            id = "nucleo_fractal",
            name = "Núcleo Fractal",
            rarity = MaterialRarity.MYTHIC,
            weaponDamageBonus = 0.040,
            weaponAttackSpeedBonus = 0.050,
            weaponMobilityBonus = 0.040,
            armorDefenseBonus = 0.000,
            armorMobilityBonus = 0.036,
            vitality = 0.0,
            resistance = 30.0,
            strength = 1.8,
            precision = 10.5,
            dexterity = 0.0,
            luck = 12.5,
            durabilityModifier = 20,
            fabricationCost = 75
        )
    ),

    MATRIZ_CONDENSADA(
        MaterialData(
            id = "matriz_condensada",
            name = "Matriz Condensada",
            rarity = MaterialRarity.MYTHIC,
            weaponDamageBonus = 0.050,
            weaponAttackSpeedBonus = 0.010,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.080,
            armorMobilityBonus = 0.012,
            vitality = 39.5,
            resistance = -12.0,
            strength = 3.6,
            precision = 7.5,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 80,
            fabricationCost = 72
        )
    ),

    FRAGMENTO_ONIRICO(
        MaterialData(
            id = "fragmento_onirico",
            name = "Fragmento Onírico",
            rarity = MaterialRarity.ONIRIC,
            weaponDamageBonus = 0.040,
            weaponAttackSpeedBonus = 0.040,
            weaponMobilityBonus = 0.040,
            armorDefenseBonus = 0.035,
            armorMobilityBonus = 0.035,
            vitality = 34.6,
            resistance = 26.2,
            strength = 4.5,
            precision = 11.2,
            dexterity = 15.0,
            luck = 15.6,
            durabilityModifier = 30,
            fabricationCost = 100
        )
    ),

    MATERIA_CONDENSADA(
        MaterialData(
            id = "materia_condensada",
            name = "Materia Condensada",
            rarity = MaterialRarity.ONIRIC,
            weaponDamageBonus = 0.070,
            weaponAttackSpeedBonus = 0.020,
            weaponMobilityBonus = 0.010,
            armorDefenseBonus = 0.020,
            armorMobilityBonus = 0.012,
            vitality = 24.7,
            resistance = -11.2,
            strength = 7.5,
            precision = 18.8,
            dexterity = 25.0,
            luck = 0.0,
            durabilityModifier = 100,
            fabricationCost = 110
        )
    ),

    ESENCIA_TRASCENDENTE(
        MaterialData(
            id = "esencia_trascendente",
            name = "Esencia Trascendente",
            rarity = MaterialRarity.ONIRIC,
            weaponDamageBonus = 0.050,
            weaponAttackSpeedBonus = 0.050,
            weaponMobilityBonus = 0.050,
            armorDefenseBonus = 0.100,
            armorMobilityBonus = 0.047,
            vitality = 49.4,
            resistance = 37.5,
            strength = 6.0,
            precision = 15.0,
            dexterity = 20.0,
            luck = 30.4,
            durabilityModifier = 50,
            fabricationCost = 120
        )
    ),

    TEJIDO_ALTERADO(
        MaterialData(
            id = "tejido_alterado",
            name = "Tejido Alterado",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.0,
            weaponAttackSpeedBonus = 0.0,
            weaponMobilityBonus = 0.0,
            armorDefenseBonus = 0.0,
            armorMobilityBonus = 0.0,
            vitality = 0.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 0,
            fabricationCost = 4,
            affinity = Affinity.SANGRE,
            affinityWeight = 5
        )
    ),

    CORAZON_ALTERADO(
        MaterialData(
            id = "corazon_alterado",
            name = "Corazón Alterado",
            rarity = MaterialRarity.EPIC,
            weaponDamageBonus = 0.0,
            weaponAttackSpeedBonus = 0.0,
            weaponMobilityBonus = 0.0,
            armorDefenseBonus = 0.0,
            armorMobilityBonus = 0.0,
            vitality = 0.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 0,
            fabricationCost = 10,
            affinity = Affinity.SANGRE,
            affinityWeight = 15
        )
    ),

    CARBON_VIVO(
        MaterialData(
            id = "carbon_vivo",
            name = "Carbón Vivo",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.0,
            weaponAttackSpeedBonus = 0.0,
            weaponMobilityBonus = 0.0,
            armorDefenseBonus = 0.0,
            armorMobilityBonus = 0.0,
            vitality = 0.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 0,
            fabricationCost = 4,
            affinity = Affinity.FUEGO,
            affinityWeight = 5
        )
    ),

    CABLE_SOBRECARGADO(
        MaterialData(
            id = "cable_sobrecargado",
            name = "Cable Sobrecargado",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.0,
            weaponAttackSpeedBonus = 0.0,
            weaponMobilityBonus = 0.0,
            armorDefenseBonus = 0.0,
            armorMobilityBonus = 0.0,
            vitality = 0.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 0,
            fabricationCost = 4,
            affinity = Affinity.ELECTRICIDAD,
            affinityWeight = 5
        )
    ),

    ESPORA_TOXICA(
        MaterialData(
            id = "espora_toxica",
            name = "Espora Tóxica",
            rarity = MaterialRarity.COMMON,
            weaponDamageBonus = 0.0,
            weaponAttackSpeedBonus = 0.0,
            weaponMobilityBonus = 0.0,
            armorDefenseBonus = 0.0,
            armorMobilityBonus = 0.0,
            vitality = 0.0,
            resistance = 0.0,
            strength = 0.0,
            precision = 0.0,
            dexterity = 0.0,
            luck = 0.0,
            durabilityModifier = 0,
            fabricationCost = 4,
            affinity = Affinity.VENENO,
            affinityWeight = 5
        )
    )

}