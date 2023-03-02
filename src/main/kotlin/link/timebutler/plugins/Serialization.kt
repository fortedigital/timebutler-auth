package link.timebutler.plugins

import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.cbor.Cbor

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
        cbor(cbor = Cbor {
            this.ignoreUnknownKeys = false
        })
    }
}
