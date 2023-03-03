
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class HealthTest {
    @Test
    fun alive() = testApplication {
        val response = client.get("/health/alive")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ALIVE", response.bodyAsText())
    }

    @Test
    fun ready() = testApplication {
        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("READY", response.bodyAsText())
    }
}
