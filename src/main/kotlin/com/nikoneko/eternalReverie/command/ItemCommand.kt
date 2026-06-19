package com.nikoneko.eternalReverie.command

import com.nikoneko.eternalReverie.EternalReverie
import com.nikoneko.eternalReverie.items.MaterialType
import com.nikoneko.eternalReverie.items.ItemFactory
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ItemCommand(val plugin: EternalReverie) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player ||
            !sender.isOp ||
            args.isEmpty()
            ) return false

        val id = args[0]
        val amount = if (!args[1].isEmpty()) args[1].toInt() else 1
        val item = ItemFactory.createMaterialItem(MaterialType.valueOf(id), amount)
        sender.inventory.addItem(item)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        return when (args.size) {
            1 -> MaterialType.entries.map { it.name }.filter { it.contains(args[0], true) }
            2 -> listOf("[amount]")
            else -> emptyList()
        }
    }
}