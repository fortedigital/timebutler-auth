package link.timebutler.routes.auth

import com.yubico.webauthn.RelyingParty
import io.ktor.server.routing.*
import link.timebutler.repository.UserRepository
import link.timebutler.routes.auth.registration.registration

internal fun Routing.auth(relyingParty: RelyingParty, userRepository: UserRepository) {
    route("auth") {
        registration(relyingParty, userRepository)
    }
}