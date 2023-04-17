package mega.privacy.android.data.cryptography

import android.util.Base64
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class DecryptData @Inject constructor(
    @Named("aes_key") private val aesKey: ByteArray,
) {
    operator fun invoke(data: String?) = data?.let {
        runCatching {
            val encoded = Base64.decode(data, Base64.DEFAULT)
            val skeySpec = SecretKeySpec(aesKey, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec)
            val original = cipher.doFinal(encoded)
            String(original)
        }.onFailure {
            Timber.e(it, "Error decrypting DB field")
        }.getOrNull()
    }
}