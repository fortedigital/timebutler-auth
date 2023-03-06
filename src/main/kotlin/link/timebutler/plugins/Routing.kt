package link.timebutler.plugins

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.PublicKeyCredentialParameters
import com.yubico.webauthn.data.RelyingPartyIdentity
import io.ktor.server.application.*
import io.ktor.server.routing.*
import link.timebutler.repository.AuthCredentialRepository
import link.timebutler.repository.UserRepository
import link.timebutler.routes.auth.auth
import link.timebutler.routes.health
import javax.sql.DataSource

// FIXME
fun Application.configureRouting(dataSource: DataSource) {
    val userRepository = UserRepository(dataSource)
    val credentialRepository = AuthCredentialRepository(dataSource = dataSource, userRepository = userRepository)

    val identity = RelyingPartyIdentity
        .builder()
        .id(environment.config.property("auth.domain").getString())
        .name("TimeButler Auth")
        .build()
    val relyingParty = RelyingParty
        .builder()
        .identity(identity)
        .credentialRepository(credentialRepository)
        .origins(environment.config.property("auth.allowed_origins").getString().split(",").toSet())
        .preferredPubkeyParams(listOf(PublicKeyCredentialParameters.ES256, PublicKeyCredentialParameters.RS256))
        .build()

    routing {
        health(dataSource)
        route("api") {
            auth(relyingParty = relyingParty, userRepository = userRepository)
        }
    }
}

