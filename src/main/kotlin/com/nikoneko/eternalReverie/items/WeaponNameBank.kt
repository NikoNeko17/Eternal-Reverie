package com.nikoneko.eternalReverie.items

import com.nikoneko.eternalReverie.weapons.Weapons.Affinity
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
    val article: String,
    val gender: Gender
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

    // Adjetivos genéricos de afinidad (usados solo si no hay una Affinity real
    // para tematizar el nombre, ej. tier sin afinidad colocada).
    private val affinityAdjectives = listOf(
        Adjective("Carmesí"),
        Adjective("Incandescente"),
        Adjective("Glacial"),
        Adjective("Tempestuoso", "Tempestuosa"),
        Adjective("Corrupto", "Corrupta"),
        Adjective("Encadenado", "Encadenada"),
        Adjective("Quebrado", "Quebrada")
    )

    // 4 adjetivos temáticos por cada Affinity real, usados cuando el ítem
    // craftado sí tiene una afinidad dominante (ver parámetro `affinities` en random()).
    private val adjectivesByAffinity: Map<Affinity, List<Adjective>> = mapOf(
        Affinity.SANGRE to listOf(
            Adjective("Carmesí"),
            Adjective("Sangriento", "Sangrienta"),
            Adjective("Desangrado", "Desangrada"),
            Adjective("Escarlata")
        ),
        Affinity.FUEGO to listOf(
            Adjective("Incandescente"),
            Adjective("Ardiente"),
            Adjective("Calcinado", "Calcinada"),
            Adjective("Abrasador", "Abrasadora")
        ),
        Affinity.HIELO to listOf(
            Adjective("Glacial"),
            Adjective("Helado", "Helada"),
            Adjective("Escarchado", "Escarchada"),
            Adjective("Gélido", "Gélida")
        ),
        Affinity.ELECTRICIDAD to listOf(
            Adjective("Tempestuoso", "Tempestuosa"),
            Adjective("Fulminante"),
            Adjective("Cargado", "Cargada"),
            Adjective("Voltaico", "Voltaica")
        ),
        Affinity.VENENO to listOf(
            Adjective("Corrupto", "Corrupta"),
            Adjective("Ponzoñoso", "Ponzoñosa"),
            Adjective("Pútrido", "Pútrida"),
            Adjective("Tóxico", "Tóxica")
        ),
        Affinity.ATADURA to listOf(
            Adjective("Encadenado", "Encadenada"),
            Adjective("Atado", "Atada"),
            Adjective("Grilletado", "Grilletada"),
            Adjective("Sometido", "Sometida")
        ),
        Affinity.FRAGILIDAD to listOf(
            Adjective("Quebrado", "Quebrada"),
            Adjective("Fracturado", "Fracturada"),
            Adjective("Frágil"),
            Adjective("Resquebrajado", "Resquebrajada")
        )
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
        AuxiliaryNoun("Vigilia", "la", Gender.FEMININE),
        AuxiliaryNoun("Ascensión", "la", Gender.FEMININE),
        AuxiliaryNoun("Ruina", "la", Gender.FEMININE),
        AuxiliaryNoun("Horizonte", "el", Gender.MASCULINE),
        AuxiliaryNoun("Abismo", "el", Gender.MASCULINE),
        AuxiliaryNoun("Crepúsculo", "el", Gender.MASCULINE),
        AuxiliaryNoun("Peregrino", "el", Gender.MASCULINE),
        AuxiliaryNoun("Santuario", "el", Gender.MASCULINE)
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

    // Elige un adjetivo de afinidad: si hay una Affinity real dominante, usa su banco
    // temático; si no, cae al banco genérico de afinidad (comportamiento anterior).
    private fun pickAffinityAdjective(dominantAffinity: Affinity?): Adjective {
        val pool = dominantAffinity?.let { adjectivesByAffinity[it] }
        return pool?.random() ?: affinityAdjectives.random()
    }

    /**
     * Genera un nombre temático para un arma/armadura según su Rarity
     * (com.nikoneko.eternalReverie.items.Rarity, 1-7 estrellas).
     *
     * @param affinities lista de afinidades presentes en el ítem craftado, ordenadas
     *   de mayor a menor peso (ej. el resultado de CraftingCalculator.computeAffinities).
     *   La primera (si existe) se usa como afinidad dominante para tematizar el adjetivo.
     *   Si está vacía, se usa un adjetivo genérico de afinidad (comportamiento anterior).
     *
     * Reglas de concordancia: cada adjetivo concuerda en género con el sustantivo
     * gramaticalmente más cercano que lo precede. Cuando hay un AuxiliaryNoun (tier
     * MYTHIC+), todo lo que va después de él (incluyendo el adjetivo de afinidad y el
     * epicAdjective final) concuerda con SU género, no con el del noun principal.
     *
     * Mapeo de bandas:
     *  - COMMON, RARE        (1-2★) -> nombre genérico simple
     *  - EPIC, LEGENDARY     (3-4★) -> nombre con adjetivo de afinidad
     *  - MYTHIC, ONIRIC      (5-6★) -> nombre compuesto con sustantivo auxiliar
     *  - ASCENDED            (7★)   -> igual que MYTHIC/ONIRIC, siempre con epíteto épico
     */
    fun random(rarity: Rarity, affinities: List<Affinity> = emptyList()): String {
        val dominantAffinity = affinities.firstOrNull()

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

                // El adjetivo de afinidad es lo último en la frase: concuerda con noun.
                val adjective = pickAffinityAdjective(dominantAffinity)
                "${noun.text} ${adjective.forGender(noun.gender)}"
            }

            Rarity.MYTHIC,
            Rarity.ONIRIC,
            Rarity.ASCENDED -> {
                val noun = epicNouns.random()
                val auxiliary = auxiliaryNouns.random()

                // A partir de acá, auxiliary es el sustantivo más cercano:
                // tanto el adjetivo de afinidad como el epicAdjective (si aplica)
                // concuerdan con auxiliary.gender, no con noun.gender.
                val affinityAdjective = pickAffinityAdjective(dominantAffinity)

                val connector = if (auxiliary.article == "el") "del" else "de la"

                val base = "${noun.text} $connector ${auxiliary.text} " +
                    affinityAdjective.forGender(auxiliary.gender)

                val forceEpic = rarity == Rarity.ASCENDED
                if (forceEpic || Random.nextInt(100) < 50) {
                    val epic = epicAdjectives.random()
                    "$base ${epic.forGender(auxiliary.gender)}"
                } else {
                    base
                }
            }
        }
    }
}
