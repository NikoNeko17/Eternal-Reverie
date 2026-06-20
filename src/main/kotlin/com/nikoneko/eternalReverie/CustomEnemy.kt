package com.nikoneko.eternalReverie

import com.nikoneko.eternalReverie.items.Keys
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
    val plugin: EternalReverie
) {
    var currentTarget: Player? = null
    private var combatTask: BukkitRunnable? = null
    private lateinit var customName: String
    private var nameDisplay: TextDisplay? = null
    private var lastAttackTime: Long = 0
    private var navCheckCooldown = 0


    fun iniciar() {
        // 1. Spawneamos el NPC

        npc.spawn(spawnLocation)
        val entity = npc.entity as? LivingEntity ?: return

        // 2. Creamos la barra de vida flotante moderna (TextDisplay)
        nameDisplay = spawnLocation.world.spawn(spawnLocation, TextDisplay::class.java) { display ->
            display.billboard = Display.Billboard.VERTICAL // Siempre mira al jugador
            display.isShadowed = true
            display.backgroundColor = org.bukkit.Color.fromARGB(0, 0, 0, 0) // Fondo transparente
        }

        initializeIfAbsent(entity)

        npc.isProtected = false


        val skinTrait = npc.getOrAddTrait(SkinTrait::class.java)
        customName = plugin.npcNameList.random()
        skinTrait.skinName = customName



        updateLabel(entity)
        entity.addPassenger(nameDisplay!!)

        // 3. Iniciamos el bucle de IA de Combate
        iniciarBucleCombate()
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

                val attackSpeed = entity.getAttribute(Attribute.ATTACK_SPEED)?.value ?: 4.0
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



                // 4. LÓGICA DE ALCANCE Y VELOCIDAD DE ATAQUE (Igual que antes)
                val distancia = entity.location.distance(target.location)
                if (distancia <= attackReach) {
                    val tiempoActual = System.currentTimeMillis()
                    val ticksEnMilis = attackSpeed * 50

                    if (tiempoActual - lastAttackTime >= ticksEnMilis) {
                        triggerHit(entity, target)
                        lastAttackTime = tiempoActual
                    }
                }
            }
        }
        combatTask!!.runTaskTimer(plugin, 0L, 1L) // Corre cada 1 tick
    }


    private fun triggerHit(attacker: LivingEntity, victim: Player) {
        // Ejecuta el movimiento de brazo visual del NPC
        attacker.swingMainHand()
        // Infligimos el daño a través del evento nativo para que tu DamageSystemListener lo procese
        // Al pasarle 'attacker' como damager, tu sistema podrá leer los blueprints/materiales del arma del NPC
        victim.damage(1.0, attacker)
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
        combatTask?.cancel()
        nameDisplay?.remove()
        if (npc.isSpawned) npc.despawn()
        net.citizensnpcs.api.CitizensAPI.getNPCRegistry().deregister(npc)
    }
}
