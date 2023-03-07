package link.timebutler.routes.auth.login

import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.UserVerificationRequirement
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import link.timebutler.plugins.AssertionOptionsSession
import link.timebutler.repository.AuthCredentialRepository
import java.util.*

internal fun Route.login(relyingParty: RelyingParty, credentialRepository: AuthCredentialRepository) {
    route("login") {
        post("options") {
            val body = call.receive<AssertionRequestDTO>()
            val username = if (body.username?.isNotEmpty() == true) Optional.of(body.username) else Optional.empty()
            val startAssertionOptions = StartAssertionOptions
                .builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .username(username)
                .build()
            val assertionRequest = relyingParty.startAssertion(startAssertionOptions)
            val json = assertionRequest.toJson()
            call.sessions.set(AssertionOptionsSession(json))
            call.respond(json)
        }
        post("verify") {
            val assertionRequest =
                call.sessions.get<AssertionOptionsSession>()?.toOptions() ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Could not find valid assertionrequest, please restart the login procedure"
                )
            call.sessions.clear<AssertionOptionsSession>()
            val body = call.receive<AssertionVerificationDTO>()
            val publicKey = PublicKeyCredential.parseAssertionResponseJson(body.response)
            val finishAssertionOptions = FinishAssertionOptions
                .builder()
                .request(assertionRequest)
                .response(publicKey)
                .build()
            val result = relyingParty.finishAssertion(finishAssertionOptions)
            credentialRepository.updateCredential(result.credential, result.signatureCount)
            if (result.isSuccess) {
                return@post call.respond(result.username)
            }
            return@post call.respond(HttpStatusCode.BadRequest, "Faen...")
        }
    }
}

@Serializable
private data class AssertionRequestDTO(val username: String? = null)

@Serializable
private data class AssertionVerificationDTO(val username: String? = null, val response: String)