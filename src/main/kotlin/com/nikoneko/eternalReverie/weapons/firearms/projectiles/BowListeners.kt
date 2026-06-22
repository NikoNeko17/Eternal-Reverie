package com.nikoneko.eternalReverie.weapons.firearms.projectiles

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.crafting.CraftingCalculator
import com.nikoneko.eternalReverie.crafting.MaterialType
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.player.CombatResolver
import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import com.nikoneko.eternalReverie.weapons.WeaponFamily
import com.nikoneko.eternalReverie.weapons.firearms.WeaponStateManager
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * Maneja los 3 arcos, todos usando Arrow real de Bukkit (física vanilla nativa):
 *  - Arco Corto: dispara inmediato al click derecho (sin cargar), peor que el Largo.
 *  - Arco Largo: mecánica de carga vanilla real (sostener click), gravedad normal.
 *  - Arco Compuesto: se tensa igual que el Largo, daño similar al Corto (mejor),
 *    pero con gravedad reducida al 10% (trayectoria casi recta).
 *
 * Los 3 comparten el mismo tope de alcance: min(Keys.REACH del arma,
 * ENTITY_INTERACTION_RANGE × 10), y la flecha se autodestruye al llegar a ese límite
 * (chequeado en un tick propio en vez de esperar a ProjectileHitEvent, ya que una
 * flecha que no impacta nada nunca dispararía ese evento por sí sola).
 */
class BowListeners(private val plugin: EternalReverie) : Listener {

    companion object {
        private const val COMPOUND_GRAVITY_FACTOR = 0.10 // 10% de la gravedad normal
    }

    // --- Arco Corto: disparo inmediato, sin mecánica de carga ---
    @EventHandler
    fun onShortBowFire(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return

        val player = event.player

        // Atadura: el jugador anclado no puede disparar mientras la Marca esté activa.
        if (com.nikoneko.eternalReverie.affinities.AffinityMarkManager.hasMark(player, Affinity.ATADURA)) {
            return
        }

        // Exhausto: sin stamina suficiente, no puede disparar.
        if (com.nikoneko.eternalReverie.player.StaminaManager.isExhausted(player)) {
            return
        }

        val item = event.item ?: return
        val itemMeta = item.itemMeta ?: return
        val pdc = itemMeta.persistentDataContainer

        val familyStr = pdc.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: return
        val family = runCatching { WeaponFamily.valueOf(familyStr) }.getOrNull() ?: return
        if (family != WeaponFamily.ARCO_CORTO) return

        val attackSpeed = pdc.get(Keys.ATTACK_SPEED, PersistentDataType.DOUBLE) ?: return
        val weaponUuidStr = pdc.get(Keys.INSTANCE_UUID, PersistentDataType.STRING) ?: return
        val weaponUuid = UUID.fromString(weaponUuidStr)

        if (!WeaponStateManager.canShoot(player, weaponUuid, attackSpeed)) return

        com.nikoneko.eternalReverie.player.StaminaManager.tryConsumeForAttack(player, family)

        val (computedStats, _) = computeFiredWeaponStats(player)
        val maxDistance = effectiveMaxDistance(player, pdc.get(Keys.REACH, PersistentDataType.DOUBLE))

        val arrow = player.world.spawnArrow(
            player.eyeLocation,
            player.eyeLocation.direction,
            1.4f,  // velocidad menor que el Largo/Compuesto (peor, según lo pedido)
            6f     // dispersión leve
        )
        arrow.shooter = player
        configureCustomArrow(arrow, computedStats.damage, computedStats.affinities, maxDistance)

        WeaponStateManager.trigger(player, weaponUuid)
    }

    // --- Arco Largo y Arco Compuesto: mecánica de carga vanilla real ---
    @EventHandler
    fun onChargedBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return

        // Atadura: el jugador anclado no puede disparar mientras la Marca esté activa.
        if (com.nikoneko.eternalReverie.affinities.AffinityMarkManager.hasMark(player, Affinity.ATADURA)) {
            event.isCancelled = true
            return
        }

        // Exhausto: sin stamina suficiente, no puede disparar.
        if (com.nikoneko.eternalReverie.player.StaminaManager.isExhausted(player)) {
            event.isCancelled = true
            return
        }

        val bow = event.bow ?: return
        val pdc = bow.itemMeta?.persistentDataContainer ?: return

        val familyStr = pdc.get(Keys.WEAPON_FAMILY, PersistentDataType.STRING) ?: return
        val family = runCatching { WeaponFamily.valueOf(familyStr) }.getOrNull() ?: return
        if (family != WeaponFamily.ARCO_LARGO && family != WeaponFamily.ARCO_COMPUESTO) return

        com.nikoneko.eternalReverie.player.StaminaManager.tryConsumeForAttack(player, family)

        val projectile = event.projectile as? Arrow ?: return
        val maxDistance = effectiveMaxDistance(player, pdc.get(Keys.REACH, PersistentDataType.DOUBLE))

        val (computedStats, _) = computeFiredWeaponStats(player)

        // event.force ya viene de la carga vanilla (0.0-1.0); lo dejamos intacto
        // para no romper el feeling del arco cargado, solo agregamos nuestros datos.
        configureCustomArrow(projectile, computedStats.damage, computedStats.affinities, maxDistance)

        if (family == WeaponFamily.ARCO_COMPUESTO) {
            applyReducedGravity(projectile)
        }
    }

    // --- Impacto: resuelve daño custom para los 3 arcos con Arrow real ---
    @EventHandler
    fun onArrowHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? Arrow ?: return
        resolveArrowImpact(arrow, hitEntityOverride = event.hitEntity as? LivingEntity)
    }

    // ============================================================
    //  GRAVEDAD REDUCIDA (Arco Compuesto)
    // ============================================================

    // Bukkit no permite un % de gravedad directo (solo on/off vía setGravity).
    // Desactivamos la gravedad nativa y reaplicamos manualmente solo el 10% de
    // la caída estándar de una flecha (~0.05 blocks/tick) cada tick, dando una
    // trayectoria casi recta sin reescribir toda la física de vuelo.
    private fun applyReducedGravity(arrow: Arrow) {
        arrow.setGravity(false)
        val vanillaArrowGravityPerTick = 0.05 // aproximación estándar de caída de flecha vanilla

        object : BukkitRunnable() {
            override fun run() {
                if (!arrow.isValid || arrow.isDead) {
                    cancel()
                    return
                }
                val velocity = arrow.velocity
                velocity.y -= vanillaArrowGravityPerTick * COMPOUND_GRAVITY_FACTOR
                arrow.velocity = velocity

                // Chequeo de alcance manual: con gravedad nativa apagada, el tick
                // normal de la flecha sigue corriendo, así que esto solo refuerza
                // el límite junto al chequeo de checkArrowDistance().
                checkArrowDistance(arrow)
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    // ============================================================
    //  LÍMITE DE ALCANCE COMPARTIDO (los 3 arcos)
    // ============================================================

    private fun effectiveMaxDistance(player: Player, weaponReach: Double?): Double {
        val baseReach = weaponReach ?: 20.0
        val interactionRange = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue ?: 3.0
        val safetyCap = interactionRange * 10.0
        return baseReach.coerceAtMost(safetyCap)
    }

    // Tarea periódica liviana por flecha: se autodestruye si viajó más que su
    // maxDistance guardado, sin esperar a que impacte algo (una flecha que vuela
    // al vacío nunca dispararía ProjectileHitEvent por sí sola).
    private fun checkArrowDistance(arrow: Arrow) {
        val pdc = arrow.persistentDataContainer
        val maxDistance = pdc.get(RealArrowKeys.ARROW_MAX_DISTANCE, PersistentDataType.DOUBLE) ?: return
        val travelled = arrow.origin?.distance(arrow.location) ?: 0.0

        if (travelled >= maxDistance) {
            arrow.remove()
        }
    }

    // ============================================================
    //  RESOLUCIÓN DE DAÑO (compartida por los 3 arcos)
    // ============================================================

    private fun resolveArrowImpact(arrow: Arrow, hitEntityOverride: LivingEntity?) {
        val pdc = arrow.persistentDataContainer
        if (!pdc.has(RealArrowKeys.ARROW_DAMAGE, PersistentDataType.DOUBLE)) return // no es flecha custom

        val target = hitEntityOverride ?: run {
            arrow.remove()
            return
        }

        val shooter = arrow.shooter as? LivingEntity ?: run {
            arrow.remove()
            return
        }

        val baseDamage = pdc.get(RealArrowKeys.ARROW_DAMAGE, PersistentDataType.DOUBLE) ?: 0.0
        val maxDistance = pdc.get(RealArrowKeys.ARROW_MAX_DISTANCE, PersistentDataType.DOUBLE) ?: 1.0
        val affinitiesStr = pdc.get(RealArrowKeys.ARROW_AFFINITIES, PersistentDataType.STRING) ?: ""
        val affinities = deserializeAffinities(affinitiesStr)

        val travelled = arrow.origin?.distance(arrow.location) ?: 0.0
        val distanceMultiplier = (1.0 - (travelled / maxDistance)).coerceIn(0.30, 1.0)
        val finalRawDamage = baseDamage * distanceMultiplier

        val shooterEquipment = PlayerStats.computeEquipmentStats(shooter)

        CombatResolver.resolveHit(
            attacker = shooter,
            victim = target,
            rawDamage = finalRawDamage,
            attackerEquipment = shooterEquipment,
            weaponAffinities = affinities
        )

        // No usamos target.health = 0.0 directo para evitar bugs visuales;
        // si el HP custom llegó a 0, forzamos un daño vanilla enorme para que
        // Minecraft dispare correctamente animación/sonido/drops de muerte.
        if (PlayerStats.getCurrentHp(target) <= 0.0) {
            target.damage(9999.0, shooter)
        } else {
            target.damage(0.0, shooter)
        }

        arrow.remove()
    }

    // ============================================================
    //  HELPERS
    // ============================================================

    private fun configureCustomArrow(
        arrow: Arrow,
        damage: Double,
        affinities: List<Pair<Affinity, Double>>,
        maxDistance: Double
    ) {
        val pdc = arrow.persistentDataContainer
        pdc.set(RealArrowKeys.ARROW_DAMAGE, PersistentDataType.DOUBLE, damage)
        pdc.set(RealArrowKeys.ARROW_AFFINITIES, PersistentDataType.STRING, serializeAffinities(affinities))
        pdc.set(RealArrowKeys.ARROW_MAX_DISTANCE, PersistentDataType.DOUBLE, maxDistance)
        arrow.isCritical = false // el crítico ya lo maneja CombatResolver, no vanilla
        arrow.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED // no se puede recoger del piso (loot custom es otra cosa)
    }

    private fun computeFiredWeaponStats(
        player: Player
    ): Pair<CraftingCalculator.ComputedWeaponStats, PlayerStats.EquipmentStats> {
        val firedItem = player.inventory.itemInMainHand
        val firedMeta = firedItem.itemMeta
        val firedBlueprintId = firedMeta?.persistentDataContainer?.get(Keys.BLUEPRINT_ID, PersistentDataType.STRING)
        val firedMaterialIds = firedMeta?.persistentDataContainer?.get(Keys.MATERIALS, PersistentDataType.LIST.strings())

        val stats = if (firedBlueprintId != null && firedMaterialIds != null) {
            val parsedBlueprint = runCatching { BlueprintRegistry.get(firedBlueprintId) }.getOrNull()
            val parsedMaterials = firedMaterialIds.mapNotNull {
                runCatching { MaterialType.valueOf(it) }.getOrNull()
            }
            if (parsedBlueprint != null) {
                CraftingCalculator.computeWeaponStatsPublic(parsedBlueprint, parsedMaterials)
            } else {
                CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList())
            }
        } else {
            CraftingCalculator.ComputedWeaponStats(8.0, 4.0, 0.0, emptyList())
        }

        return Pair(stats, PlayerStats.computeEquipmentStats(player))
    }

    // Serialización simple "AFINIDAD:peso,AFINIDAD:peso" ya que PDC no tiene
    // un tipo compuesto directo para List<Pair<Affinity,Double>>.
    private fun serializeAffinities(affinities: List<Pair<Affinity, Double>>): String =
        affinities.joinToString(",") { "${it.first.name}:${it.second}" }

    private fun deserializeAffinities(raw: String): List<Pair<Affinity, Double>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val affinity = runCatching { Affinity.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val weight = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            affinity to weight
        }
    }
}
