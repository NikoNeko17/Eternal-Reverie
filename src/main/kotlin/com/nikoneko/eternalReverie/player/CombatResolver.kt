package com.nikoneko.eternalReverie.player

import com.nikoneko.eternalReverie.EnemyObject
import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.affinities.AffinityMarkManager
import com.nikoneko.eternalReverie.affinities.MarkRegistry
import com.nikoneko.eternalReverie.remnants.RemnantSpecialEffectHandler
import com.nikoneko.eternalReverie.weapons.Affinity
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.random.Random

object CombatResolver {

    fun resolveHit(
        attacker: LivingEntity,
        victim: LivingEntity,
        rawDamage: Double,
        attackerEquipment: PlayerStats.EquipmentStats,
        weaponAffinities: List<Pair<Affinity, Double>>,
        plugin: EternalReverie
    ): Double {

        // ── Evasión (Poison Tamer y futuros Vestigios de AttackEvasion) ───────
        // Evaluada antes de cualquier cálculo — si evade, el hit no ocurre.
        if (victim is Player && RemnantSpecialEffectHandler.shouldEvadeAttack(victim)) {
            victim.world.playSound(victim.location, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f)
            DamageIndicator.spawnEvade(plugin, victim)
            return 0.0
        }

        // ── Cálculo de daño base ───────────────────────────────────────────────
        val victimDefense = PlayerStats.computeEquipmentStats(victim).defense
        val victimHealth  = PlayerStats.getCurrentHp(victim)

        val damageMitigation = 100.0 / (100.0 + victimDefense)
        val isCrit = if (Random.nextDouble() <= attackerEquipment.critChance) 1 else 0

        var finalDamage = (rawDamage
            * (1 + attackerEquipment.strengthMultiplier)
            * (1 + attackerEquipment.critDamageMultiplier * isCrit)) * damageMitigation

        // ── Fragilidad: víctima recibe daño aumentado ─────────────────────────
        if (AffinityMarkManager.hasMark(victim, Affinity.FRAGILIDAD)) {
            MarkRegistry.configs[Affinity.FRAGILIDAD]?.let {
                finalDamage *= (1.0 + it.effectValue)
            }
        }

        // ── Bonus de afinidad del atacante (Poison Tamer y futuros) ──────────
        // Suma daño extra basado en el peso de la afinidad del arma.
        if (attacker is Player) {
            finalDamage += RemnantSpecialEffectHandler.getAffinityDamageBonus(
                attacker, finalDamage, weaponAffinities
            )
        }

        // ── Aplicar daño ──────────────────────────────────────────────────────
        PlayerStats.setCurrentHp(victim, victimHealth - finalDamage)

        // ── Sangre: robo de vida al atacante ──────────────────────────────────
        if (AffinityMarkManager.hasMark(victim, Affinity.SANGRE)) {
            MarkRegistry.configs[Affinity.SANGRE]?.let { config ->
                val healAmount = finalDamage * config.effectValue
                val attackerHp = PlayerStats.getCurrentHp(attacker)
                val attackerMaxHp = PlayerStats.getMaxHp(attacker)
                PlayerStats.setCurrentHp(attacker, (attackerHp + healAmount).coerceAtMost(attackerMaxHp))
            }
        }

        // ── Proc de marcas de afinidad ────────────────────────────────────────
        AffinityMarkManager.onHit(
            attacker       = attacker,
            target         = victim,
            weaponAffinities = weaponAffinities,
            hitDamage      = finalDamage
        )

        // ── Feedback visual y sonoro ──────────────────────────────────────────
        val kb = victim.location.toVector().subtract(attacker.location.toVector())
            .normalize().multiply(0.4).setY(0.18)
        victim.velocity = kb

        victim.world.playSound(victim.location, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f)
        DamageIndicator.spawn(plugin, victim, finalDamage, isCrit == 1)

        // ── NPC: actualizar HP y eliminar si murió ────────────────────────────
        val npc = CitizensAPI.getNPCRegistry().getNPC(victim)
        if (npc != null) {
            val enemy = EnemyObject.get(npc.id)!!
            enemy.stats.currentHp -= finalDamage
            if (PlayerStats.getCurrentHp(victim) <= 0.0) {
                EnemyObject.remove(enemy.npc.id)
            }
        }

        return finalDamage
    }
}
