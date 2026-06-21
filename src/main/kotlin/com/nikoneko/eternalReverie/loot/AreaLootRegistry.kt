package com.nikoneko.eternalReverie.loot

import com.google.gson.GsonBuilder
import com.nikoneko.eternalReverie.EternalReverie
import java.io.File
import java.lang.reflect.Type
import com.google.gson.reflect.TypeToken

/**
 * Registro de tablas de loot por Área/Instancia, cargado desde areas.json
 * (copiado del jar a la carpeta de datos si no existe, igual patrón que
 * BlueprintRegistry con blueprints.yml).
 *
 * save() permite REGENERAR el archivo completo desde código (ej. un comando
 * admin que agregue una entrada nueva y la persista), gracias a que Gson
 * serializa AreaLootData de forma simétrica.
 */
object AreaLootRegistry {

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val mapType: Type = object : TypeToken<Map<String, AreaLootData>>() {}.type

    private val byId: MutableMap<String, AreaLootData> = mutableMapOf()
    private lateinit var file: File

    fun load(plugin: EternalReverie) {
        byId.clear()

        file = File(plugin.dataFolder, "areas.json")
        if (!file.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.getResource("areas.json")?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }

        if (!file.exists()) {
            plugin.logger.warning("areas.json no encontrado ni en el jar ni en la carpeta de datos.")
            return
        }

        val json = file.readText(Charsets.UTF_8)
        val parsed: Map<String, AreaLootData>? = runCatching {
            gson.fromJson<Map<String, AreaLootData>>(json, mapType)
        }.getOrNull()

        if (parsed == null) {
            plugin.logger.warning("areas.json tiene un formato inválido, no se cargó ninguna tabla de Área.")
            return
        }

        byId.putAll(parsed)
        plugin.logger.info("Cargadas ${byId.size} tablas de loot de Área desde areas.json")
    }

    /** Reescribe areas.json completo con el contenido actual en memoria. */
    fun save() {
        if (!::file.isInitialized) return
        file.writeText(gson.toJson(byId, mapType), Charsets.UTF_8)
    }

    fun get(id: String): AreaLootData? = byId[id]

    fun all(): Map<String, AreaLootData> = byId

    /** Agrega o reemplaza una entrada en memoria y persiste a disco. */
    fun put(id: String, data: AreaLootData) {
        byId[id] = data
        save()
    }
}
