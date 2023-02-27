package link.timebutler.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import link.timebutler.routes.health
import javax.sql.DataSource

// FIXME
fun Application.configureRouting(dataSource: DataSource) {
    routing {
        health(dataSource)
    }
}

