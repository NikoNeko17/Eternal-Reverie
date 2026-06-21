package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.materials.MaterialType
import com.nikoneko.eternalReverie.player.PlayerStats.initializeIfAbsent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.SkinTrait
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Display
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID
import kotlin.jvm.java
import kotlin.random.Random

class CustomEnemy(
    val npc: NPC,
    val spawnLocation: Location,
    val plugin: EternalReverie,
    val rarity: com.nikoneko.eternalReverie.items.Rarity = com.nikoneko.eternalReverie.items.Rarity.COMMON
) {
    var currentTarget: Player? = null
    private var combatTask: BukkitRunnable? = null
    private lateinit var customName: String
    private var nameDisplay: TextDisplay? = null
    private var lastAttackTime: Long = 0
    private var navCheckCooldown = 0

    // Stats sin arma equipada (combate a puño limpio): attackSpeed vanilla = 4.0
    companion object {
        private const val UNARMED_ATTACK_SPEED = 4.0
        private const val UNARMED_DAMAGE = 8.0
    }


    fun iniciar() {
        // 1. Asignamos la skin ANTES de spawnear: SkinTrait.setSkinName() fuerza un
        // respawn si el NPC ya está spawneado, lo cual destruye la entidad y con ella
        // cualquier passenger montado (el TextDisplay de vida). Asignándola antes,
        // el primer spawn ya sale con la skin correcta, sin respawn intermedio.
        val skinTrait = npc.getOrAddTrait(SkinTrait::class.java)
        customName = plugin.npcNameList.random()
        skinTrait.skinName = customName

        // 2. Spawneamos el NPC (ya con la skin asignada)
        npc.spawn(spawnLocation)
        val entity = npc.entity as? LivingEntity ?: return

        // 3. Creamos la barra de vida flotante moderna (TextDisplay)
        nameDisplay = spawnLocation.world.spawn(spawnLocation, TextDisplay::class.java) { display ->
            display.billboard = Display.Billboard.VERTICAL // Siempre mira al jugador
            display.isShadowed = true
            display.backgroundColor = org.bukkit.Color.fromARGB(0, 0, 0, 0) // Fondo transparente
        }

        initializeIfAbsent(entity)

        npc.isProtected = false

        updateLabel(entity)
        entity.addPassenger(nameDisplay!!)

        // 4. Iniciamos el bucle de IA de Combate
        iniciarBucleCombate()
    }

    // Lee attackSpeed/daño desde el arma equipada en mano principal (mismo PDC
    // que usan los jugadores: Keys.BLUEPRINT_ID + Keys.MATERIALS), o usa stats
    // fijas de combate a puño limpio si el NPC no tiene nada equipado.
    private fun computeEnemyCombatStats(entity: LivingEntity): Pair<Double, Double> {
        val weaponItem = entity.equipment?.itemInMainHand
        val pdc = weaponItem?.itemMeta?.persistentDataContainer

        val blueprintId = pdc?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
        val materialIds = pdc?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

        if (blueprintId != null && materialIds != null) {
            val blueprint = runCatching { BlueprintRegistry.get(blueprintId) }.getOrNull()
            if (blueprint != null) {
                val materials = materialIds.mapNotNull {
                    runCatching { MaterialType.valueOf(it) }.getOrNull()
                }
                val stats = CraftingCalculator.computeWeaponStatsPublic(blueprint, materials)
                return stats.attackSpeed to stats.damage
            }
        }

        return UNARMED_ATTACK_SPEED to UNARMED_DAMAGE
    }

    private fun iniciarBucleCombate() {
        combatTask = object : BukkitRunnable() {
            override fun run() {
                if (!npc.isSpawned || npc.entity == null || npc.entity!!.isDead) {
                    eliminar()
                    cancel()
                    return
                }

                val entity = npc.entity as LivingEntity
                updateLabel(entity)

                val (attackSpeed, attackDamage) = computeEnemyCombatStats(entity)
                val attackReach = entity.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0

                // 1. Si no hay objetivo, buscamos al jugador real más cercano
                if (currentTarget == null || !currentTarget!!.isOnline || currentTarget!!.isDead) {
                    val cercano = entity.location.world.getNearbyEntities(entity.location, 15.0, 15.0, 15.0)
                        .filterIsInstance<Player>()
                        .firstOrNull { jugador -> jugador.uniqueId != entity.uniqueId && !jugador.hasMetadata("NPC") }

                    if (cercano != null) {
                        currentTarget = cercano
                    } else {
                        if (npc.navigator.isNavigating) npc.navigator.cancelNavigation()
                        return
                    }
                }

                val target = currentTarget!!

                navCheckCooldown++
                if (navCheckCooldown >= 10) {
                    navCheckCooldown = 0
                    if (!npc.navigator.isNavigating || npc.navigator.entityTarget != target) {
                        npc.navigator.localParameters.speedModifier(1.5f)
                        npc.navigator.setTarget(target, true)
                    }
                }

                // Mirar siempre al objetivo: el navigator solo orienta la cabeza hacia
                // donde se MUEVE, así que dentro del rango de ataque (parado, sin
                // moverse) nunca giraba hacia el jugador sin esto.
                npc.faceLocation(target.location)

                // 4. LÓGICA DE ALCANCE Y VELOCIDAD DE ATAQUE
                val distancia = entity.location.distance(target.location)
                if (distancia <= attackReach) {
                    val tiempoActual = System.currentTimeMillis()
                    val ticksEnMilis = 1000.0 / attackSpeed

                    if (tiempoActual - lastAttackTime >= ticksEnMilis) {
                        triggerHit(entity, target, attackDamage)
                        lastAttackTime = tiempoActual
                    }
                }
            }
        }
        combatTask!!.runTaskTimer(plugin, 0L, 1L) // Corre cada 1 tick
    }


    private fun triggerHit(attacker: LivingEntity, victim: Player, damage: Double) {
        // Ejecuta el movimiento de brazo visual del NPC
        attacker.swingMainHand()
        // Infligimos el daño a través del evento nativo para que tu DamageSystemListener lo procese.
        // Al pasarle 'attacker' como damager, tu sistema podrá leer los blueprints/materiales del
        // arma del NPC si tiene una equipada (vía computeEnemyCombatStats), o usar el fallback
        // sin arma; el valor pasado acá a damage() es solo el trigger del evento, el daño REAL
        // final lo recalcula CombatResolver leyendo el arma del attacker en PlayerListeners.
        victim.damage(damage, attacker)
    }

    private fun updateLabel(entity: LivingEntity) {
        val display = nameDisplay ?: return

        val currentHealth = entity.persistentDataContainer.get(Keys.CURRENT_HP, PersistentDataType.DOUBLE) ?: return
        val maxHealth = entity.persistentDataContainer.get(Keys.MAX_HP, PersistentDataType.DOUBLE) ?: return

        // Ajustamos la altura sutilmente hacia arriba de la cabeza
        display.transformation = Transformation(
            Vector3f(0f, 0.5f, 0f), // Desplazamiento en Y (arriba de la cabeza)
            AxisAngle4f(0f, 0f, 0f, 1f),
            Vector3f(1f, 1f, 1f),  // Escala
            AxisAngle4f(0f, 0f, 0f, 1f)
        )
        // Formateamos el texto de forma dinámica con colores limpios
        display.text(Component.text("§c${customName} §7[§a${currentHealth.toInt()}§7/§a${maxHealth.toInt()} ❦§7]"))
    }

    fun attack(cantidad: Double, entity: LivingEntity) {
        val currentHealth = entity.persistentDataContainer.get(Keys.CURRENT_HP, PersistentDataType.DOUBLE)!!

        entity.persistentDataContainer.set(Keys.CURRENT_HP, PersistentDataType.DOUBLE, (currentHealth - cantidad).coerceAtLeast(0.0))
        updateLabel(entity)
        if (currentHealth <= 0.0) {
            eliminar()
        }
    }

    fun eliminar() {
        // Genera el loot ANTES de despawnear, usando la última ubicación conocida
        // de la entidad para dropear los ítems ahí mismo.
        val deathLocation = (npc.entity as? LivingEntity)?.location ?: spawnLocation
        dropLoot(deathLocation)

        combatTask?.cancel()
        nameDisplay?.remove()
        if (npc.isSpawned) npc.despawn()
        net.citizensnpcs.api.CitizensAPI.getNPCRegistry().deregister(npc)
    }

    private fun dropLoot(location: Location) {
        val world = location.world ?: return

        // Chatarra: drop garantizado en cada muerte, independiente de lo demás.
        val currencyAmount = com.nikoneko.eternalReverie.loot.LootGenerator.rollCurrency(rarity)
        if (currencyAmount > 0) {
            world.dropItemNaturally(
                location,
                com.nikoneko.eternalReverie.economy.CurrencyItem.create(currencyAmount)
            )
        }

        when (val result = com.nikoneko.eternalReverie.loot.LootGenerator.rollLoot(rarity)) {
            is com.nikoneko.eternalReverie.loot.LootGenerator.LootResult.Nothing -> {
                // Sin drop, no se hace nada.
            }
            is com.nikoneko.eternalReverie.loot.LootGenerator.LootResult.Materials -> {
                for (item in result.items) {
                    world.dropItemNaturally(location, item)
                }
            }
            is com.nikoneko.eternalReverie.loot.LootGenerator.LootResult.CraftedItem -> {
                world.dropItemNaturally(location, result.item)
            }
        }
    }
}
