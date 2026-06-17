package com.nikoneko.eternalReverie.items

import kotlin.random.Random

enum class Gender {
    MASCULINE,
    FEMININE
}

data class Noun(
    val text: String,
    val gender: Gender
)

data class AuxiliaryNoun(
    val text: String,
    val article: String
)

data class Adjective(
    val masculine: String,
    val feminine: String = masculine
) {
    fun forGender(gender: Gender): String {
        return when (gender) {
            Gender.MASCULINE -> masculine
            Gender.FEMININE -> feminine
        }
    }
}

object WeaponNameBank {

    private val weaponTypes = listOf(
        Noun("Escopeta de Corredera", Gender.FEMININE),
        Noun("Trabuco", Gender.MASCULINE),
        Noun("Rifle", Gender.MASCULINE),
        Noun("Carabina", Gender.FEMININE),
        Noun("Pistola", Gender.FEMININE),
        Noun("Revólver", Gender.MASCULINE),
        Noun("Sable", Gender.MASCULINE),
        Noun("Espada", Gender.FEMININE),
        Noun("Mandoble", Gender.MASCULINE),
        Noun("Pica", Gender.FEMININE),
        Noun("Lanza", Gender.FEMININE),
        Noun("Alabarda", Gender.FEMININE),
        Noun("Guantelete", Gender.MASCULINE),
        Noun("Hacha", Gender.FEMININE),
        Noun("Arco Compuesto", Gender.MASCULINE)
    )

    private val genericAdjectives = listOf(
        Adjective("Recuperado", "Recuperada"),
        Adjective("Militar"),
        Adjective("Reforzado", "Reforzada"),
        Adjective("Modificado", "Modificada"),
        Adjective("Desgastado", "Desgastada"),
        Adjective("Antiguo", "Antigua"),
        Adjective("Robusto", "Robusta")
    )

    private val affinityAdjectives = listOf(
        Adjective("Carmesí"),
        Adjective("Incandescente"),
        Adjective("Glacial"),
        Adjective("Tempestuoso", "Tempestuosa"),
        Adjective("Corrupto", "Corrupta"),
        Adjective("Encadenado", "Encadenada"),
        Adjective("Quebrado", "Quebrada")
    )

    private val epicNouns = listOf(
        Noun("Epitafio", Gender.MASCULINE),
        Noun("Réquiem", Gender.MASCULINE),
        Noun("Vestigio", Gender.MASCULINE),
        Noun("Juramento", Gender.MASCULINE),
        Noun("Corona", Gender.FEMININE),
        Noun("Elegía", Gender.FEMININE),
        Noun("Herencia", Gender.FEMININE),
        Noun("Silencio", Gender.MASCULINE)
    )

    private val auxiliaryNouns = listOf(
        AuxiliaryNoun("Vigilia", "la"),
        AuxiliaryNoun("Ascensión", "la"),
        AuxiliaryNoun("Ruina", "la"),
        AuxiliaryNoun("Horizonte", "el"),
        AuxiliaryNoun("Abismo", "el"),
        AuxiliaryNoun("Crepúsculo", "el"),
        AuxiliaryNoun("Peregrino", "el"),
        AuxiliaryNoun("Santuario", "el")
    )

    private val epicAdjectives = listOf(
        Adjective("Eterno", "Eterna"),
        Adjective("Onírico", "Onírica"),
        Adjective("Primordial"),
        Adjective("Inalcanzable"),
        Adjective("Astral"),
        Adjective("Sublime"),
        Adjective("Trascendente")
    )

    /**
     * Genera un nombre temático para un arma/armadura según su Rarity
     * (com.nikoneko.eternalReverie.items.Rarity, 1-7 estrellas).
     *
     * Mapeo de bandas:
     *  - COMMON, RARE        (1-2★) -> nombre genérico simple
     *  - EPIC, LEGENDARY     (3-4★) -> nombre con adjetivo de afinidad
     *  - MYTHIC, ONIRIC      (5-6★) -> nombre compuesto con sustantivo auxiliar
     *  - ASCENDED            (7★)   -> igual que MYTHIC/ONIRIC, siempre con epíteto épico
     */
    fun random(rarity: Rarity): String {
        return when (rarity) {

            Rarity.COMMON,
            Rarity.RARE -> {
                val noun = weaponTypes.random()
                val adjective = genericAdjectives.random()
                "${noun.text} ${adjective.forGender(noun.gender)}"
            }

            Rarity.EPIC,
            Rarity.LEGENDARY -> {
                val noun =
                    if (Random.nextBoolean()) weaponTypes.random()
                    else epicNouns.random()

                val adjective = affinityAdjectives.random()
                "${noun.text} ${adjective.forGender(noun.gender)}"
            }

            Rarity.MYTHIC,
            Rarity.ONIRIC,
            Rarity.ASCENDED -> {
                val noun = epicNouns.random()
                val auxiliary = auxiliaryNouns.random()
                val affinity = affinityAdjectives.random()

                val connector = if (auxiliary.article == "el") "del" else "de la"

                val base = "${noun.text} $connector ${auxiliary.text} ${affinity.forGender(noun.gender)}"

                val forceEpic = rarity == Rarity.ASCENDED
                if (forceEpic || Random.nextInt(100) < 50) {
                    val epic = epicAdjectives.random()
                    "$base ${epic.forGender(noun.gender)}"
                } else {
                    base
                }
            }
        }
    }
}
