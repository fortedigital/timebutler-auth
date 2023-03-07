package link.timebutler.repository

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.*
import com.yubico.webauthn.data.ByteArray
import io.ktor.server.util.*
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import link.timebutler.domain.User
import org.intellij.lang.annotations.Language
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.let
import kotlin.to

internal class AuthCredentialRepository(private val dataSource: DataSource, val userRepository: UserRepository) :
    CredentialRepository {
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
                        .id(ByteArray.fromBase64Url(it.string("credential_id")))
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
            session.run(
                queryOf(
                    query,
                    mapOf("credentialId" to credentialId.base64Url)
                ).map {
                    it.toRegisteredCredential()
                }.asList
            )
        }
        return credentials.toSet()
    }

    fun saveCredential(credential: Credential): StoredCredential {
        @Language("PostgreSQL")
        val query = """
            insert into credentials (credential_id, user_id, public_key, transports, signature_count, attestation_object, client_data_json) values (:credentialId, :userId, :publicKey, :transports, :signatureCount, :attestationObject, to_json(:clientDataJson)) returning *
        """.trimIndent()
        val credential = sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val userId = userRepository.save(
                    transaction = tx,
                    user = User(
                        userHandleUUID = UUID.fromString(
                            Base64.getUrlDecoder().decode(credential.user.id.base64Url).decodeToString()
                        ),
                        username = credential.user.name
                    )
                ) ?: userRepository.getByUsername(credential.user.name)?.id ?: throw IllegalStateException("Could not store credential based on user")
                tx.run(
                    queryOf(
                        query, mapOf(
                            "credentialId" to credential.credentialId,
                            "userId" to userId,
                            "publicKey" to credential.publicKey,
                            "transports" to session.createArrayOf("text", credential.transports.map { it.id }),
                            "signatureCount" to credential.signatureCount,
                            "attestationObject" to credential.attestationObject.replace("\u0000", ""),
                            "clientDataJson" to credential.clientDataJson.replace("\u0000", "")
                        )
                    ).map { it.toStoredCredential(credential.user) }.asSingle
                )
            }
        }!!
        return credential
    }

    private fun Row.toRegisteredCredential() = RegisteredCredential
        .builder()
        .credentialId(ByteArray(bytes("credentials.credential_id")))
        .userHandle(ByteArray(bytes("u.user_handle")))
        .publicKeyCose(ByteArray(bytes("credentials.public_key")))
        .signatureCount(long("signature_count"))
        .build()

}

private fun Row.toStoredCredential(user: UserIdentity): StoredCredential {
    return StoredCredential(
        id = int("id"),
        credentialId = string("credential_id"),
        attestationObject = string("attestation_object"),
        clientDataJson = string("client_data_json"),
        createdAt = sqlTimestamp("created_at").toInstant().atZone(ZoneId.systemDefault()),
        updatedAt = sqlTimestamp("updated_at").toInstant().atZone(ZoneId.systemDefault()),
        publicKey = bytes("public_key"),
        signatureCount = long("signature_count"),
        transports = array<String>("transports").map(AuthenticatorTransport::of),
        user = user
    )
}

data class Credential(
    val credentialId: String,
    val user: UserIdentity,
    val transports: List<AuthenticatorTransport>,
    val publicKey: kotlin.ByteArray,
    val signatureCount: Long,
    val attestationObject: String,
    val clientDataJson: String
)

data class StoredCredential(
    val id: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val credentialId: String,
    val user: UserIdentity,
    val transports: List<AuthenticatorTransport>,
    val publicKey: kotlin.ByteArray,
    val signatureCount: Long,
    val attestationObject: String,
    val clientDataJson: String
)