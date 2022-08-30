package fasabi.leanin.database

import org.jetbrains.exposed.sql.Database

object DatabaseCfg {
    val db by lazy {
        Database.connect("jdbc:h2:./tpDb", "org.h2.Driver")
    }
}