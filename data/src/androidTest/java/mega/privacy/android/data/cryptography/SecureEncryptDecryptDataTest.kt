package mega.privacy.android.data.cryptography

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SecureEncryptDecryptDataTest {
    @Test
    fun `test_that_encrypt_and_decrypt_are_matching`() = runTest {
        repeat(100) {
            val aesKey = provideAesKey()
            val data = UUID.randomUUID().toString()
            val encryptData = EncryptData(aesKey)
            val decryptData = DecryptData(aesKey)
            val encryptedData = encryptData(data)
            val decryptedData = decryptData(encryptedData)
            Truth.assertThat(decryptedData).isEqualTo(data)
        }
    }

    private fun provideAesKey(): ByteArray {
        val key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC\$G*(G($*GR*(#)*huio4h389\$G"
        return key.toByteArray().copyOfRange(0, 32)
    }
}