package link.timebutler.plugins

import com.yubico.webauthn.RelyingParty
import io.ktor.server.application.*
import io.ktor.server.routing.*
import link.timebutler.routes.auth
import link.timebutler.routes.health
import javax.sql.DataSource

// FIXME
fun Application.configureRouting(dataSource: DataSource, relyingParty: RelyingParty) {
    routing {
        health(dataSource)
        auth(relyingParty)
    }
}

