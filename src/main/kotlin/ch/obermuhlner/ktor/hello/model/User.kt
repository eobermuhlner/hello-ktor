package ch.obermuhlner.ktor.hello.model

import org.jetbrains.exposed.sql.Table

object UserTable : Table() {
    val id = integer("id").autoIncrement()
    var name = varchar("name", length = 256)
    var password = varchar("password", length = 256)

    override val primaryKey = PrimaryKey(id)
}

data class User(val name: String, val password: String)

data class PostUser(val user: User)

