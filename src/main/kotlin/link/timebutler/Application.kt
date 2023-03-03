package link.timebutler

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.PublicKeyCredentialParameters
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import link.timebutler.plugins.configureRouting
import link.timebutler.plugins.configureSecurity
import link.timebutler.plugins.configureSerialization
import link.timebutler.repository.AuthCredentialRepository
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val dataSource = initDatabase(applicationConfig = environment.config)

    val identity = RelyingPartyIdentity
        .builder()
        .id(environment.config.property("auth.domain").getString())
        .name("Timebutler Auth")
        .build()
    val relyingParty = RelyingParty
        .builder()
        .identity(identity)
        .credentialRepository(AuthCredentialRepository(dataSource))
        .origins(environment.config.property("auth.allowed_origins").getString().split(",").toSet())
        .preferredPubkeyParams(listOf(PublicKeyCredentialParameters.ES256, PublicKeyCredentialParameters.RS256))
        .build()
    configureSerialization()
    configureSecurity()
    configureRouting(dataSource, relyingParty)
}

internal fun ApplicationConfig.isDevelopment() =
    this.propertyOrNull("ktor.development")?.getString()?.toBoolean() ?: false

private fun initDatabase(applicationConfig: ApplicationConfig): DataSource {
    val databaseConfig = HikariConfig().apply {
        val dbHost = applicationConfig.property("storage.database.host").getString()
        val dbPort = applicationConfig.property("storage.database.port").getString()
        val dbName = applicationConfig.property("storage.database.name").getString()

        jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        driverClassName = applicationConfig.property("storage.database.driverClass").getString()

        username = applicationConfig.property("storage.database.user").getString()
        password = applicationConfig.property("storage.database.password").getString()
        addDataSourceProperty("databaseName", dbName)
        addDataSourceProperty("serverName", dbHost)
        addDataSourceProperty("portNumber", dbPort)

        maximumPoolSize = 10
        minimumIdle = 1
        idleTimeout = 100000
        connectionTimeout = 100000
        maxLifetime = 300000
    }
    val dataSource = HikariDataSource(databaseConfig)
    val databaseMigrator = Flyway.configure()
        .dataSource(dataSource)
        .cleanDisabled(!applicationConfig.isDevelopment())
        .load()
    if (applicationConfig.isDevelopment()) {
        databaseMigrator.clean()
    }
    databaseMigrator.migrate()
    return dataSource
}
