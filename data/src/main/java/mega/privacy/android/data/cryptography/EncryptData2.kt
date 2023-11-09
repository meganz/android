package mega.privacy.android.data.cryptography

import android.util.Base64
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Gcm Iv Length
 */
internal const val GCM_IV_LENGTH = 12

/**
 * Data Encrypted Algorithm
 */
internal const val DATA_ENCRYPTED_ALGORITHM = "AES/GCM/NoPadding"

/**
 * Gcm Spec Length
 */
internal const val GCM_SPEC_LENGTH = 128

/**
 * Secure Encryption with AES/GCM algorithm
 * Note that it only use for new database table or new data store, for the existing table please continue use [EncryptData]
 */
@Suppress("RedundantSuspendModifier")
@Singleton
internal class EncryptData2 @Inject constructor(
    @Named("new_aes_key") private val secretKey: SecretKey,
) {
    suspend operator fun invoke(data: String?) = data?.let {
        runCatching {
            val cipher = Cipher.getInstance(DATA_ENCRYPTED_ALGORITHM)
            val iv = SecureRandom.getSeed(GCM_IV_LENGTH)
            val parameterSpec = GCMParameterSpec(GCM_SPEC_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
            val encrypted = cipher.doFinal(it.toByteArray())

            val byteBuffer = ByteBuffer.allocate(iv.size + encrypted.size)
            byteBuffer.put(iv)
            byteBuffer.put(encrypted)
            Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT)
        }.onFailure {
            Timber.e(it, "Error encrypting DB field")
        }.getOrNull()
    }
}