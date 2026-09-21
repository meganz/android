package mega.privacy.android.data.cryptography

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Guards the on-disk compatibility with the deprecated `androidx.security.crypto` library:
 * files written by either implementation must be readable by the other, because existing
 * installs hold credentials, passcode and the SQLCipher passphrase written by the legacy one.
 */
internal class FileStreamingAeadFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.filesDir, "legacy_compat_test.bin")
    private val payload = Random(42).nextBytes(1024)

    @Before
    fun setUp() {
        file.delete()
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun test_that_file_encrypted_by_legacy_EncryptedFile_is_decrypted_by_StreamingAead() {
        legacyEncryptedFile().openFileOutput().use { it.write(payload) }

        val streamingAead = requireNotNull(FileStreamingAeadFactory(context)())
        val decrypted = streamingAead
            .newDecryptingStream(
                file.inputStream(),
                FileStreamingAeadFactory.associatedData(file.name),
            )
            .use { it.readBytes() }

        assertThat(decrypted).isEqualTo(payload)
    }

    @Test
    fun test_that_file_encrypted_by_StreamingAead_is_decrypted_by_legacy_EncryptedFile() {
        val streamingAead = requireNotNull(FileStreamingAeadFactory(context)())
        streamingAead
            .newEncryptingStream(
                file.outputStream(),
                FileStreamingAeadFactory.associatedData(file.name),
            )
            .use { it.write(payload) }

        val decrypted = legacyEncryptedFile().openFileInput().use { it.readBytes() }

        assertThat(decrypted).isEqualTo(payload)
    }

    @Test
    fun test_that_keyset_is_stored_where_legacy_EncryptedFile_stored_it() {
        requireNotNull(FileStreamingAeadFactory(context)())

        val legacyKeysetPrefs = context.getSharedPreferences(
            "__androidx_security_crypto_encrypted_file_pref__",
            Context.MODE_PRIVATE,
        )

        assertThat(legacyKeysetPrefs.contains("__androidx_security_crypto_encrypted_file_keyset__"))
            .isTrue()
    }

    @Suppress("DEPRECATION")
    private fun legacyEncryptedFile(): EncryptedFile {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
    }
}
