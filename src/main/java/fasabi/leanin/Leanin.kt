package fasabi.leanin

import fasabi.leanin.database.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.concurrent.thread
import kotlin.math.round
import fasabi.leanin.commando.*

data class CmdCtx(val sender: CommandSender, val parameters: HashMap<String, String>)

class Leanin : JavaPlugin() {
    companion object {
        val service = WarpsService()
    }

    override fun onEnable() {
        Bukkit.getCommandMap()
        transaction(DatabaseCfg.db) {
            SchemaUtils.create(Users, Warps)
        }
        commands {
            command("warp") {
                command("rename") {
                    command("{old}") {
                        callback("{new}") {

                        }
                    }
                }
                command("remove") {
                    callback("{name}", tab = { sender, _ ->
                        if (sender is Player) {
                            service.getWarps(sender.uniqueId).getOrNull()?.warps?.map { it.name } ?: emptyList()
                        }
                        else {
                            emptyList()
                        }
                    }) { ctx ->
                        if (ctx.sender is Player) {
                            thread {
                                service.deleteWarp(ctx.parameters["name"]!!, ctx.sender.uniqueId).onSuccess {
                                    ctx.sender.sendMessage("warp ${ctx.parameters["name"]!!} removed")
                                }.onFailure {
                                    ctx.sender.sendMessage("failed to delete warp $it")
                                }
                            }
                        }
                    }
                }
                command("set") {
                    callback("{name}") { ctx ->
                        if (ctx.sender is Player) {
                            thread {
                                service.createWarp(ctx.sender.uniqueId, ctx.parameters["name"]!!, ctx.sender.location).onSuccess {
                                    ctx.sender.sendMessage("warp $name set at ${ctx.sender.location.str()}")
                                }.onFailure {
                                    ctx.sender.sendMessage("failed to set warp $it")
                                }
                            }
                        }
                    }
                }
                callback("list") { ctx ->
                    if (ctx.sender is Player) {
                        thread {
                            var message = ""
                            service.getWarps(ctx.sender.uniqueId).onSuccess {
                                message += "limit ${it.limit} ${plural("warp", it.limit)}\n"
                                val remaining = it.limit-it.warps.size
                                message += "remaining $remaining ${plural("warp", remaining)}\n \n"

                                it.warps.forEach {warp ->
                                    message += "${warp.name}\n"
                                    message += "  dimension ${warp.location.world.name}\n"
                                    message += "  at ${warp.location.str()}\n"
                                    message += "  ${round(warp.location.distance(ctx.sender.location))} blocks away\n"
                                }
                                message += "\n "
                                message += "${it.warps.size} ${plural("warp", it.warps.size)} in total\n"
                                ctx.sender.sendMessage(message)
                            }.onFailure {
                                ctx.sender.sendMessage("failed to get warps $it")
                            }
                        }
                    }
                }
                callback("{location}", tab = { sender, _ ->
                    if (sender is Player) {
                        service.getWarps(sender.uniqueId).getOrNull()?.warps?.map { it.name } ?: emptyList()
                    }
                    else {
                        emptyList()
                    }
                }) { ctx ->
                    if (ctx.sender is Player) {
                        thread {
                            service.getWarp(ctx.parameters["location"]!!, ctx.sender.uniqueId).onSuccess {
                                Bukkit.getScheduler().scheduleSyncDelayedTask(this@Leanin) {
                                    ctx.sender.teleport(it.location)
                                }
                            }.onFailure {
                                ctx.sender.sendMessage("failed to get warp $it")
                            }
                        }
                    }
                }
            }
        }
        println("database enabled")
    }

    override fun onDisable() {
        println("disabled")
    }

    private fun plural(t: String, int: Int): String {
        return if (int == 1) {
            t
        } else {
            t+"s"
        }
    }

    fun Location.str(): String {
        return "XYZ: ${round(x)} / ${round(y)} / ${round(z)}"
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        return true
        if (sender is Player) {
            if (args == null || args.isEmpty()) {
                sender.location
            }
            else {
                when(args[0]) {
                    "set" -> {
                        if (args.getOrNull(1) == null) {
                            sender.sendMessage("you need to provide warp name")
                            return false
                        }
                        thread {
                            service.createWarp(sender.uniqueId, args[1], sender.location).onSuccess {
                                sender.sendMessage("warp $name set at ${sender.location.str()}")
                            }.onFailure {
                                sender.sendMessage("failed to set warp $it")
                            }
                        }
                    }
                    "list" -> {
                        thread {
                            var message = ""
                            service.getWarps(sender.uniqueId).onSuccess {
                                message += "limit ${it.limit} ${plural("warp", it.limit)}\n"
                                val remaining = it.limit-it.warps.size
                                message += "remaining $remaining ${plural("warp", remaining)}\n \n"

                                it.warps.forEach {warp ->
                                    message += "${warp.name}\n"
                                    message += "  dimension ${warp.location.world.name}\n"
                                    message += "  at ${warp.location.str()}\n"
                                    message += "  ${round(warp.location.distance(sender.location))} blocks away\n"
                                }
                                message += "\n "
                                message += "${it.warps.size} ${plural("warp", it.warps.size)} in total\n"
                                sender.sendMessage(message)
                            }.onFailure {
                                sender.sendMessage("failed to get warps $it")
                            }
                        }
                    }
                    "help" -> {

                    }
                    "info" -> {

                    }
                    "remove" -> {
                        if (args.getOrNull(1) == null) {
                            sender.sendMessage("you need to provide warp name")
                            return false
                        }
                        thread {
                            service.deleteWarp(args[1], sender.uniqueId).onSuccess {
                                sender.sendMessage("war ${args[1]} removed")
                            }.onFailure {
                                sender.sendMessage("failed to remove warp $it")
                            }
                        }
                    }
                    "delete" -> {
                        if (args.getOrNull(1) == null) {
                            sender.sendMessage("you need to provide warp name")
                            return false
                        }
                        thread {
                            service.deleteWarp(args[1], sender.uniqueId).onSuccess {
                                sender.sendMessage("war ${args[1]} removed")
                            }.onFailure {
                                sender.sendMessage("failed to delete warp $it")
                            }
                        }
                    }
                    else -> {
                        if (args.getOrNull(0) == null) {
                            sender.sendMessage("you need to provide warp name")
                            return false
                        }
                        thread {
                            service.getWarp(args[0], sender.uniqueId).onSuccess {
                                Bukkit.getScheduler().scheduleSyncDelayedTask(this) {
                                    sender.teleport(it.location)
                                }
                            }.onFailure {
                                sender.sendMessage("failed to get warp $it")
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}