package link.timebutler.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.health() {
    route("health") {
        get("alive") {
            call.respondText("ALIVE")
        }
        get("ready") {
            call.respondText("READY")
        }
    }
}