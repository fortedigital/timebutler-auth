package link.timebutler.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

internal fun Route.health(dataSource: DataSource) {
    route("health") {
        get("alive") {
            call.respondText("ALIVE")
        }
        get("ready") {
            runDatabaseReadinessQuery(dataSource)
            call.respondText("READY")
        }
    }
}

private fun runDatabaseReadinessQuery(dataSource: DataSource) {
    sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT 1").map {
            it.string(1)
        }.asSingle)
    }
}