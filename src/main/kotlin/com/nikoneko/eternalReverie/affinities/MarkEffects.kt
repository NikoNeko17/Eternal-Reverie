package com.nikoneko.eternalReverie.affinities

import com.destroystokyo.paper.ParticleBuilder
import com.nikoneko.eternalReverie.EnemyObject
import com.nikoneko.eternalReverie.player.PlayerStats
import com.nikoneko.eternalReverie.weapons.Affinity
import com.nikoneko.eternalReverie.weapons.firearms.AttackSpeedDebuff
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity

/**
 * Aplica el efecto periódico (1 vez por segundo, vía AffinityMarkManager.tickAll)
 * de cada Marca activa. Sin stacks: cada Affinity tiene un único comportamiento
 * fijo mientras la Marca esté activa, definido acá.
 *
 * `source` es quien aplicó la Marca (el atacante original), relevante para
 * Sangre (Robo de Vida) y Electricidad (Drenaje), que benefician al atacante.
 * Puede ser null si el atacante salió del rango de entidades trackeadas.
 */
object MarkEffects {

    fun applyTick(target: LivingEntity, source: LivingEntity?, mark: AffinityMark, config: MarkConfig) {
        val mitigation = AffinityMarkManager.computeArmorAffinityMitigation(target, mark.affinity)
        val effectiveMultiplier = (1.0 - mitigation).coerceIn(0.0, 1.0)

        when (mark.affinity) {
            Affinity.SANGRE -> applyBleed(target, source, config, effectiveMultiplier)
            Affinity.FUEGO -> applyBurn(target, mark, config, effectiveMultiplier)
            Affinity.HIELO -> applyFreeze(target, config, effectiveMultiplier)
            Affinity.ELECTRICIDAD -> applyDrain(target, source, config, effectiveMultiplier)
            Affinity.VENENO -> applyPoison(target, config, effectiveMultiplier)
            Affinity.ATADURA -> applyAnchor(target, config, effectiveMultiplier)
            Affinity.FRAGILIDAD -> { /* Exposición: leída directamente en CombatResolver al calcular daño, no en el tick */ }
        }
    }

    /** Llamar al expirar/remover una Marca, para revertir efectos persistentes (Hielo/Veneno/Atadura). */
    fun onMarkExpire(target: LivingEntity, affinity: Affinity) {
        when (affinity) {
            Affinity.HIELO, Affinity.VENENO, Affinity.ATADURA -> MovementSpeedModifier.restore(target, affinity)
            else -> {}
        }
    }

    // --- Sangre: Robo de Vida. El ATACANTE se cura un % del daño infligido mientras
    // la Marca esté activa en la víctima. No hace daño extra por sí misma; el robo
    // ocurre en CombatResolver al momento del golpe (ver nota abajo), acá solo
    // mantenemos viva la referencia por si se necesita un tick visual a futuro. ---
    private fun applyBleed(target: LivingEntity, source: LivingEntity?, config: MarkConfig, mult: Double) {
        // El robo real se resuelve en CombatResolver (lee hasMark(target, SANGRE) y
        // cura al atacante un % del daño de ESE golpe). Acá no hay tick periódico real.
        ParticleBuilder(Particle.BLOCK_CRUMBLE)
            .data(Material.REDSTONE_BLOCK.createBlockData())
            .location(target.location.add(0.0, 1.0, 0.0))
            .count(50)
            .extra(0.0)
            .offset(0.25, 0.5, 0.25)
            .allPlayers()
            .spawn()
    }

    // --- Fuego: DoT proporcional al daño del golpe que generó/refrescó la Marca ---
    private fun applyBurn(
        target: LivingEntity,
        mark: AffinityMark,
        config: MarkConfig,
        mult: Double
    ) {
        val rawDamage = mark.sourceHitDamage * config.effectValue
        val mitigatedDamage = rawDamage * mult

        val currentHp = PlayerStats.getCurrentHp(target)
        PlayerStats.setCurrentHp(target, (currentHp - mitigatedDamage).coerceAtLeast(0.0))

        val npc = CitizensAPI.getNPCRegistry().getNPC(target)
        if (npc != null) {
            val enemy = EnemyObject.get(npc.id)!!
            enemy.stats.currentHp -= mitigatedDamage
        }

        ParticleBuilder(Particle.FLAME)
            .location(target.location.add(0.0, 1.0, 0.0))
            .count(50)
            .extra(0.0)
            .offset(0.25, 0.5, 0.25)
            .allPlayers()
            .spawn()
    }

    // --- Hielo: frenazo fuerte y corto, movimiento + velocidad de ataque ---
    private fun applyFreeze(target: LivingEntity, config: MarkConfig, mult: Double) {
        val movementReduction = config.effectValue * mult         // -40% base
        val attackSpeedReduction = (config.effectValue * 0.75) * mult // -30% base (3/4 del valor de movimiento)

        MovementSpeedModifier.applyReduction(target, Affinity.HIELO, movementReduction)
        AttackSpeedDebuff.apply(target, 1.0 - attackSpeedReduction, durationMillis = 1200L) // se refresca cada tick mientras la Marca viva
    }

    // --- Electricidad: Drenaje. Roba % de la regen. De Stamina del enemigo hacia el atacante ---
    private fun applyDrain(target: LivingEntity, source: LivingEntity?, config: MarkConfig, mult: Double) {
        if (source == null) return

        // "Robar regeneración" = el objetivo no regenera Stamina este tick (penalización
        // completa de regen, no solo reducción), y el atacante recibe ese % como Stamina extra.
        val targetMaxStamina = PlayerStats.getMaxStamina(target)
        val stolenAmount = targetMaxStamina * config.effectValue * mult

        val targetCurrent = PlayerStats.getCurrentStamina(target)
        PlayerStats.setCurrentStamina(target, (targetCurrent - stolenAmount).coerceAtLeast(0.0))

        val sourceCurrent = PlayerStats.getCurrentStamina(source)
        PlayerStats.setCurrentStamina(source, sourceCurrent + stolenAmount)

        ParticleBuilder(Particle.ELECTRIC_SPARK)
            .location(target.location.add(0.0, 1.0, 0.0))
            .count(50)
            .extra(0.0)
            .offset(0.25, 0.5, 0.25)
            .allPlayers()
            .spawn()
    }

    // --- Veneno: progresivo y prolongado, movimiento leve + regen. HP/Stamina golpeada ---
    private fun applyPoison(target: LivingEntity, config: MarkConfig, mult: Double) {
        val movementReduction = config.effectValue * mult // -15% base

        MovementSpeedModifier.applyReduction(target, Affinity.VENENO, movementReduction)

        // -25% de regen.: implementado como un pequeño drenaje directo de Stamina,
        // ya que no existe (todavía) un sistema de regeneración pasiva que debuffear.
        val regenPenalty = config.effectValue * 1.67 * mult // ~25% base (0.15 * 1.67 ≈ 0.25)
        val targetMaxStamina = PlayerStats.getMaxStamina(target)
        val targetCurrent = PlayerStats.getCurrentStamina(target)
        PlayerStats.setCurrentStamina(target, (targetCurrent - targetMaxStamina * regenPenalty * 0.05).coerceAtLeast(0.0))
    }

    // --- Atadura: Anclaje. Inmoviliza por completo (Movilidad a 0). ---
    // El bloqueo de uso de ítems/habilidades se maneja del lado del usuario
    // (return temprano en PlayerListener al detectar AffinityMarkManager.hasMark(player, ATADURA)).
    private fun applyAnchor(target: LivingEntity, config: MarkConfig, mult: Double) {
        MovementSpeedModifier.applyReduction(target, Affinity.ATADURA, 1.0) // inmovilizado total, binario
    }
}
