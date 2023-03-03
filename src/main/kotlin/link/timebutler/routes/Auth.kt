package link.timebutler.routes

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.*
import com.yubico.webauthn.data.ByteArray
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

internal fun Routing.auth(relyingParty: RelyingParty) {
    route("auth") {
        route("register") {
            post("options") {
                val user = UserIdentity
                    .builder()
                    .name("Alexander")
                    .displayName("Alexander den store")
                    .id(ByteArray.fromHex(HexFormat.of().formatHex(UUID.randomUUID().toString().toByteArray())))
                    .build()
                val selections = AuthenticatorSelectionCriteria
                    .builder()
                    .residentKey(ResidentKeyRequirement.PREFERRED)
                    .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                    .userVerification(UserVerificationRequirement.PREFERRED)
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
}