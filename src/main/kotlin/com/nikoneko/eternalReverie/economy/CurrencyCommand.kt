package com.nikoneko.eternalReverie.economy

import com.nikoneko.eternalReverie.EternalReverie
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import kotlin.text.contains

class CurrencyCommand(val plugin: EternalReverie) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player ||
            !sender.isOp ||
            args.isEmpty()
        ) return false

        val method = args[0]
        val player = Bukkit.getPlayerExact(args[1])
        val value = args[2].toIntOrNull()

        if (player == null) {
            sender.sendMessage(Component.text("¡Este jugador no existe o no está en línea!", NamedTextColor.RED))
            return true
        }
        if (value == null) {
            sender.sendMessage(Component.text("¡Este valor no es un número válido!", NamedTextColor.RED))
            return true
        } else if (value <= 0) {
            sender.sendMessage(Component.text("¡Este valor no es un número válido!", NamedTextColor.RED))
            return true
        }
        when (method) {
            "add" -> {
                CurrencyManager.addBalance(player, value)
                sender.sendMessage(Component.text("Añadidas $value unidades de chatarra del jugador ${player.name}.", NamedTextColor.GREEN))
            }
            "set" -> {
                CurrencyManager.setBalance(player, value)
                sender.sendMessage(Component.text("Asignadas las unidades de chatarra de ${player.name} a $value.", NamedTextColor.GREEN))
            }
            "remove" -> {
                if (!CurrencyManager.tryRemoveBalance(player, value)) {
                    sender.sendMessage(Component.text("¡El jugador no tiene suficiente chatarra!", NamedTextColor.RED))
                    return true
                } else {
                    sender.sendMessage(Component.text("Descontada $value unidades de chatarra del jugador ${player.name}.", NamedTextColor.GREEN))
                }
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("add", "set", "remove")
            2 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.contains(args[1]) }
            3 -> listOf("[valor]")
            else -> emptyList()
        }
    }
}