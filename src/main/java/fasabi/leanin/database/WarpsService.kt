package fasabi.leanin.database

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.IndexOutOfBoundsException
import java.time.LocalDateTime
import java.util.UUID

data class Warp(
    val name: String,
    val timestamp: LocalDateTime,
    val location: Location
)
data class UserWarps(val owner: UUID, val warps: List<Warp>, val limit: Int)

class DatabaseError: Throwable()
class NotFound(name: String): Throwable("$name not found")
class LimitExceeded: Throwable()

class WarpsService {
    fun getWarps(user: UUID): Result<UserWarps> {
        return transaction(DatabaseCfg.db) {
            val usr = User.findById(user) ?: User.new(user) {
                limit = 10
            }
            val locations = Warps.select {
                Warps.owner eq user
            }.toList().map {
                val world = it[Warps.world]
                val x = it[Warps.x]
                val y = it[Warps.y]
                val z = it[Warps.z]
                val pitch = it[Warps.pitch]
                val yaw = it[Warps.yaw]
                val name = it[Warps.name]
                val timestamp = it[Warps.timestamp]

                Warp(
                    name,
                    timestamp,
                    Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)
                )
            }
            return@transaction Result.success(
                UserWarps(user, locations, usr.limit)
            )
        }
    }

    fun getWarp(name: String, user: UUID): Result<Warp> {
        try {
            val w = transaction(DatabaseCfg.db) {
                Warps.select {
                    (Warps.owner eq user) and (Warps.name eq name)
                }.toList().map {
                    val world = it[Warps.world]
                    val x = it[Warps.x]
                    val y = it[Warps.y]
                    val z = it[Warps.z]
                    val pitch = it[Warps.pitch]
                    val yaw = it[Warps.yaw]
                    val name = it[Warps.name]
                    val timestamp = it[Warps.timestamp]

                    Warp(
                        name,
                        timestamp,
                        Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)
                    )
                }
            }
            return Result.success(w[0])
        }
        catch (t: Throwable) {
            if (t is IndexOutOfBoundsException) {
                return Result.failure(NotFound(name))
            }
            return Result.failure(t)
        }
    }

    fun createWarp(user: UUID, namee: String, location: Location): Result<Unit> {
        transaction(db = DatabaseCfg.db) {
            Warps.insert {
                it[x] = location.x
                it[y] = location.y
                it[z] = location.z
                it[pitch] = location.pitch
                it[yaw] = location.yaw
                it[world] = location.world.uid
                it[name] = namee
                it[owner] = user
            }
        }
        return Result.success(Unit)
    }

    fun deleteWarp(name: String, user: UUID): Result<Unit> {
        transaction(DatabaseCfg.db) {
            Warps.deleteWhere {
                (Warps.owner eq user) and (Warps.name eq name)
            }
        }
        return Result.success(Unit)
    }

    fun updateWarp(name: String, user: UUID, location: Location): Result<Unit> {
        transaction(DatabaseCfg.db) {
            Warps.update({
                (Warps.owner eq user) and (Warps.name eq name)
            }) {
                it[x] = location.x
                it[y] = location.y
                it[z] = location.z
                it[pitch] = location.pitch
                it[yaw] = location.yaw
                it[world] = location.world.uid
            }
        }
        return Result.success(Unit)
    }

    fun renameWarp(namee: String, user: UUID, newName: String): Result<Unit> {
        transaction(DatabaseCfg.db) {
            Warps.update({
                (Warps.owner eq user) and (Warps.name eq namee)
            }) {
                it[name] = name
            }
        }
        return Result.success(Unit)
    }
}