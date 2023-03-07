package link.timebutler.routes.auth.registration

import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.*
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.exception.RegistrationFailedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import link.timebutler.domain.User
import link.timebutler.plugins.RegistrationOptionsSession
import link.timebutler.repository.AuthCredentialRepository
import link.timebutler.repository.Credential
import java.util.*

internal fun Route.registration(relyingParty: RelyingParty, credentialRepository: AuthCredentialRepository) {
    route("register") {
        post("options") {
            val body = call.receive<RegistrationOptionsDTO>()
            val user = try {
                credentialRepository.userRepository.getByUsername(body.username)?.toUserIdentity() ?: User(
                    userHandleUUID = UUID.randomUUID(),
                    username = body.username
                ).toUserIdentity()
            } catch (e: IllegalArgumentException) {
                return@post call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
            }
            val selections = AuthenticatorSelectionCriteria
                .builder()
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build()
            val registrationOptions = StartRegistrationOptions
                .builder()
                .user(user)
                .authenticatorSelection(selections)
                .build()
            val credentialOptions = relyingParty
                .startRegistration(registrationOptions)
            val json = credentialOptions.toJson()
            call.sessions.set(RegistrationOptionsSession(json))
            call.respondText(json)
        }
        post("verify") {
            val request = call.sessions.get<RegistrationOptionsSession>()?.toOptions()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Challenge not generated, need to start registration before verification can happen."
                )
            val body = call.receive<PublicKeyDTO>()
            call.application.log.trace("Request verification started with requestoptions: ${request.toJson()}")
            if (request.user.name != body.username) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Username ${body.username} does not match the request username"
                )
            }
            call.sessions.clear<RegistrationOptionsSession>()
            val user =
                credentialRepository.userRepository.getByUsername(request.user.name)?.toUserIdentity() ?: request.user
            val publicKey = PublicKeyCredential.parseRegistrationResponseJson(body.response)
            val finishRegistrationOptions = FinishRegistrationOptions
                .builder()
                .request(request)
                .response(publicKey)
                .build()
            try {
                val result = relyingParty.finishRegistration(finishRegistrationOptions)
                val storedCredential = credentialRepository.saveCredential(
                    Credential(
                        credentialId = result.keyId.id.base64Url,
                        publicKey = result.publicKeyCose.bytes,
                        user = user,
                        attestationObject = publicKey.response.attestationObject.bytes.decodeToString(),
                        clientDataJson = publicKey.response.clientDataJSON.bytes.decodeToString(),
                        transports = result.keyId.transports.orElse(sortedSetOf()).toList(),
                        signatureCount = result.signatureCount
                    )
                )
                return@post call.respond("Credential created at ${storedCredential.createdAt} with id ${storedCredential.credentialId}")
            } catch (e: RegistrationFailedException) {
                call.application.log.debug("Registration failed", e)
                return@post call.respond(HttpStatusCode.UnprocessableEntity)
            }

        }
    }

}

private fun User.toUserIdentity() = UserIdentity
    .builder()
    .name(username)
    .displayName(username)
    .id(ByteArray(userHandle))
    .build()

@Serializable
private data class RegistrationOptionsDTO(val username: String)

@Serializable
private data class PublicKeyDTO(val response: String, val username: String)