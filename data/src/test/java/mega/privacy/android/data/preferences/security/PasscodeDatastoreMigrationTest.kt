package mega.privacy.android.data.preferences.security

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class PasscodeDatastoreMigrationTest {
    private lateinit var underTest: PasscodeDatastoreMigration

    private val databaseHandler = mock<DatabaseHandler>()
    private val passcodeDataStore = mock<PasscodeDataStore>()

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeDatastoreMigration(
            databaseHandler = databaseHandler,
            passcodeDataStoreFactory = mock { on { invoke(anyOrNull()) }.thenReturn(passcodeDataStore) },
        )
    }

    @Test
    internal fun `test that should migrate returns true if no keys exist`() = runTest {
        val currentData = mock<Preferences> { on { asMap() }.thenReturn(emptyMap()) }
        val actual = underTest.shouldMigrate(currentData)
        assertThat(actual).isTrue()
    }

    @Test
    internal fun `test that should migrate returns false if keys exist`() = runTest {
        val valueMap = mapOf<Preferences.Key<*>, Any>(
            Pair(stringPreferencesKey("fakePasscodeEnabledKey"), ""),
            Pair(stringPreferencesKey("fakeFailedAttemptsKey"), ""),
            Pair(stringPreferencesKey("fakePasscodeKey"), ""),
            Pair(stringPreferencesKey("fakeLockedStateKey"), ""),
            Pair(stringPreferencesKey("fakePasscodeTimeOutKey"), ""),
            Pair(stringPreferencesKey("fakePasscodeLastBackgroundKey"), ""),
        )
        val currentData = mock<Preferences> { on { asMap() }.thenReturn(valueMap) }
        val actual = underTest.shouldMigrate(currentData)
        assertThat(actual).isFalse()
    }

    @Test
    internal fun `test that default values are set if no preferences exist`() = runTest {
        databaseHandler.stub {
            on { preferences }.thenReturn(null)
        }

        underTest.migrate(mock())

        verify(passcodeDataStore).setPasscodeEnabledState(false)
        verify(passcodeDataStore).setFailedAttempts(0)
        verify(passcodeDataStore).setPasscode(null)
        verify(passcodeDataStore).setLockedState(false)
        verify(passcodeDataStore).setPasscodeTimeout(null)
        verify(passcodeDataStore).setLastBackgroundTime(null)
        verify(passcodeDataStore).setPasscodeType(null)
        verify(passcodeDataStore).setBiometricsEnabled(null)
    }

    @Test
    internal fun `test that existing values are transferred if they exist`() = runTest {
        val expectedPasscodeLockEnabled = true
        val expectedPasscodeLockCode = "expectedPasscodeLockCode"
        val expectedPasscodeLockRequireTime = 1234L
        val expectedAttempts = 5
        val expectedType = "4"
        val expectedBiometricsState = true

        val megaPreferences = mock<MegaPreferences> {
            on { passcodeLockEnabled }.thenReturn(expectedPasscodeLockEnabled.toString())
            on { passcodeLockCode }.thenReturn(expectedPasscodeLockCode)
            on { passcodeLockEnabled }.thenReturn(expectedPasscodeLockEnabled.toString())
            on { passcodeLockRequireTime }.thenReturn(expectedPasscodeLockRequireTime.toString())
            on { passcodeLockType }.thenReturn(expectedType)
        }
        val megaAttributes = mock<MegaAttributes> {
            on { attempts }.thenReturn(expectedAttempts)
        }

        databaseHandler.stub {
            on { preferences }.thenReturn(megaPreferences)
            on { attributes }.thenReturn(megaAttributes)
            on { isFingerprintLockEnabled }.thenReturn(expectedBiometricsState)
        }

        underTest.migrate(mock())

        verify(passcodeDataStore).setPasscodeEnabledState(expectedPasscodeLockEnabled)
        verify(passcodeDataStore).setFailedAttempts(expectedAttempts)
        verify(passcodeDataStore).setPasscode(expectedPasscodeLockCode)
        verify(passcodeDataStore).setLockedState(expectedPasscodeLockEnabled)
        verify(passcodeDataStore).setPasscodeTimeout(expectedPasscodeLockRequireTime)
        verify(passcodeDataStore).setLastBackgroundTime(null)
        verify(passcodeDataStore).setPasscodeType(expectedType)
        verify(passcodeDataStore).setBiometricsEnabled(expectedBiometricsState)
    }

    @Test
    internal fun `test that null passcode lock time does not cause an exception`() = runTest{
        val expectedPasscodeLockEnabled = true
        val expectedPasscodeLockCode = "expectedPasscodeLockCode"
        val expectedPasscodeLockRequireTime = null
        val expectedAttempts = 5
        val expectedType = "4"
        val expectedBiometricsState = true

        val megaPreferences = mock<MegaPreferences> {
            on { passcodeLockEnabled }.thenReturn(expectedPasscodeLockEnabled.toString())
            on { passcodeLockCode }.thenReturn(expectedPasscodeLockCode)
            on { passcodeLockEnabled }.thenReturn(expectedPasscodeLockEnabled.toString())
            on { passcodeLockRequireTime }.thenReturn(expectedPasscodeLockRequireTime)
            on { passcodeLockType }.thenReturn(expectedType)
        }
        val megaAttributes = mock<MegaAttributes> {
            on { attempts }.thenReturn(expectedAttempts)
        }

        databaseHandler.stub {
            on { preferences }.thenReturn(megaPreferences)
            on { attributes }.thenReturn(megaAttributes)
            on { isFingerprintLockEnabled }.thenReturn(expectedBiometricsState)
        }

        underTest.migrate(mock())

        verify(passcodeDataStore).setPasscodeEnabledState(expectedPasscodeLockEnabled)
        verify(passcodeDataStore).setFailedAttempts(expectedAttempts)
        verify(passcodeDataStore).setPasscode(expectedPasscodeLockCode)
        verify(passcodeDataStore).setLockedState(expectedPasscodeLockEnabled)
        verify(passcodeDataStore).setPasscodeTimeout(expectedPasscodeLockRequireTime)
        verify(passcodeDataStore).setLastBackgroundTime(null)
        verify(passcodeDataStore).setPasscodeType(expectedType)
        verify(passcodeDataStore).setBiometricsEnabled(expectedBiometricsState)
    }
}