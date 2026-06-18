package com.nikoneko.eternalReverie.command

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.items.BlueprintRegistry
import com.nikoneko.eternalReverie.items.ItemFactory
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BlueprintCommand(val plugin: EternalReverie) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player ||
            !sender.isOp ||
            args.isEmpty()
            ) return false

        val id = args[0].uppercase()
        val amount = args.getOrNull(1)?.toInt() ?: 1
        val item = ItemFactory.createBlueprintItem(id, amount) ?: return false
        sender.inventory.addItem(item)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): List<String?>? {
        return when (args?.size) {
            1 -> BlueprintRegistry.all().map { it.id }.filter { it.startsWith(args[0], true) }
            2 -> listOf("[amount]")
            else -> emptyList()
        }
    }
}