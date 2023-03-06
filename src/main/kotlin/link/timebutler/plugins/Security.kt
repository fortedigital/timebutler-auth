package link.timebutler.plugins

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

// FIXME

@Serializable
data class RegistrationOptionsSession(val options: String) {
    fun toOptions() = PublicKeyCredentialCreationOptions.fromJson(options)
}

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<RegistrationOptionsSession>("RegistrationOptions", SessionStorageMemory()) {
            cookie.extensions["SameSite"] = "lax"
        }
    }
}
