package link.timebutler.domain

import java.util.*

private val FORTE_VALID_USER_DOMAINS = listOf("fortedigital.com", "fortedigital.no")

class User(val id: Int = 0, val userHandleUUID: UUID, val username: String) {
    val userHandle: ByteArray = userHandleUUID.toString().encodeToByteArray()

    init {
        try {
            val domain = username.split("@").last()
            require(domain in FORTE_VALID_USER_DOMAINS) {
                "Invalid username $username"
            }
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Illegal username $username")
        }
    }
}
