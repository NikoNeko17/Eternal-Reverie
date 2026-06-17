package com.nikoneko.eternalReverie.items

data class CatalystData(
    val id: String,
    val name: String
    // efecto real a definir más adelante (ej. boost de rareza +1)
)

enum class CatalystType(
    val data: CatalystData
) {

    CATALIZADOR_ASCENDENTE(
        CatalystData(
            id = "catalizador_ascendente",
            name = "Catalizador Ascendente"
        )
    )
}