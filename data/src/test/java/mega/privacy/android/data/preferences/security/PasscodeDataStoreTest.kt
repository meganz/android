package mega.privacy.android.data.preferences.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PasscodeDataStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var underTest: PasscodeDataStore
    private lateinit var fileName: String

    @Before
    fun setUp() {
        fileName = "passcode-test-${UUID.randomUUID()}"
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { context.preferencesDataStoreFile(fileName) }
        )
        underTest = PasscodeDataStore(dataStore = dataStore)
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(fileName).delete()
    }

    @Test
    fun `test that failed attempts round trips`() = runTest {
        underTest.setFailedAttempts(8)
        underTest.monitorFailedAttempts().test {
            assertThat(awaitItem()).isEqualTo(8)
        }
    }

    @Test
    fun `test that passcode round trips`() = runTest {
        val passcode = "My passcode"
        underTest.setPasscode(passcode)
        assertThat(underTest.getPasscode()).isEqualTo(passcode)
    }

    @Test
    fun `test that null passcode clears the value`() = runTest {
        underTest.setPasscode("something")
        underTest.setPasscode(null)
        assertThat(underTest.getPasscode()).isNull()
    }

    @Test
    fun `test that locked state round trips`() = runTest {
        underTest.setLockedState(true)
        underTest.monitorLockState().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that passcode enabled state round trips`() = runTest {
        underTest.setPasscodeEnabledState(true)
        underTest.monitorPasscodeEnabledState().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that passcode timeout round trips`() = runTest {
        underTest.setPasscodeTimeout(12345L)
        underTest.monitorPasscodeTimeOut().test {
            assertThat(awaitItem()).isEqualTo(12345L)
        }
    }

    @Test
    fun `test that null passcode timeout clears the value`() = runTest {
        underTest.setPasscodeTimeout(12345L)
        underTest.setPasscodeTimeout(null)
        underTest.monitorPasscodeTimeOut().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that last background time round trips`() = runTest {
        underTest.setLastBackgroundTime(6543L)
        underTest.monitorLastBackgroundTime().test {
            assertThat(awaitItem()).isEqualTo(6543L)
        }
    }

    @Test
    fun `test that null last background time clears the value`() = runTest {
        underTest.setLastBackgroundTime(6543L)
        underTest.setLastBackgroundTime(null)
        underTest.monitorLastBackgroundTime().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that passcode type round trips`() = runTest {
        underTest.setPasscodeType("4")
        underTest.monitorPasscodeType().test {
            assertThat(awaitItem()).isEqualTo("4")
        }
    }

    @Test
    fun `test that null passcode type clears the value`() = runTest {
        underTest.setPasscodeType("4")
        underTest.setPasscodeType(null)
        underTest.monitorPasscodeType().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that biometric enabled state round trips`() = runTest {
        underTest.setBiometricsEnabled(true)
        underTest.monitorBiometricEnabledState().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that null biometric enabled state clears the value`() = runTest {
        underTest.setBiometricsEnabled(true)
        underTest.setBiometricsEnabled(null)
        underTest.monitorBiometricEnabledState().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that configuration change status round trips`() = runTest {
        underTest.setConfigurationChangedStatus(true)
        underTest.monitorConfigurationChangedStatus().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that configuration change status defaults to false`() = runTest {
        underTest.monitorConfigurationChangedStatus().test {
            assertThat(awaitItem()).isFalse()
        }
    }
}
