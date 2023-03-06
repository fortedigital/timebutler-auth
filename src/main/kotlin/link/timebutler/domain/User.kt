package link.timebutler.domain

import java.util.*

class User(val id: Int = 0, val userHandleUUID: UUID, val username: String) {
    val userHandle: ByteArray = userHandleUUID.toString().encodeToByteArray()
}
