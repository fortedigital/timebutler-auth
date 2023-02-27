package link.timebutler

import io.ktor.server.application.*
import io.ktor.server.netty.*
import link.timebutler.plugins.configureRouting
import link.timebutler.plugins.configureSecurity
import link.timebutler.plugins.configureSerialization

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureRouting()
}
