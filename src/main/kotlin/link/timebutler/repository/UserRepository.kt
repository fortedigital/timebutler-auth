package link.timebutler.repository

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import link.timebutler.domain.User
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class UserRepository(private val dataSource: DataSource) {
    fun getByUsername(username: String): User? {
        @Language("PostgreSQL")
        val query = """
            select * from users where username = :username
        """.trimIndent()

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("username" to username)).map {
                it.mapToUser()
            }.asSingle)
        }
    }

    fun getByUserHandle(userHandle: UUID): User? {
        @Language("PostgreSQL")
        val query = """
            select * from users where user_handle = :userHandle
        """.trimIndent()

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("userHandle" to userHandle)).map(Row::mapToUser).asSingle)
        }
    }
}

private fun Row.mapToUser() = User(
    id = int("id"),
    userHandle = uuid("user_handle"),
    username = string("username")
)