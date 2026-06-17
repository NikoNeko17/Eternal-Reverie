package com.nikoneko.eternalReverie.items

object TextFormat {

    /**
     * Convierte un nombre de enum en SNAKE_CASE (ej. "ESPADA_RECTA")
     * a un texto legible con cada palabra capitalizada (ej. "Espada Recta").
     */
    fun capitalizeEnumName(rawName: String): String {
        return rawName
            .split("_")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}