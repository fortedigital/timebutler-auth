package link.timebutler.domain

import java.util.*

class User(val id: Int, userHandle: UUID, val username: String) {
    val userHandle: ByteArray = userHandle.toString().encodeToByteArray()
}
