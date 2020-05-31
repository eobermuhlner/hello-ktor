package ch.obermuhlner.ktor.hello.model

import org.jetbrains.exposed.sql.Table

object SnippetTable : Table() {
    val id = integer("id").autoIncrement()
    var text = varchar("text", length = 2000)

    override val primaryKey = PrimaryKey(id)
}

data class Snippet(val text: String)

data class PostSnippet(val snippet: Snippet)
