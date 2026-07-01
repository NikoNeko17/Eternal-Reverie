package com.nikoneko.eternalReverie.remnants.athanor

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AthanorCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("Este comando solo puede usarse en el juego.")
            return true
        }

        if (player.isOp) {
            val item = EssenceCellManager.createEmptyCell()
            EssenceCellManager.addEssenceAndCheck(item, 300, "zone_13")
            player.inventory.addItem(item)
        }

        val holder = AthanorGuiHolder(player)
        player.openInventory(holder.inventory)
        return true
    }
}
