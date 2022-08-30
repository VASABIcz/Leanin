package fasabi.leanin.database

import fasabi.leanin.CmdCtx
import fasabi.leanin.database.Warps.defaultExpression
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*
import org.bukkit.command.Command as BukkitCommand

object Users : UUIDTable() {
    val warpLimit = integer("limit")
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime())
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var limit by Users.warpLimit
    var timestamp by Users.timestamp.defaultExpression(CurrentDateTime())
}

object Warps : Table() {
    val name = varchar("name", 40)
    val owner = reference("owner", Users)

    val world = uuid("world")
    val x = double("position_x")
    val y = double("position_y")
    val z = double("position_z")
    val yaw = float("yaw")
    val pitch = float("pitch")

    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime())

    override val primaryKey = PrimaryKey(name, owner)
}