package mega.privacy.android.app.presentation.passcode.model

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt.CryptoObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_STORE_ID = "AndroidKeyStore"
private const val KEY_NAME = "MEGA_KEY"

/**
 * Passcode crypt object factory
 *
 * @property keyStore
 */
@Singleton
class PasscodeCryptObjectFactory @Inject constructor() {

    private val keyStore = KeyStore.getInstance(KEY_STORE_ID)
    private val cryptoObject by lazy { CryptoObject(getCipher()) }

    operator fun invoke() = cryptoObject
    private fun getCipher() = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7
    ).apply { init(Cipher.ENCRYPT_MODE, getSecretKey()) }

    private fun getSecretKey(): SecretKey {
        KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEY_STORE_ID
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .build()
            )

            generateKey()
        }


        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)

        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }
}