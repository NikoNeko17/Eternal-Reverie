package com.nikoneko.eternalReverie.remnants

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RemnantCommand : CommandExecutor {

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

        val holder = RemnantGuiHolder(player)
        player.openInventory(holder.inventory)
        return true
    }
}
