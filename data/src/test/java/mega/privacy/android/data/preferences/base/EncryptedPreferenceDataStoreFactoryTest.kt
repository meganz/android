package mega.privacy.android.data.preferences.base

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.monitoring.CrashReporter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EncryptedPreferenceDataStoreFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val crashReporter = mock<CrashReporter>()
    private val enabledKey = booleanPreferencesKey("passcodeEnabled")

    private lateinit var fileName: String
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        fileName = "store-${UUID.randomUUID()}"
        scope = CoroutineScope(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        scope.cancel()
        encryptedFile().delete()
        fallbackFile().delete()
    }

    private fun encryptedFile() = context.preferencesDataStoreFile(fileName)

    private fun fallbackFile() =
        context.preferencesDataStoreFile(unencryptedFallbackFileName(fileName))

    /**
     * Robolectric has no Android keystore, so a null StreamingAead is the only way to reach the
     * fallback - which is also the path this factory takes on a device when the keystore fails.
     */
    private fun createWithoutStreamingAead(
        migrations: List<DataMigration<Preferences>> = emptyList(),
    ): DataStore<Preferences> = PreferenceDataStoreFactory.createEncrypted(
        context = context,
        migrations = migrations,
        scope = scope,
        streamingAead = null,
        fileName = fileName,
        crashReporter = crashReporter,
    )

    /**
     * The keyset lives in memory rather than behind the Android keystore, which Robolectric lacks;
     * the cipher and key template are the ones the production factory uses.
     */
    private fun createWithStreamingAead(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createEncrypted(
            context = context,
            scope = scope,
            streamingAead = inMemoryStreamingAead(),
            fileName = fileName,
            crashReporter = crashReporter,
        )

    private fun inMemoryStreamingAead(): StreamingAead {
        StreamingAeadConfig.register()
        return KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    private fun seed(file: File, build: MutablePreferences.() -> Unit) {
        val preferences = mutablePreferencesOf().apply(build)
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            runBlocking { PreferencesSerializer.writeTo(preferences, out) }
        }
    }

    @Test
    fun `test that migrations are applied when no StreamingAead is available`() = runTest {
        val migration = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = true
            override suspend fun migrate(currentData: Preferences) =
                currentData.toMutablePreferences().apply { this[enabledKey] = true }

            override suspend fun cleanUp() = Unit
        }

        val underTest = createWithoutStreamingAead(migrations = listOf(migration))

        assertThat(underTest.data.first()[enabledKey]).isTrue()
    }

    @Test
    fun `test that the fallback store writes to a file of its own`() = runTest {
        val underTest = createWithoutStreamingAead()

        underTest.edit { it[enabledKey] = true }

        assertThat(fallbackFile().exists()).isTrue()
        assertThat(encryptedFile().exists()).isFalse()
    }

    @Test
    fun `test that the fallback store leaves a file it cannot read at the encrypted path untouched`() =
        runTest {
            encryptedFile().parentFile?.mkdirs()
            encryptedFile().writeBytes(zeroProtobufTag)

            val underTest = createWithoutStreamingAead()
            underTest.edit { it[enabledKey] = true }

            assertThat(encryptedFile().readBytes()).isEqualTo(zeroProtobufTag)
            verify(crashReporter, never()).report(isA<PreferenceDataStoreCorruptionException>())
        }

    @Test
    fun `test that the fallback store adopts preferences left at the encrypted path`() = runTest {
        seed(encryptedFile()) { this[enabledKey] = true }

        val underTest = createWithoutStreamingAead()

        assertThat(underTest.data.first()[enabledKey]).isTrue()
    }

    @Test
    fun `test that the fallback is reported when no StreamingAead is available`() = runTest {
        createWithoutStreamingAead()

        verify(crashReporter).report(isA<EncryptedPreferenceFallbackException>())
    }

    @Test
    fun `test that a corrupt preference file is reported`() = runTest {
        fallbackFile().parentFile?.mkdirs()
        fallbackFile().writeBytes(zeroProtobufTag)

        val underTest = createWithoutStreamingAead()

        assertThat(underTest.data.first().asMap()).isEmpty()
        verify(crashReporter).report(isA<PreferenceDataStoreCorruptionException>())
    }

    @Test
    fun `test that no plaintext file is left on disk once the encrypted store adopts the fallback`() =
        runTest {
            seed(fallbackFile()) { this[enabledKey] = true }

            val underTest = createWithStreamingAead()

            assertThat(underTest.data.first()[enabledKey]).isTrue()
            verify(crashReporter, never()).report(isA<EncryptedPreferenceFallbackException>())
            assertThat(fallbackFile().exists()).isFalse()
            assertThat(String(encryptedFile().readBytes(), Charsets.ISO_8859_1))
                .doesNotContain(enabledKey.name)
        }

    private companion object {
        /**
         * A zero protobuf tag, which no preferences file can hold: it stands in for the bytes of
         * an encrypted file handed to the plaintext reader.
         */
        val zeroProtobufTag = byteArrayOf(0, 0, 0, 0)
    }
}
