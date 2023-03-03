package link.timebutler.repository

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.AuthenticatorTransport
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import com.yubico.webauthn.data.PublicKeyCredentialType
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class AuthCredentialRepository(private val dataSource: DataSource, private val userRepository: UserRepository) : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String?): Set<PublicKeyCredentialDescriptor> {
        if (username == null) {
            return emptySet()
        }

        @Language("PostgreSQL")
        val query = """
            select credentials.*, u.user_handle from credentials 
            join users u on credentials.user_id = u.id 
            where username = :username
        """.trimIndent()

        val credentials = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf("username" to username)
                ).map {
                    PublicKeyCredentialDescriptor
                        .builder()
                        .id(ByteArray(it.bytes("credential_id")))
                        .transports(it.array<String>("transports").map(AuthenticatorTransport::of).toSet())
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build()
                }.asList
            )
        }
        return credentials.toSet()
    }

    override fun getUserHandleForUsername(username: String?): Optional<ByteArray> {
        if (username == null) {
            return Optional.empty()
        }

        val userHandle = userRepository.getByUsername(username)?.let { ByteArray(it.userHandle) }
        return Optional.ofNullable(userHandle)
    }

    override fun getUsernameForUserHandle(userHandle: ByteArray?): Optional<String> {
        if (userHandle == null) {
            return Optional.empty()
        }

        val userHandleUUID = UUID.fromString(userHandle.bytes.decodeToString())

        val user = userRepository.getByUserHandle(userHandleUUID)
        return Optional.ofNullable(user?.username)
    }

    override fun lookup(credentialId: ByteArray?, userHandle: ByteArray?): Optional<RegisteredCredential> {
        if (credentialId == null || userHandle == null) {
            return Optional.empty()
        }

        val userHandleUUID = UUID.fromString(userHandle.bytes.decodeToString())

        @Language("PostgreSQL")
        val query = """
            select credentials.*, u.user_handle from credentials 
            join users u on credentials.user_id = u.id 
            where credential_id = :credentialId and user_handle = :userHandle
        """.trimIndent()

        val credential = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf("credentialId" to credentialId, "userHandle" to userHandleUUID)
                ).map { it.toRegisteredCredential() }.asSingle
            )
        }
        return Optional.ofNullable(credential)
    }

    override fun lookupAll(credentialId: ByteArray?): Set<RegisteredCredential> {
        if (credentialId == null) {
            return emptySet()
        }

        @Language("PostgreSQL")
        val query = """
            select credentials.*, u.user_handle from credentials 
            join users u on credentials.user_id = u.id 
            where credential_id = :credentialId 
        """.trimIndent()

        val credentials = sessionOf(dataSource).use { session ->
            session.run(queryOf(
                query,
                mapOf("credentialId" to credentialId.bytes)
            ).map {
                it.toRegisteredCredential()
            }.asList
            )
        }
        return credentials.toSet()
    }

    private fun Row.toRegisteredCredential() = RegisteredCredential
        .builder()
        .credentialId(ByteArray(bytes("credentials.credential_id")))
        .userHandle(ByteArray(bytes("u.user_handle")))
        .publicKeyCose(ByteArray(bytes("credentials.public_key")))
        .signatureCount(long("signature_count"))
        .build()
}