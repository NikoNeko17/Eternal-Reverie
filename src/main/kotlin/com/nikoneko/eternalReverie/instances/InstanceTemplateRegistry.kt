package com.nikoneko.eternalReverie.instances

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nikoneko.eternalReverie.EternalReverie
import java.io.File
import java.lang.reflect.Type

/**
 * Registro de plantillas (templateIds) disponibles por Zona, cargado desde
 * instances.json (separado de areas.json, que solo maneja loot).
 *
 * zone_1..7   -> 1 plantilla fija cada uno
 * zone_8..10  -> 3 plantillas posibles, elegidas al azar
 * zone_11..12 -> plantillas gigantes (1-2, con rituales, a definir en detalle)
 * zone_13     -> pool grande para evitar repetición
 */
object InstanceTemplateRegistry {

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val mapType: Type = object : TypeToken<Map<String, List<String>>>() {}.type

    private val byZone: MutableMap<String, List<String>> = mutableMapOf()
    private lateinit var file: File

    fun load(plugin: EternalReverie) {
        byZone.clear()

        file = File(plugin.dataFolder, "instances.json")
        if (!file.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.getResource("instances.json")?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }

        if (!file.exists()) {
            plugin.logger.warning("instances.json no encontrado ni en el jar ni en la carpeta de datos.")
            return
        }

        val json = file.readText(Charsets.UTF_8)
        val parsed: Map<String, List<String>>? = runCatching {
            gson.fromJson<Map<String, List<String>>>(json, mapType)
        }.getOrNull()

        if (parsed == null) {
            plugin.logger.warning("instances.json tiene un formato inválido.")
            return
        }

        byZone.putAll(parsed)
        plugin.logger.info("Cargadas plantillas de ${byZone.size} zonas desde instances.json")
    }

    fun save() {
        if (!::file.isInitialized) return
        file.writeText(gson.toJson(byZone, mapType), Charsets.UTF_8)
    }

    fun templatesFor(zoneId: String): List<String> = byZone[zoneId] ?: emptyList()

    fun randomTemplateFor(zoneId: String): String? = templatesFor(zoneId).randomOrNull()

    /** Agrega un templateId nuevo a una zona (usado por el comando de captura) y persiste. */
    fun addTemplate(zoneId: String, templateId: String) {
        val current = byZone[zoneId]?.toMutableList() ?: mutableListOf()
        if (templateId !in current) {
            current.add(templateId)
            byZone[zoneId] = current
            save()
        }
    }

    fun allZones(): Set<String> = byZone.keys
}
