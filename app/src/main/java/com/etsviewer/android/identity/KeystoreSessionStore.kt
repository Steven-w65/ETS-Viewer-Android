package com.etsviewer.android.identity

import com.etsviewer.core.identity.InstallationIdentity
import com.etsviewer.core.identity.LoginSession
import com.etsviewer.core.identity.LoginSessionStore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreSessionStore(context: Context, name: String = "cloud-access") : LoginSessionStore {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val alias = context.packageName + ".cloud-session"
    private val json = Json { ignoreUnknownKeys = true }
    override fun installationId(): String = preferences.getString("installation", null) ?: InstallationIdentity.create().also {
        check(preferences.edit().putString("installation", it).commit()) { "无法保存安装标识" }
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(): LoginSession? = runCatching {
        val stored = preferences.getString("session", null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val ivLength = bytes[0].toInt()
        require(ivLength in 12..16 && bytes.size > ivLength + 1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, ivLength + 1)))
        json.decodeFromString<LoginSession>(String(cipher.doFinal(bytes.copyOfRange(ivLength + 1, bytes.size)), Charsets.UTF_8))
    }.getOrNull()
    override fun write(session: LoginSession) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(json.encodeToString(session).toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("session", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) { "无法保存登录信息" }
    }
    override fun clearSession() { check(preferences.edit().remove("session").commit()) { "无法清除登录信息" } }
    var theme: String
        get() = preferences.getString("theme", "system") ?: "system"
        set(value) { preferences.edit().putString("theme", value).apply() }
}
