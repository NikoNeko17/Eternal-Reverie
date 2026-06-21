package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.affinities.AffinityTickScheduler
import com.nikoneko.eternalReverie.command.BlueprintCommand
import com.nikoneko.eternalReverie.command.CraftingCommand
import com.nikoneko.eternalReverie.command.ItemCommand
import com.nikoneko.eternalReverie.crafting.CraftingGuiListener
import com.nikoneko.eternalReverie.durability.DurabilityListener
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.listeners.PlayerStatsListener
import com.nikoneko.eternalReverie.player.PlayerListeners
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ActionBarManager
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.BowListeners
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileScheduler
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.RealArrowKeys
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.TraitInfo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class EternalReverie : JavaPlugin() {
    val npcNameList = listOf("NikoNeko17")
    private val enemigosTemporales = mutableListOf<CustomEnemy>()
    lateinit var instanceManager : InstanceManager
    override fun onEnable() {
        Keys.init(this)
        RealArrowKeys.init(this)
        BlueprintRegistry.generateDefaults(this)
        BlueprintRegistry.load(this)
        ProjectileScheduler(this).start()
        AffinityTickScheduler(this).start()
        StaminaRegenScheduler(this).start()
        AreaLootRegistry.load(this)
        AreaLootGenerator.generateIfMissing()
        ChestLootListener.init(this)
        instanceManager = InstanceManager(this)
        InstanceTemplateRegistry.load(this)
        

        getCommand("material-item")?.setExecutor(ItemCommand(this))
        getCommand("blueprint-item")?.setExecutor(BlueprintCommand(this))
        getCommand("craft")?.setExecutor(CraftingCommand())
        getCommand("capture-template")?.setExecutor(InstanceTemplateCommand(this, instanceManager))
        server.pluginManager.registerEvents(PlayerListeners(this), this)
        server.pluginManager.registerEvents(CitizensHookListener(), this)
        server.pluginManager.registerEvents(CraftingGuiListener(), this)
        server.pluginManager.registerEvents(DurabilityListener(this), this)
        server.pluginManager.registerEvents(PlayerStatsListener(), this)
        server.pluginManager.registerEvents(BowListeners(this), this)
        server.pluginManager.registerEvents(SprintStaminaListener(this), this)
        server.pluginManager.registerEvents(CurrencyListener(), this)
        loadPlayerTicks()

        // Registramos el Trait (Es obligatorio hacerlo en el onEnable antes de crear NPC)
        if (server.pluginManager.isPluginEnabled("Citizens")) {
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(RpgStatsTrait::class.java as Class<out net.citizensnpcs.api.trait.Trait>)
            )

            // Ejemplo: Aparecer el enemigo 5 segundos después de encender el servidor
            // Esto evita que intente aparecer antes de que el mundo cargue por completo
            Bukkit.getScheduler().runTaskLater(this, Runnable {
                npcTest()
            }, 100L) // 100 ticks = 5 segundos
        }
    }

    override fun onDisable() {
        logger.info("Iniciando limpieza masiva de NPCs temporales y NameDisplays...")

        // 1. Eliminamos de forma ordenada cada enemigo de nuestra lista activa
        // Usamos un iterador o una copia (.toTypedArray) para evitar errores de modificación concurrente
        enemigosTemporales.toTypedArray().forEach { enemigo ->
            enemigo.eliminar() // Esto borra el TextDisplay, des-spawnea el NPC y lo quita de Citizens
        }
        enemigosTemporales.clear()

        // 2. ¡BARRIDO DE SEGURIDAD ABSOLUTO! (Opcional, pero altamente recomendado)
        // Si el servidor crashea o se apaga de golpe, algunos TextDisplays podrían quedar flotando en el mapa.
        // Este bucle busca cualquier TextDisplay remanente en el mundo "world" y lo remueve del mapa.
        val mundoPrincipal = Bukkit.getWorld("world")
        mundoPrincipal?.entities?.forEach { entidad ->
            if (entidad == EntityType.TEXT_DISPLAY.entityClass) {
                // Puedes verificar si tiene tus coordenadas o simplemente remover los displays flotantes
                entidad.remove()
            }
        }

        logger.info("¡Limpieza completada! Todos los registros temporales han sido purgados de Citizens.")
    }

    private fun npcTest() {
        // 1. Obtenemos el mundo principal del servidor (por defecto suele llamarse "world")
        val mundo = Bukkit.getWorld("world")
        if (mundo == null) {
            logger.warning("¡No se pudo encontrar el mundo 'world' para spawnear el NPC de prueba!")
            return
        }
        
        // 2. Definimos las coordenadas exactas de spawn (X, Y, Z) en el centro del mapa
        val coordenadaSpawn = Location(mundo, 0.0, 100.0, 0.0)

        // Buscamos la superficie sólida más alta de forma automática para que no spawnee en el aire
        coordenadaSpawn.y = mundo.getHighestBlockYAt(coordenadaSpawn).toDouble() + 1.0

        // 3. Obtenemos el registro de Citizens y creamos el molde del NPC
        val registry = CitizensAPI.getNPCRegistry()
        val npc = registry.createNPC(EntityType.PLAYER, "[NPC] ${npcNameList.random()}")

        // Desactivamos la etiqueta flotante de Citizens para usar tu TextDisplay custom
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false)

        // 4. Instanciamos tu clase custom con el molde y la coordenada
        val enemigoPrueba = CustomEnemy(npc, coordenadaSpawn, this)

        // 5. ¡Activamos el NPC! Aquí adentro se ejecuta el npc.spawn(), se rellenan los PDCs y arranca la IA
        enemigoPrueba.iniciar()

        enemigosTemporales.add(enemigoPrueba)

        logger.info("¡NPC de prueba inicializado exitosamente en ${coordenadaSpawn.toVector()} con PDCs de vida!")
    }


    fun loadPlayerTicks(){
        object : BukkitRunnable(){
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    ActionBarManager.render(
                        player
                    )
                }
            }
        }.runTaskTimer(this, 0L, 1L)
    }
}