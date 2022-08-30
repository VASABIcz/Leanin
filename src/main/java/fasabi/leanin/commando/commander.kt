package fasabi.leanin.commando

import fasabi.leanin.CmdCtx
import fasabi.leanin.Leanin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.round

class CustomCommand(name: String, private val cmd: Command) : org.bukkit.command.Command(name) {
    init {
        description = cmd.description ?: ""
        if (cmd.usage != null) {
            usage = cmd.usage
        }
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>?): Boolean {
        return cmd.route(args?.toList() ?: emptyList(), sender)
    }

    override fun tabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>?,
        location: Location?
    ): MutableList<String> {
        return cmd.autoComp(args?.toList() ?: emptyList(), sender).toMutableList()
    }
}

class Command(
    var name: String,
    val aliases: List<String>?,
    val description: String?,
    val permission: String?,
    val usage: String?,
    val permissionMessage: String?,
    private var callback: ((CmdCtx) -> Unit)?,
    val parent: Command?,
    val tabGen: ((CommandSender, HashMap<String, String>) -> List<String>)?,
    val wildCard: Boolean
) {
    var children = hashMapOf<String, Command>()

    fun call(parameters: HashMap<String, String>, sender: CommandSender): Boolean {
        callback?.let { it(CmdCtx(sender, parameters)) } ?: return false
        return true
    }

    val hasCallback: Boolean
        get() = callback != null


    fun registerCommand(
        name: String? = null,
        description: String? = null,
        aliases: List<String>? = null,
        permission: String? = null,
        usage: String? = null,
        tab: ((CommandSender, HashMap<String, String>) -> List<String>)?,
        permissionMessage: String? = null,
        block: ((CmdCtx) -> Unit)?
    ): Command {
        if (name == null) {
            this.callback = block
            return this
        } else {
            val cmd = if (name.startsWith("{") && name.endsWith("}")) {
                val cd = Command(
                    name.slice(1 until name.length - 1),
                    aliases,
                    description,
                    permission,
                    usage,
                    permissionMessage,
                    block,
                    this,
                    tab,
                    true
                )
                children["*"] = cd
                cd
            } else {
                val cd = Command(
                    name,
                    aliases,
                    description,
                    permission,
                    usage,
                    permissionMessage,
                    block,
                    this,
                    tab,
                    false
                )
                children[name] = cd
                cd
            }
            if (aliases != null) {
                for (alias in aliases) {
                    children[alias] = cmd
                }
            }
            return cmd
        }
    }
}

fun Command.callback(
    name: String? = null,
    description: String? = null,
    aliases: List<String>? = null,
    permission: String? = null,
    usage: String? = null,
    permissionMessage: String? = null,
    tab: ((CommandSender, HashMap<String, String>) -> List<String>)? = null,
    block: (CmdCtx) -> Unit
) {
    this.registerCommand(
        name,
        description,
        aliases,
        permission,
        usage,
        tab,
        permissionMessage,
        block,
    )
}

fun Command.command(
    name: String,
    description: String? = null,
    aliases: List<String>? = null,
    permission: String? = null,
    usage: String? = null,
    permissionMessage: String? = null,
    block: Command.() -> Unit
): Command {
    return this.registerCommand(
        name,
        description,
        aliases,
        permission,
        usage,
        null,
        permissionMessage,
        null
    ).apply(block)
}

fun Command.route(
    cmds: List<String>,
    sender: CommandSender,
    parameters: HashMap<String, String> = hashMapOf()
): Boolean {
    if (cmds.isEmpty()) {
        println(this.name)
        return this.call(parameters, sender)
    }

    var cmd = children[cmds[0]]
    return if (cmd == null) {
        cmd = children["*"]
        if (cmd == null) {
            false
        } else {
            parameters[cmd.name] = cmds[0]
            cmd.route(cmds.slice(1 until cmds.size), sender, parameters)
        }
    } else {
        cmd.route(cmds.slice(1 until cmds.size), sender, parameters)
    }
}

fun Command.autoComp(
    cmds: List<String>,
    sender: CommandSender,
    parameters: HashMap<String, String> = hashMapOf()
): List<String> {
    println("$cmds ${cmds.size}")
    if (cmds.isEmpty()) {
        val complete = mutableListOf<String>()
        complete.addAll(this.children.values.map { it.name })
        this.tabGen?.let { it(sender, parameters) }?.let { complete.addAll(it) }
        return complete
    }
    var cmd = children[cmds[0]]
    return if (cmd == null) {
        cmd = children["*"]
        if (cmd == null) {
            return this.children.values.map { it.name }.filter { it.startsWith(cmds[0], true) }
        } else if (cmds.size == 1) {
            val complete = mutableListOf<String>()
            complete.addAll(this.children.filter { it.key != "*" }.values.map { it.name })
            cmd.tabGen?.let { it(sender, parameters) }?.let { complete.addAll(it) }
            return complete.filter {
                it.startsWith(cmds[0])
            }
        } else {
            parameters[cmd.name] = cmds[0]
            cmd.autoComp(cmds.slice(1 until cmds.size), sender, parameters)
        }
    } else {
        cmd.autoComp(cmds.slice(1 until cmds.size), sender, parameters)
    }
}

fun commands(
    block: Command.() -> Unit
): Command {
    val cmd = Command(
        "root",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false
    ).apply(block)
    cmd.hook()
    return cmd
}

fun Command.walk(indent: Int = 0, cb: (Command, Int) -> Unit) {
    cb(this, indent)
    for ((_, child) in children) {
        child.walk(indent + 1, cb)
    }
}

fun Command.hook() {
    val mapping = Bukkit.getCommandMap()

    for ((name, cmd) in children) {
        mapping.register(name, "fasabi", CustomCommand(name, cmd))
    }
}

fun main() {
    // for testing
    commands {
        command("echo") {
            callback("{text}", tab = { sender, parameters ->
                listOf("hello", "test")
            }) {
                val text = it.parameters["text"]
                it.sender.sendMessage("$text")
            }
        }
    }
    val x = commands {
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
                        Leanin.service.getWarps(sender.uniqueId).getOrNull()?.warps?.map { it.name } ?: emptyList()
                    }
                    else {
                        emptyList()
                    }
                }) { ctx ->
                    if (ctx.sender is Player) {
                        thread {
                            Leanin.service.deleteWarp(ctx.parameters["name"]!!, ctx.sender.uniqueId).onSuccess {
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
                }
            }
            callback("list") { ctx ->
            }
            callback("{location}", tab = { sender, _ ->
                if (sender is Player) {
                    Leanin.service.getWarps(sender.uniqueId).getOrNull()?.warps?.map { it.name } ?: emptyList()
                }
                else {
                    emptyList()
                }
            }) { ctx ->
            }
        }
    }
    println(x.children)
    x.walk { cmd, i ->
        repeat(i) {
            print("  ")
        }
        print("-> ")
        if (cmd.wildCard) {
            print("{${cmd.name}}")
        }
        else {
            print(cmd.name)
        }
        if (cmd.hasCallback) {
            println("()")
        }
        else {
            println()
        }
    }
}