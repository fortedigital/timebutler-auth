package routes.auth.registration

import com.yubico.webauthn.data.COSEAlgorithmIdentifier
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserVerificationRequirement
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotliquery.queryOf
import kotliquery.sessionOf
import link.timebutler.getDataSource
import link.timebutler.getFlyway
import link.timebutler.plugins.RegistrationOptionsSession
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import routes.auth.MockRegistration
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class RegistrationTest {
    private val appConfig = ApplicationConfig(null)
    private val dataSource = getDataSource(appConfig)

    @BeforeEach
    fun setUp() {
        getFlyway(appConfig, dataSource).clean()
    }

    @Test
    fun options() = testApplication {
        val minAllowedChallengeSize = 16

        val response = client.post("/api/auth/register/options") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "username": "hei123@fortedigital.com" 
                }
            """.trimMargin()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.decodeFromString<JsonElement>(response.bodyAsText()).jsonObject
        assertEquals("TimeButler Auth", json["rp"]?.jsonObject?.get("name")?.jsonPrimitive?.content)
        assertEquals("localhost", json["rp"]?.jsonObject?.get("id")?.jsonPrimitive?.content)
        assertEquals("hei123@fortedigital.com", json["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content)

        val acceptedPublicKeyTypes = json["pubKeyCredParams"]?.jsonArray?.map {
            COSEAlgorithmIdentifier.fromId(it.jsonObject["alg"]!!.jsonPrimitive.long).get()
        } ?: emptyList()
        assertContains(acceptedPublicKeyTypes, COSEAlgorithmIdentifier.RS256)
        assertContains(acceptedPublicKeyTypes, COSEAlgorithmIdentifier.ES256)

        val challenge = json["challenge"]!!.jsonPrimitive.content
        assertTrue(challenge.toByteArray().size >= minAllowedChallengeSize)

        val authenticatorSelection = json["authenticatorSelection"]!!.jsonObject
        assertEquals(false, authenticatorSelection["requireResidentKey"]!!.jsonPrimitive.boolean)
        assertEquals(
            ResidentKeyRequirement.PREFERRED.value, authenticatorSelection["residentKey"]!!.jsonPrimitive.content
        )
        assertEquals(
            UserVerificationRequirement.PREFERRED.value,
            authenticatorSelection["userVerification"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `username cannot be other than forte domain email`() = testApplication {
        runBlocking {
            assertEquals(HttpStatusCode.BadRequest, client.post("/api/auth/register/options") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "username": "hei123" 
                        }
                    """.trimMargin()
                )
            }.status)
            assertEquals(HttpStatusCode.OK, client.post("/api/auth/register/options") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "username": "hei123@fortedigital.no" 
                        }
                    """.trimMargin()
                )
            }.status)
            assertEquals(HttpStatusCode.OK, client.post("/api/auth/register/options") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "username": "hei123@fortedigital.com" 
                        }
                    """.trimMargin()
                )
            }.status)
            assertEquals(HttpStatusCode.BadRequest, client.post("/api/auth/register/options") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "username": "hei123@mail.com" 
                        }
                    """.trimMargin()
                )
            }.status)
        }
    }

    @Test
    fun `only post method allowed`() = testApplication {
        assertEquals(HttpStatusCode.MethodNotAllowed, client.get("/api/auth/register/options").status)
    }

    @Test
    fun `verifies registration of a given user`() = testApplication {
        val httpClient = createClient {
            install(HttpCookies)
        }

        application {
            routing {
                /**
                 *  This endpoint initializes a registration-session, which assumes/stubs out a call that normally would go to /api/auth/register/options in order to
                 *  have a static challenge and user to test with.
                 *
                 * */
                get("init-session") {
                    call.sessions.set(RegistrationOptionsSession(MockRegistration.publicKeyRequestOptions))
                }
            }
        }
        httpClient.get("init-session")
        val response = httpClient.post("/api/auth/register/verify") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                    {
                        "username": "${MockRegistration.username}",
                        "response": "${MockRegistration.publicKeyResponse}"
                    }
                """.trimIndent()
            )
        }
        val body = response.bodyAsText()
        val expectedCredentialId = "_C-P0bfGluxXiE9TUDMAU9ceUm1_3xa0MhiB7kv_cGw"
        assertTrue(expectedCredentialId in body)

        val isUserRegistered = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL") val query = """
                select count(username) from users where username = '${MockRegistration.username}'
            """.trimIndent()
            session.run(queryOf(query).map { it.int(1) == 1 }.asSingle)
        }
        assertEquals(true, isUserRegistered)
    }
}