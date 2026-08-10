package mega.privacy.android.data.preferences.security

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.preferences.base.PreferencesSerializer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PasscodeDatastoreV1ToV2MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val decryptData = mock<DecryptData>()
    private lateinit var underTest: PasscodeDatastoreV1ToV2Migration

    @Before
    fun setUp() {
        context.preferencesDataStoreFile(passcodeDatastoreV1Name).delete()
        underTest = PasscodeDatastoreV1ToV2Migration(
            context = context,
            decryptData = decryptData,
        )
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(passcodeDatastoreV1Name).delete()
    }

    private fun seedV1File(build: MutablePreferences.() -> Unit) {
        val prefs = mutablePreferencesOf().apply(build)
        val v1File = context.preferencesDataStoreFile(passcodeDatastoreV1Name)
        v1File.parentFile?.mkdirs()
        v1File.outputStream().use { out ->
            runBlocking { PreferencesSerializer.writeTo(prefs, out) }
        }
    }

    @Test
    fun `test that should migrate returns true when V2 is empty`() = runTest {
        assertThat(underTest.shouldMigrate(mutablePreferencesOf())).isTrue()
    }

    @Test
    fun `test that should migrate returns false when V2 already has data`() = runTest {
        val existing = preferencesOf(PasscodeDataStore.passcodeEnabledKey to true)
        assertThat(underTest.shouldMigrate(existing)).isFalse()
    }

    @Test
    fun `test that migrate decrypts V1 keys and writes typed V2 values`() = runTest {
        seedV1File {
            this[stringPreferencesKey("failedAttemptsKey")] = "ENC_attempts"
            this[stringPreferencesKey("passcode")] = "ENC_passcode"
            this[stringPreferencesKey("lockedState")] = "ENC_locked"
            this[stringPreferencesKey("passcodeEnabled")] = "ENC_enabled"
            this[stringPreferencesKey("passcodeTimeOutKey")] = "ENC_timeout"
            this[stringPreferencesKey("passcodeLastBackgroundKey")] = "ENC_background"
            this[stringPreferencesKey("passcodeTypeKey")] = "ENC_type"
            this[stringPreferencesKey("biometricsEnabledKey")] = "ENC_biometrics"
            this[stringPreferencesKey("configurationChangeKey")] = "ENC_config"
        }
        decryptData.stub {
            on { invoke("ENC_attempts") }.thenReturn("5")
            on { invoke("ENC_passcode") }.thenReturn("1234")
            on { invoke("ENC_locked") }.thenReturn("true")
            on { invoke("ENC_enabled") }.thenReturn("true")
            on { invoke("ENC_timeout") }.thenReturn("60000")
            on { invoke("ENC_background") }.thenReturn("1700000000000")
            on { invoke("ENC_type") }.thenReturn("4")
            on { invoke("ENC_biometrics") }.thenReturn("false")
            on { invoke("ENC_config") }.thenReturn("true")
        }

        val result = underTest.migrate(mutablePreferencesOf())

        assertThat(result[PasscodeDataStore.failedAttemptsKey]).isEqualTo(5)
        assertThat(result[PasscodeDataStore.passcodeKey]).isEqualTo("1234")
        assertThat(result[PasscodeDataStore.lockedStateKey]).isTrue()
        assertThat(result[PasscodeDataStore.passcodeEnabledKey]).isTrue()
        assertThat(result[PasscodeDataStore.passcodeTimeOutKey]).isEqualTo(60_000L)
        assertThat(result[PasscodeDataStore.passcodeLastBackgroundKey])
            .isEqualTo(1_700_000_000_000L)
        assertThat(result[PasscodeDataStore.passcodeTypeKey]).isEqualTo("4")
        assertThat(result[PasscodeDataStore.biometricsEnabledKey]).isFalse()
        assertThat(result[PasscodeDataStore.configurationChangeKey]).isTrue()
    }

    @Test
    fun `test that missing V1 keys are skipped`() = runTest {
        val result = underTest.migrate(mutablePreferencesOf())
        assertThat(result.asMap()).isEmpty()
    }

    @Test
    fun `test that unparseable V1 values are skipped`() = runTest {
        seedV1File {
            this[stringPreferencesKey("failedAttemptsKey")] = "ENC_garbage"
            this[stringPreferencesKey("lockedState")] = "ENC_garbage_bool"
        }
        decryptData.stub {
            on { invoke("ENC_garbage") }.thenReturn("not-a-number")
            on { invoke("ENC_garbage_bool") }.thenReturn("not-a-bool")
            on { invoke(any()) }.thenReturn(null)
        }

        val result = underTest.migrate(mutablePreferencesOf())

        assertThat(result[PasscodeDataStore.failedAttemptsKey]).isNull()
        assertThat(result[PasscodeDataStore.lockedStateKey]).isNull()
    }

    @Test
    fun `test that cleanUp leaves the V1 file in place`() = runTest {
        val v1File = context.preferencesDataStoreFile(passcodeDatastoreV1Name)
        v1File.parentFile?.mkdirs()
        v1File.writeBytes(byteArrayOf(1, 2, 3))

        underTest.cleanUp()

        assertThat(v1File.exists()).isTrue()
    }
}
