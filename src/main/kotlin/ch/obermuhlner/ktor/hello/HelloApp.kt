package ch.obermuhlner.ktor.hello

import ch.obermuhlner.ktor.hello.model.*
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level

fun Application.main() {
    setupDatabase()

    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        jackson {
        }
    }
    install(Authentication) {
        basic {
            realm = "hello-ktor"
            validate {
                if (isValidPassword(it.name, it.password)) {
                    UserIdPrincipal(it.name)
                } else {
                    null
                }
            }
        }
    }

    install(Routing) {
        get("/") {
            call.respondText("Welcome to Ktor Snippet App")
        }
        route("/snippets") {
            get {
                val snippets = transaction {
                    SnippetTable.selectAll().map {
                        Snippet(it[SnippetTable.text])
                    }.toList()
                }
                call.respond(mapOf("snippets" to snippets))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    transaction {
                        SnippetTable.insert {
                            it[text] = post.snippet.text
                        }
                    }
                    call.respond(mapOf(
                        "OK" to true
                    ))
                }
            }
        }

        route("/users") {
            get {
                val users = transaction {
                    UserTable.selectAll().map {
                        User(it[UserTable.name], it[UserTable.password])
                    }.toList()
                }
                call.respond(mapOf("users" to users))
            }
            authenticate {
                post {
                    val post = call.receive<PostUser>()
                    transaction {
                        UserTable.insert {
                            it[name] = post.user.name
                            it[password] = post.user.password
                        }
                    }
                    call.respond(mapOf(
                            "OK" to true
                    ))
                }
            }
        }
    }
}

fun Application.property(path: String, default: String): String {
    return environment.config.propertyOrNull(path)?.getString() ?: default
}

fun Application.setupDatabase() {
    Database.connect(
            url = property("db.url", "jdbc:h2:~/test"),
            driver = property("db.driver", "org.h2.Driver"),
            user = property("db.user", ""),
            password = property("db.password", ""))

    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.create(SnippetTable, UserTable)

        if (UserTable.selectAll().count() == 0L) {
            UserTable.insert {
                it[name] = "admin"
                it[password] = "admin"
            }
        }
    }
}

fun isValidPassword(name: String, password: String) = transaction {
    val user = UserTable.select { UserTable.name eq name }.singleOrNull()
    user != null && user[UserTable.password] == password
}
