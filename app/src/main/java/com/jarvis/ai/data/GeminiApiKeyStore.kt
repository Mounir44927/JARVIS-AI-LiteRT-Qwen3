package com.jarvis.ai.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

private val Context.geminiKeyDataStore by preferencesDataStore(name = "jarvis_gemini_key")

/** Stores the Gemini API key locally, encrypted with an Android Keystore AES key. */
class GeminiApiKeyStore(private val context: Context) {
    private object Keys {
        val encryptedApiKey = stringPreferencesKey("encrypted_api_key")
    }

    val isConfigured: Flow<Boolean> = context.geminiKeyDataStore.data.map { !it[Keys.encryptedApiKey].isNullOrBlank() }

    suspend fun getApiKey(): String? = context.geminiKeyDataStore.data.map { prefs ->
        prefs[Keys.encryptedApiKey]?.let(::decrypt)
    }.first()

    suspend fun setApiKey(apiKey: String) {
        val normalized = apiKey.trim()
        context.geminiKeyDataStore.edit { prefs ->
            if (normalized.isBlank()) {
                prefs.remove(Keys.encryptedApiKey)
            } else {
                prefs[Keys.encryptedApiKey] = encrypt(normalized)
            }
        }
    }

    suspend fun clear() = setApiKey("")

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? = runCatching {
        val packed = Base64.decode(value, Base64.NO_WRAP)
        require(packed.size > GCM_IV_LENGTH_BYTES)
        val iv = packed.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encrypted = packed.copyOfRange(GCM_IV_LENGTH_BYTES, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "jarvis_gemini_api_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
