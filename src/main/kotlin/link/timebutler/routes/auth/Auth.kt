package link.timebutler.routes.auth

import com.yubico.webauthn.RelyingParty
import io.ktor.server.routing.*
import link.timebutler.repository.AuthCredentialRepository
import link.timebutler.routes.auth.login.login
import link.timebutler.routes.auth.registration.registration

internal fun Route.auth(relyingParty: RelyingParty, credentialRepository: AuthCredentialRepository) {
    route("auth") {
        registration(relyingParty, credentialRepository)
        login(relyingParty, credentialRepository)
    }
}