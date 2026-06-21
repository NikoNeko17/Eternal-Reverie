package com.nikoneko.eternalReverie.instances

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * /capturarplantilla <zoneId> <templateId>
 *
 * Convierte el mundo VANILLA en el que está parado el jugador en un Slime
 * World guardado bajo <templateId>, y lo registra en InstanceTemplateRegistry
 * bajo la zona <zoneId>. El mundo de origen NO se modifica (ASP migra/lee
 * el mundo Anvil y escribe una copia en formato Slime).
 *
 * Requiere que el jugador esté parado en un mundo Bukkit cargado normalmente
 * (no en una instancia ASP ya activa), ya que se usa como mundo "fuente" a
 * importar. Pensado para flujo de diseño: construís la zona normal en un
 * mundo de trabajo, después la "capturás" como plantilla reutilizable.
 */
class InstanceTemplateCommand(
    private val plugin: EternalReverie,
    private val instanceManager: InstanceManager
) : CommandExecutor {

    private val asp: AdvancedSlimePaperAPI = AdvancedSlimePaperAPI.instance()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("Este comando solo puede usarse en el juego.")
            return true
        }

        if (args.size < 2) {
            player.sendMessage("§cUso: /capturarplantilla <zoneId> <templateId>")
            return true
        }

        val zoneId = args[0]
        val templateId = args[1]
        val sourceWorld = player.world

        player.sendMessage("§eCapturando '${sourceWorld.name}' como plantilla '$templateId'...")

        captureWorldAsTemplate(sourceWorld.name, templateId)
            .thenAccept { success ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (success) {
                        InstanceTemplateRegistry.addTemplate(zoneId, templateId)
                        player.sendMessage(
                            "§aPlantilla '$templateId' capturada y registrada en la zona '$zoneId'."
                        )
                    } else {
                        player.sendMessage("§cFalló la captura de la plantilla. Revisá la consola.")
                    }
                })
            }

        return true
    }

    private fun captureWorldAsTemplate(bukkitWorldName: String, templateId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            runCatching {
                val templatesLoader = com.infernalsuite.asp.loaders.file.FileLoader(
                    java.io.File(plugin.dataFolder, "slime_worlds")
                )

                // Paso 1: leer el mundo Anvil (vanilla) actual y convertirlo a SRF en memoria.
                // worldContainerDir es la carpeta raíz donde Bukkit guarda los mundos
                // (normalmente la carpeta del servidor), y bukkitWorldName es el nombre
                // de la carpeta del mundo dentro de ella.
                val worldContainerDir = plugin.server.worldContainer
                val slimeWorld = asp.readVanillaWorld(worldContainerDir, bukkitWorldName, templatesLoader)

                // Paso 2: persistir el mundo ya convertido en el loader de plantillas,
                // bajo el nombre templateId (no el nombre original del mundo vanilla).
                // SlimeWorld no tiene un "rename" directo; clonamos bajo el nombre final.
                val savedTemplate = slimeWorld.clone(templateId, templatesLoader)
                asp.saveWorld(savedTemplate)

                true
            }.getOrElse { ex ->
                plugin.logger.severe("Error capturando plantilla '$templateId': ${ex.message}")
                false
            }
        }
    }
}
