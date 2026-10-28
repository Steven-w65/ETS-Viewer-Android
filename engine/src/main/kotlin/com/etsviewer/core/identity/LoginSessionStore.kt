package com.etsviewer.core.identity

import com.etsviewer.core.accounts.LearnerAccount
import com.etsviewer.core.service.digest
import kotlinx.serialization.Serializable
import java.security.SecureRandom

@Serializable
data class LoginSession(val phone: String, val token: String, val account: LearnerAccount, val password: String? = null)
interface LoginSessionStore {
    fun installationId(): String
    fun read(): LoginSession?
    fun write(session: LoginSession)
    fun clearSession()
}
object InstallationIdentity {
    fun create(): String {
        val random = SecureRandom()
        fun part(): String {
            val input = ByteArray(8).also(random::nextBytes).joinToString("") { "%02X".format(it) }
            return digest("MD5", input.toByteArray()).substring(8, 24)
        }
        return part() + "|" + part()
    }
}
