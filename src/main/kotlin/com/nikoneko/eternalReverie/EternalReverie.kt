package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.command.BlueprintCommand
import com.nikoneko.eternalReverie.command.CraftingCommand
import com.nikoneko.eternalReverie.command.ItemCommand
import com.nikoneko.eternalReverie.crafting.CraftingGuiListener
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.player.PlayerListeners
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ActionBarManager
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileScheduler
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.trait.TraitInfo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class EternalReverie : JavaPlugin() {
    lateinit var playerListeners: PlayerListeners
    lateinit var citizensListener: CitizensHookListener
    override fun onEnable() {
        Keys.init(this)
        BlueprintRegistry.generateDefaults(this)
        BlueprintRegistry.load(this)
        playerListeners = PlayerListeners(this)
        citizensListener = CitizensHookListener()
        ProjectileScheduler(this).start()

        getCommand("material-item")?.setExecutor(ItemCommand(this))
        getCommand("blueprint-item")?.setExecutor(BlueprintCommand(this))
        getCommand("craft")?.setExecutor(CraftingCommand())
        server.pluginManager.registerEvents(playerListeners, this)
        server.pluginManager.registerEvents(citizensListener, this)
        server.pluginManager.registerEvents(CraftingGuiListener(), this)

        loadPlayerTicks()

        // Registramos el Trait (Es obligatorio hacerlo en el onEnable antes de crear NPC)
        if (server.pluginManager.isPluginEnabled("Citizens")) {
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(RpgStatsTrait::class.java as Class<out net.citizensnpcs.api.trait.Trait>)
            )



            // Ejemplo: Aparecer el enemigo 5 segundos después de encender el servidor
            // Esto evita que intente aparecer antes de que el mundo cargue por completo
            Bukkit.getScheduler().runTaskLater(this, Runnable {
                val mundo = Bukkit.getWorld("world") // Reemplaza por el nombre de tu mundo principal
                if (mundo != null) {
                    val coordenadaSpawn = Location(mundo, -19.0, 83.0, 22.0) // Tus coordenadas
                    val nameList = listOf("AngryGato", "almaccino", "NikoNeko17", "Farfadox", "PalitoXDER", "YoyiArnold")
                    spawnAndActivateEnemy(coordenadaSpawn, nameList.random())
                }
            }, 100L) // 100 ticks = 5 segundos
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun spawnAndActivateEnemy(location: Location, npcName: String) {
        val registry = CitizensAPI.getNPCRegistry()
        val npc = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, npcName)

        // Hacemos que sea vulnerable para que el jugador pueda golpearlo
        npc.isProtected = false
        npc.spawn(location)

        // AJUSTES DE VELOCIDAD EXTREMA Y RUTA FLUIDA:
        val params = npc.navigator.localParameters

        params.speedModifier(1.45f)      // Incrementa sustancialmente la velocidad base para simular carrera
        params.updatePathRate(10)        // Recalcula la ruta cada 10 ticks (0.5s) en lugar de cada tick, liberando la IA
        params.useNewPathfinder(false)   // Desactiva correcciones lentas de Minecraft vanilla

        // Iniciamos nuestro bucle personalizado de IA y Daño Manual
        // Corre cada 1 tick para una precisión de combate milimétrica
        RpgHostileNpcTask(
            plugin = this,
            npc = npc
        ).runTaskTimer(this, 0L, 1L)
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