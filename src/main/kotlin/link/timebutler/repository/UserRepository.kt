package link.timebutler.repository

import kotliquery.Row
import kotliquery.TransactionalSession
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

    fun save(transaction: TransactionalSession, user: User): User? {
        @Language("PostgreSQL")
        val query = """
            insert into users (user_handle, username) values (:userhandle, :username) on conflict do nothing returning *
        """.trimIndent()
        return transaction.run(
            queryOf(
                query, mapOf(
                    "userhandle" to user.userHandleUUID,
                    "username" to user.username
                )
            ).map(Row::mapToUser).asSingle
        )
    }
}


private fun Row.mapToUser() = User(
    id = int("id"),
    userHandleUUID = uuid("user_handle"),
    username = string("username")
)