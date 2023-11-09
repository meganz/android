package mega.privacy.android.data.cryptography

import android.util.Base64
import timber.log.Timber
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Secure Decryption with AES/GCM algorithm
 * Note that it only use for new database table or new data store, for the existing table please continue use [DecryptData]
 */
@Suppress("RedundantSuspendModifier")
@Singleton
internal class DecryptData2 @Inject constructor(
    @Named("new_aes_key") private val secretKey: SecretKey,
) {
    suspend operator fun invoke(data: String?) = data?.let {
        runCatching {
            val cipher = Cipher.getInstance(DATA_ENCRYPTED_ALGORITHM)
            val encoded = Base64.decode(data, Base64.DEFAULT)
            val gcmIv: AlgorithmParameterSpec =
                GCMParameterSpec(GCM_SPEC_LENGTH, encoded, 0, GCM_IV_LENGTH)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)
            val original = cipher.doFinal(encoded, GCM_IV_LENGTH, encoded.size - GCM_IV_LENGTH)
            String(original)
        }.onFailure {
            Timber.e(it, "Error decrypting DB field")
        }.getOrNull()
    }
}