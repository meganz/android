package mega.privacy.android.data.cryptography

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates the [StreamingAead] used to encrypt files on disk.
 *
 * The keyset lives in SharedPreferences, wrapped by an AES-256-GCM key in the Android Keystore.
 * The keyset location, master key alias and key template intentionally match the values that
 * the deprecated `androidx.security.crypto.EncryptedFile` used, so files written by earlier
 * versions of the app remain readable. Changing any of them makes every existing encrypted file
 * (credentials, passcode and the SQLCipher passphrase) undecryptable.
 *
 * On a fresh install Tink generates the Keystore master key itself with the same spec the
 * deprecated `MasterKey` used (AES-256, GCM, no padding, encrypt and decrypt purposes), so keys
 * created by either implementation are interchangeable.
 *
 * When the Keystore is unusable Tink would silently store the keyset in cleartext instead. The
 * deprecated implementation failed outright in that case and callers fell back to unencrypted
 * storage, so this factory returns null there too. Otherwise a device that previously wrote
 * plaintext files would suddenly be handed a working cipher and fail to read them.
 */
internal class FileStreamingAeadFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * @return the [StreamingAead], or null when the keyset cannot be created or read, in which
     * case callers fall back to unencrypted storage.
     */
    operator fun invoke(): StreamingAead? = runCatching {
        StreamingAeadConfig.register()
        val keysetManager = AndroidKeysetManager.Builder()
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE_NAME))
            .withSharedPref(context, KEYSET_ALIAS, KEYSET_PREF_NAME)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
        check(keysetManager.isUsingKeystore) { "Android Keystore is unavailable" }
        keysetManager.keysetHandle
            .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }.onFailure {
        Timber.e(it, "Failed to create StreamingAead")
    }.getOrNull()

    companion object {
        private const val MASTER_KEY_URI = "android-keystore://_androidx_security_master_key_"
        private const val KEYSET_PREF_NAME = "__androidx_security_crypto_encrypted_file_pref__"
        private const val KEYSET_ALIAS = "__androidx_security_crypto_encrypted_file_keyset__"
        private const val KEY_TEMPLATE_NAME = "AES256_GCM_HKDF_4KB"

        /**
         * Associated data authenticated alongside the ciphertext of [fileName]. Must be the file
         * name, matching what `EncryptedFile` used.
         */
        fun associatedData(fileName: String): ByteArray = fileName.toByteArray(Charsets.UTF_8)
    }
}
