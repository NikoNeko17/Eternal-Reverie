package com.nikoneko.eternalReverie.durability

import com.nikoneko.eternalReverie.items.Keys
import com.nikoneko.eternalReverie.items.TextFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import com.nikoneko.eternalReverie.EternalReverie

class DurabilityListener(private val plugin: EternalReverie) : Listener {

    // ============================================================
    //  EVENTOS
    // ============================================================

    // Al cambiar de slot: actualiza el lore del arma que ahora se sostiene
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val item = player.inventory.getItem(event.newSlot) ?: return
        if (!isCustomItem(item)) return

        refreshLore(item)
        player.updateInventory()
    }

    // Al abrir inventario: actualiza todos los ítems custom visibles
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // Actualizar el inventario del jugador completo
        player.inventory.contents.forEach { item ->
            if (item != null && isCustomItem(item)) refreshLore(item)
        }
        player.updateInventory()
    }

    // Al atacar: descuenta durabilidad del arma en mano,
    // y con 25% de probabilidad a una pieza de armadura aleatoria del defensor.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return

        // --- Arma del atacante pierde 1 durabilidad ---
        val weapon = attacker.inventory.itemInMainHand
        if (isCustomItem(weapon)) {
            val broke = decrementDurability(weapon)
            if (broke) {
                attacker.inventory.setItemInMainHand(null)
                attacker.sendMessage(
                    Component.text("Tu arma se ha roto.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                )
            } else {
                refreshLore(weapon)
            }
            attacker.updateInventory()
        }

        // --- Armadura del defensor: una pieza custom aleatoria pierde 1 durabilidad ---
        val defender = event.entity as? Player ?: return
        val armorPieces = listOf(
            defender.inventory.helmet,
            defender.inventory.chestplate,
            defender.inventory.leggings,
            defender.inventory.boots
        )

        val eligibleSlots = armorPieces.indices.filter { i ->
            armorPieces[i] != null && isCustomItem(armorPieces[i]!!)
        }

        if (eligibleSlots.isNotEmpty()) {
            val chosenIndex = eligibleSlots.random()
            val armorPiece = armorPieces[chosenIndex]!!

            val broke = decrementDurability(armorPiece)
            if (broke) {
                when (chosenIndex) {
                    0 -> defender.inventory.helmet = null
                    1 -> defender.inventory.chestplate = null
                    2 -> defender.inventory.leggings = null
                    3 -> defender.inventory.boots = null
                }
                defender.sendMessage(
                    Component.text("Una pieza de tu armadura se ha roto.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                )
            } else {
                refreshLore(armorPiece)
            }
            defender.updateInventory()
        }
    }

    // ============================================================
    //  UTILIDADES
    // ============================================================

    companion object {

        /** Devuelve true si el ítem tiene tag PDC de ítem custom (tiene DURABILITY). */
        fun isCustomItem(item: ItemStack?): Boolean {
            val meta = item?.itemMeta ?: return false
            return meta.persistentDataContainer.has(Keys.DURABILITY, PersistentDataType.INTEGER)
        }

        /**
         * Descuenta 1 de durabilidad custom y actualiza la barra vanilla.
         * Devuelve true si la durabilidad llegó a 0 (el ítem debe destruirse).
         */
        fun decrementDurability(item: ItemStack): Boolean {
            val meta = item.itemMeta ?: return false
            val pdc = meta.persistentDataContainer

            val current = pdc.get(Keys.DURABILITY, PersistentDataType.INTEGER) ?: return false
            val max = pdc.get(Keys.MAX_DURABILITY, PersistentDataType.INTEGER) ?: return false

            val newDurability = (current - 1).coerceAtLeast(0)
            pdc.set(Keys.DURABILITY, PersistentDataType.INTEGER, newDurability)

            // Actualizar barra vanilla: (actual/máximo) × maxVanilla del tipo de ítem
            updateVanillaDurabilityBar(meta as Damageable, item, newDurability, max)

            item.itemMeta = meta
            return newDurability <= 0
        }

        /**
         * Actualiza el lore del ítem para reflejar la durabilidad actual del PDC,
         * y sincroniza la barra vanilla.
         * No modifica las afinidades ni otros campos del lore — solo reemplaza
         * la línea "Durabilidad: X/Y" con los valores actuales.
         */
        fun refreshLore(item: ItemStack) {
            val meta = item.itemMeta ?: return
            val pdc = meta.persistentDataContainer

            val current = pdc.get(Keys.DURABILITY, PersistentDataType.INTEGER) ?: return
            val max = pdc.get(Keys.MAX_DURABILITY, PersistentDataType.INTEGER) ?: return

            val lore = meta.lore() ?: return

            // Buscar y reemplazar la línea de durabilidad por su texto plano
            val durabilityPrefix = "Durabilidad: "
            val updatedLore = lore.map { line ->
                val plain = line.let {
                    // Extraer texto plano para comparar
                    net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(it)
                }
                if (plain.startsWith(durabilityPrefix)) {
                    Component.text(durabilityPrefix, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("$current/$max", NamedTextColor.WHITE))
                } else {
                    line
                }
            }

            meta.lore(updatedLore)

            // Sincronizar barra vanilla
            if (meta is Damageable) {
                updateVanillaDurabilityBar(meta, item, current, max)
            }

            item.itemMeta = meta
        }

        private fun updateVanillaDurabilityBar(
            meta: Damageable,
            item: ItemStack,
            current: Int,
            max: Int
        ) {
            val vanillaMax = item.type.maxDurability.toInt()
            if (vanillaMax <= 0) return

            // damage = vanillaMax - (current/max × vanillaMax)
            // damage 0 = barra llena, damage = vanillaMax = barra vacía
            val pct = current.toDouble() / max.toDouble()
            val damage = (vanillaMax - (pct * vanillaMax)).toInt().coerceIn(0, vanillaMax)
            meta.damage = damage

            // Ocultar la barra vanilla si la durabilidad está llena
            // (para que no aparezca la barra cuando no hace falta)
            meta.isUnbreakable = false
        }
    }
}
