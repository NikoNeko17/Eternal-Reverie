package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.command.BlueprintCommand
import com.nikoneko.eternalReverie.command.CraftingCommand
import com.nikoneko.eternalReverie.command.ItemCommand
import com.nikoneko.eternalReverie.crafting.CraftingGuiListener
import com.nikoneko.eternalReverie.durability.DurabilityListener
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.listeners.PlayerStatsListener
import com.nikoneko.eternalReverie.player.PlayerListeners
import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ActionBarManager
import com.nikoneko.eternalReverie.weapons.firearms.projectiles.ProjectileScheduler
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.TraitInfo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class EternalReverie : JavaPlugin() {
    lateinit var playerListeners: PlayerListeners
    lateinit var citizensListener: CitizensHookListener
    val npcNameList = listOf("NikoNeko17")
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
        server.pluginManager.registerEvents(DurabilityListener(this), this)
        server.pluginManager.registerEvents(PlayerStatsListener(), this)

        loadPlayerTicks()
        loadPlayerRegen()




        // Registramos el Trait (Es obligatorio hacerlo en el onEnable antes de crear NPC)
        if (server.pluginManager.isPluginEnabled("Citizens")) {
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(RpgStatsTrait::class.java as Class<out net.citizensnpcs.api.trait.Trait>)
            )

            Bukkit.getScheduler().runTaskLater(this, Runnable {
                npcTest()
            }, 40L)
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
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

        logger.info("¡NPC de prueba inicializado exitosamente en ${coordenadaSpawn.toVector()} con PDCs de vida!")
    }


    fun loadPlayerTicks() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    ActionBarManager.render(
                        player
                    )
                }
            }
        }.runTaskTimer(this, 0L, 1L)
    }

    fun loadPlayerRegen() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    val currentHealth = player.persistentDataContainer.get(Keys.CURRENT_HP, PersistentDataType.DOUBLE) ?: continue
                    PlayerStats.setCurrentHp(player, currentHealth + 1)
                }
            }
        }.runTaskTimer(this, 0L, 200L)
    }
}