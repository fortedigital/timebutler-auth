package link.timebutler

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import link.timebutler.plugins.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureRouting()
}
