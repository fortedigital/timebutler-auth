package link.timebutler.plugins

import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.application.*

// FIXME
fun Application.configureSecurity() {
    data class MySession(val count: Int = 0)
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
}
