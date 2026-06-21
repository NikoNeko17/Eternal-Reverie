package com.nikoneko.eternalReverie.instances

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.world.SlimeWorld
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import com.nikoneko.eternalReverie.EternalReverie
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Crea y destruye instancias reales de Áreas/Zonas usando la API de ASP
 * (Advanced Slime Paper). Cada instancia es un clon temporal de una plantilla
 * (Slime World) elegida desde InstanceTemplateRegistry.
 *
 * Toda operación de I/O (lectura/escritura de Slime Worlds) corre async, ya
 * que tocar el disco/red en el hilo principal del servidor congelaría el TPS.
 * Solo el registro final del World en Bukkit (asp.loadWorld) se hace de
 * vuelta en el hilo principal, como exige la API de Bukkit/Paper.
 */
class InstanceManager(private val plugin: EternalReverie) {

    private val asp: AdvancedSlimePaperAPI = AdvancedSlimePaperAPI.instance()
    private val loader: SlimeLoader = createFileLoader()

    // instanceId (UUID propio, no el nombre del mundo) -> datos de la instancia activa
    private val activeInstances: MutableMap<UUID, ActiveInstance> = ConcurrentHashMap()

    data class ActiveInstance(
        val instanceId: UUID,
        val zoneId: String,
        val templateId: String,
        val worldName: String,
        val bukkitWorld: World,
        val ownerPlayerIds: MutableSet<UUID> = mutableSetOf()
    )

    private fun createFileLoader(): SlimeLoader {
        // Carpeta donde ASP guarda los Slime Worlds (plantillas e instancias activas).
        return com.infernalsuite.asp.loaders.file.FileLoader(
            java.io.File(plugin.dataFolder, "slime_worlds")
        )
    }

    /**
     * Crea una nueva instancia para una Zona, eligiendo una plantilla al azar
     * entre las disponibles (InstanceTemplateRegistry). El nombre del mundo
     * resultante es único (UUID), para permitir múltiples instancias
     * simultáneas de la misma Zona sin chocar nombres.
     *
     * @return el future con la instancia creada, o null si la zona no tiene plantillas.
     */
    fun createInstance(zoneId: String): CompletableFuture<ActiveInstance?> {
        val templateId = InstanceTemplateRegistry.randomTemplateFor(zoneId)
        if (templateId == null) {
            plugin.logger.warning("No hay plantillas registradas para la zona '$zoneId'.")
            return CompletableFuture.completedFuture(null)
        }

        return createInstanceFromTemplate(zoneId, templateId)
    }

    fun createInstanceFromTemplate(zoneId: String, templateId: String): CompletableFuture<ActiveInstance?> {
        val instanceId = UUID.randomUUID()
        val worldName = "instance_${zoneId}_${instanceId}"

        return CompletableFuture.supplyAsync {
            // Lee la plantilla y la clona bajo un nombre único, SIN tocar la plantilla original.
            // readOnly=false en el loader de destino: la instancia es modificable
            // (el jugador puede romper bloques, etc.), a diferencia de la plantilla fuente.
            val templateWorld: SlimeWorld = asp.readWorld(loader, templateId, true, SlimePropertyMap())
            templateWorld.clone(worldName, loader)
        }.thenCompose { clonedWorld ->
            // El registro final en Bukkit (loadWorld) debe ocurrir en el hilo principal.
            runOnMainThread {
                val instance = asp.loadWorld(clonedWorld, false)
                val bukkitWorld = instance.bukkitWorld
                    ?: throw IllegalStateException("ASP no devolvió un Bukkit World válido para $worldName")

                val active = ActiveInstance(
                    instanceId = instanceId,
                    zoneId = zoneId,
                    templateId = templateId,
                    worldName = worldName,
                    bukkitWorld = bukkitWorld
                )
                activeInstances[instanceId] = active
                active
            }
        }.exceptionally { ex ->
            plugin.logger.severe("Error creando instancia de '$zoneId' (template '$templateId'): ${ex.message}")
            null
        }
    }

    /**
     * Teletransporta a un jugador a una instancia ya creada (spawn point por
     * defecto del mundo). Registra al jugador como "dueño" de la instancia,
     * para poder despawnearla cuando todos sus dueños se vayan.
     */
    fun sendPlayerToInstance(player: Player, instanceId: UUID): Boolean {
        val instance = activeInstances[instanceId] ?: return false
        instance.ownerPlayerIds.add(player.uniqueId)
        player.teleport(instance.bukkitWorld.spawnLocation)
        return true
    }

    /**
     * Destruye una instancia: descarga el mundo de Bukkit y borra sus datos
     * persistidos (la instancia es desechable, distinta de la plantilla
     * original que permanece intacta para futuras instancias).
     */
    fun destroyInstance(instanceId: UUID): CompletableFuture<Boolean> {
        val instance = activeInstances[instanceId] ?: return CompletableFuture.completedFuture(false)

        // Mover cualquier jugador restante fuera del mundo antes de descargarlo.
        for (uuid in instance.ownerPlayerIds) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            val fallbackWorld = plugin.server.worlds.firstOrNull { it != instance.bukkitWorld }
            if (fallbackWorld != null) {
                player.teleport(fallbackWorld.spawnLocation)
            }
        }

        return runOnMainThread {
            plugin.server.unloadWorld(instance.bukkitWorld, false)
        }.thenComposeAsync {
            CompletableFuture.supplyAsync {
                runCatching { loader.deleteWorld(instance.worldName) }
                activeInstances.remove(instanceId)
                true
            }
        }.exceptionally { ex ->
            plugin.logger.severe("Error destruyendo instancia '$instanceId': ${ex.message}")
            false
        }
    }

    fun getInstance(instanceId: UUID): ActiveInstance? = activeInstances[instanceId]

    fun allActiveInstances(): Collection<ActiveInstance> = activeInstances.values

    private fun <T> runOnMainThread(block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                future.complete(block())
            } catch (ex: Throwable) {
                future.completeExceptionally(ex)
            }
        })
        return future
    }
}
