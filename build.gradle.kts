val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val webauthn_server_version: String by project
val hikari_version: String by project
val flyway_version: String by project
val postgres_version: String by project
val kotliquery_version: String by project

plugins {
    kotlin("jvm") version "1.8.10"
    id("io.ktor.plugin") version "2.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

group = "link.timebutler"
version = "0.0.1"
application {
    mainClass.set("link.timebutler.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    // tests
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation(kotlin("test"))


    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Database
    implementation("com.github.seratch:kotliquery:$kotliquery_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")

    // Webauthn
    implementation("com.yubico:webauthn-server-core:$webauthn_server_version")
}

tasks {
    test {
        environment(
            "TIMEBUTLER_AUTH_DATABASE_HOST" to "localhost",
            "TIMEBUTLER_AUTH_DATABASE_PORT" to "5432",
            "TIMEBUTLER_AUTH_DATABASE_NAME" to "AUTH_DB",
            "TIMEBUTLER_AUTH_DATABASE_USER" to "postgres",
            "TIMEBUTLER_AUTH_DATABASE_PASSWORD" to "superbadpassword",
            "TIMEBUTLER_AUTH_DOMAIN" to "localhost",
            "TIMEBUTLER_AUTH_ALLOWED_ORIGINS" to "http://localhost:5173",
            "FLYWAY_CLEAN_DISABLED" to "false"
        )
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(17)
}