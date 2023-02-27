package link.timebutler.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import link.timebutler.routes.health

// FIXME
fun Application.configureRouting() {
    routing {
        health()
    }
}

