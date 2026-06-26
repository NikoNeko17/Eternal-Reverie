package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.affinities.AffinityTickScheduler
import com.nikoneko.eternalReverie.affinities.MovementSpeedModifier
import com.nikoneko.eternalReverie.command.BlueprintCommand
import com.nikoneko.eternalReverie.command.CraftingCommand
import com.nikoneko.eternalReverie.command.ItemCommand
import com.nikoneko.eternalReverie.crafting.CraftingGuiListener
import com.nikoneko.eternalReverie.durability.DurabilityListener
import com.nikoneko.eternalReverie.economy.CurrencyCommand
import com.nikoneko.eternalReverie.economy.CurrencyListener
import com.nikoneko.eternalReverie.instances.InstanceManager
import com.nikoneko.eternalReverie.instances.InstanceTemplateCommand
import com.nikoneko.eternalReverie.instances.InstanceTemplateRegistry
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.listeners.PlayerStatsListener
import com.nikoneko.eternalReverie.loot.AreaLootGenerator
import com.nikoneko.eternalReverie.loot.AreaLootRegistry
import com.nikoneko.eternalReverie.loot.ChestLootListener
import com.nikoneko.eternalReverie.player.PlayerListeners
import com.nikoneko.eternalReverie.player.SprintStaminaListener
import com.nikoneko.eternalReverie.player.StaminaRegenScheduler
import com.nikoneko.eternalReverie.remnants.MAX_VESTIGIO_LEVEL
import com.nikoneko.eternalReverie.remnants.RemnantCommand
import com.nikoneko.eternalReverie.remnants.RemnantGuiHolder
import com.nikoneko.eternalReverie.remnants.RemnantGuiListener
import com.nikoneko.eternalReverie.remnants.RemnantItemFactory
import com.nikoneko.eternalReverie.remnants.RemnantKeys
import com.nikoneko.eternalReverie.remnants.RemnantType
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
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap

class EternalReverie : JavaPlugin() {
    val npcNameList = listOf("NikoNeko17")
    lateinit var instanceManager : InstanceManager
    override fun onEnable() {

        Keys.init(this)
        RealArrowKeys.init(this)
        RemnantKeys.init(this)
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
        MovementSpeedModifier.init(this)
        SprintStaminaListener(this).startTick()
        FoodEffectScheduler(this).start()
        server.pluginManager.registerEvents(FoodListener(), this)
        

        getCommand("material-item")?.setExecutor(ItemCommand(this))
        getCommand("blueprint-item")?.setExecutor(BlueprintCommand(this))
        getCommand("craft")?.setExecutor(CraftingCommand())
        getCommand("capture-template")?.setExecutor(InstanceTemplateCommand(this, instanceManager))
        getCommand("scrap")?.setExecutor(CurrencyCommand(this))
        getCommand("remnants")?.setExecutor(RemnantCommand())
        getCommand("give-remnant")?.setExecutor { sender, _, _, args ->
            if (sender !is Player) return@setExecutor true
            val tipo = runCatching {
                com.nikoneko.eternalReverie.remnants.RemnantType.valueOf(args[0].uppercase())
            }.getOrNull() ?: return@setExecutor true
            val nivel   = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, MAX_VESTIGIO_LEVEL) ?: 1
            // Tercer argumento opcional: "eternal" para dar la versión Eterna
            val eternal = args.getOrNull(2)?.lowercase() == "eternal"
            sender.inventory.addItem(
                com.nikoneko.eternalReverie.remnants.RemnantItemFactory.create(tipo, nivel, eternal)
            )
            true
        }
        getCommand("give-food")?.setExecutor { sender, _, _, args ->
            if (sender !is Player) return@setExecutor true
            val type = runCatching {
                com.nikoneko.eternalReverie.food.FoodType.valueOf(args[0].uppercase())
            }.getOrNull() ?: return@setExecutor true
            sender.inventory.addItem(
                com.nikoneko.eternalReverie.food.FoodItemFactory.create(type)
            )
            true
        }
        
        server.pluginManager.registerEvents(PlayerListeners(this), this)
        server.pluginManager.registerEvents(CitizensHookListener(), this)
        server.pluginManager.registerEvents(CraftingGuiListener(), this)
        server.pluginManager.registerEvents(DurabilityListener(this), this)
        server.pluginManager.registerEvents(PlayerStatsListener(), this)
        server.pluginManager.registerEvents(BowListeners(this), this)
        server.pluginManager.registerEvents(SprintStaminaListener(this), this)
        server.pluginManager.registerEvents(CurrencyListener(), this)
        server.pluginManager.registerEvents(ChestLootListener(this), this)
        server.pluginManager.registerEvents(RemnantGuiListener(), this)

        loadPlayerTicks()

        // Registramos el Trait (Es obligatorio hacerlo en el onEnable antes de crear NPC)
        if (server.pluginManager.isPluginEnabled("Citizens")) {
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(EnemyStatsTrait::class.java)
            )

            Bukkit.getScheduler().runTaskLater(this, Runnable {
                npcTest()
            }, 100L) // 100 ticks = 5 segundos
        }
    }

    override fun onDisable() {
        npcClearList()
    }

    private fun npcClearList() {
        logger.info("Iniciando limpieza masiva de NPCs temporales y NameDisplays...")

        // 1. Eliminamos de forma ordenada cada enemigo de nuestra lista activa
        // Usamos un iterador o una copia (.toTypedArray) para evitar errores de modificación concurrente
        EnemyObject.all.forEach { (uuid, _) ->
            EnemyObject.remove(uuid) // Esto borra el TextDisplay, des-spawnea el NPC y lo quita de Citizens
        }

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



        Bukkit.getScheduler().runTaskLater(this, Runnable {

            // 5. ¡Activamos el NPC! Aquí adentro se ejecuta el npc.spawn(), se rellenan los PDC y arranca la IA
            enemigoPrueba.iniciar()

            val entityReal = npc.entity as? LivingEntity ?: return@Runnable
            val pdc = entityReal.persistentDataContainer

            // INYECCIÓN DE TUS PDCs COMPATIBLES (La única fuente de verdad de tu plugin)
            pdc.set(Keys.CURRENT_HP, PersistentDataType.DOUBLE, 100.0)
            pdc.set(Keys.MAX_HP, PersistentDataType.DOUBLE, 100.0)
            // Podés meter tus PDCs de Blueprints o Materiales acá mismo...

            // Lo agregamos a tu lista global de limpieza del onDisable
            EnemyObject.register(enemigoPrueba)
            logger.info("¡NPC de prueba inicializado exitosamente en ${coordenadaSpawn.toVector()} con PDCs de vida!")
        }, 5L)


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

object EnemyObject {
    private val map = ConcurrentHashMap<Int, CustomEnemy>()
    fun register(enemy: CustomEnemy) { map[enemy.npc.id] = enemy }
    fun get(id: Int) = map[id]
    fun remove(id: Int) { map[id]?.eliminar(); map.remove(id) }
    val all: Map<Int, CustomEnemy> = map.toMap()
}