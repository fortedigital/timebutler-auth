package routes.auth.registration

import com.yubico.webauthn.data.COSEAlgorithmIdentifier
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserVerificationRequirement
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class RegistrationTest {
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
            ResidentKeyRequirement.PREFERRED.value,
            authenticatorSelection["residentKey"]!!.jsonPrimitive.content
        )
        assertEquals(
            UserVerificationRequirement.PREFERRED.value,
            authenticatorSelection["userVerification"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `username cannot be other than forte domain email`() = testApplication {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                client.post("/api/auth/register/options") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "username": "hei123" 
                        }
                    """.trimMargin()
                    )
                }
            }
        }
        assertDoesNotThrow {
            runBlocking {
                client.post("/api/auth/register/options") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "username": "hei123@fortedigital.no" 
                        }
                    """.trimMargin()
                    )
                }
            }
        }
        assertDoesNotThrow {
            runBlocking {
                client.post("/api/auth/register/options") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "username": "hei123@fortedigital.com" 
                        }
                    """.trimMargin()
                    )
                }
            }
        }
        assertThrows<IllegalArgumentException> {
            runBlocking {
                client.post("/api/auth/register/options") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "username": "hei123@mail.com" 
                        }
                    """.trimMargin()
                    )
                }
            }
        }
    }

    @Test
    fun `only post method allowed`() = testApplication {
        assertEquals(HttpStatusCode.MethodNotAllowed, client.get("/api/auth/register/options").status)

    }
}