package routes.auth.registration

import com.yubico.webauthn.data.COSEAlgorithmIdentifier
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserVerificationRequirement
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
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
                    "username": "hei123" 
                }
            """.trimMargin()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.decodeFromString<JsonElement>(response.bodyAsText()).jsonObject
        assertEquals("TimeButler Auth", json["rp"]?.jsonObject?.get("name")?.jsonPrimitive?.content)
        assertEquals("localhost", json["rp"]?.jsonObject?.get("id")?.jsonPrimitive?.content)
        assertEquals("hei123", json["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content)

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
            UserVerificationRequirement.DISCOURAGED.value,
            authenticatorSelection["userVerification"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `only post method allowed`() = testApplication {
        assertEquals(HttpStatusCode.MethodNotAllowed, client.get("/api/auth/register/options").status)

    }
}