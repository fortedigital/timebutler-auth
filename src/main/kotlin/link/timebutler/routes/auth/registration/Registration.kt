package link.timebutler.routes.auth.registration

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.*
import com.yubico.webauthn.data.ByteArray
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import link.timebutler.domain.User
import link.timebutler.repository.UserRepository
import java.util.*

internal fun Route.registration(relyingParty: RelyingParty, userRepository: UserRepository) {
    route("register") {
        post("options") {
            val body = call.receive<RegistrationOptions>()
            val user = userRepository.getByUsername(body.username)?.toUserIdentity() ?: createUserIdentity(body.username)
            val selections = AuthenticatorSelectionCriteria
                .builder()
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                .userVerification(UserVerificationRequirement.DISCOURAGED)
                .build()
            val registrationOptions = StartRegistrationOptions
                .builder()
                .user(user)
                .authenticatorSelection(selections)
                .build()
            val credentialOptions = relyingParty
                .startRegistration(registrationOptions)
            call.respondText(credentialOptions.toJson())
        }
    }
}

private fun User.toUserIdentity() = UserIdentity
    .builder()
    .name(username)
    .displayName(username)
    .id(ByteArray(userHandle))
    .build()

private fun createUserIdentity(username: String) = UserIdentity
    .builder()
    .name(username)
    .displayName(username)
    .id(ByteArray(UUID.randomUUID().toString().encodeToByteArray()))
    .build()

@Serializable
private data class RegistrationOptions(val username: String)