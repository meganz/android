package mega.privacy.android.data.preferences.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeDataStoreTest {
    private lateinit var underTest: PasscodeDataStore

    private val encryptData = mock<EncryptData>()
    private val decryptData = mock<DecryptData>()
    private val preferences = mock<Preferences> {
        on { get<String>(any()) }.thenReturn("encrypted data")
    }
    private val dataStore = mock<DataStore<Preferences>> {
        on { data }.thenReturn(flow {
            emit(preferences)
            awaitCancellation()
        })
    }

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    internal fun setUp() {
        Mockito.clearInvocations(encryptData, decryptData)
        underTest = PasscodeDataStore(
            dataStore = dataStore,
            encryptData = encryptData,
            decryptData = decryptData,
        )
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that failed attempts are encrypted`() = runTest {
        val input = 8

        underTest.setFailedAttempts(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that failed attempts are decrypted`() = runTest {
        val expected = 5
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        underTest.monitorFailedAttempts().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that passcode is encrypted`() = runTest {
        val input = "My passcode"

        underTest.setPasscode(input)

        verifyBlocking(encryptData) { invoke(input) }
    }

    @Test
    internal fun `test that passcode is decrypted`() = runTest {
        val expected = "My passcode"
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected) }

        assertThat(underTest.getPasscode()).isEqualTo(expected)
    }

    @Test
    internal fun `test that locked state is encrypted`() = runTest {
        val input = true

        underTest.setLockedState(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that locked state is decrypted`() = runTest {
        val expected = false
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        underTest.monitorLockState().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that passcode enabled state is encrypted`() = runTest {
        val input = true

        underTest.setPasscodeEnabledState(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that passcode enabled state is decrypted`() = runTest {
        val expected = false
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        underTest.monitorPasscodeEnabledState().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that passcode timeout is encrypted`() = runTest {
        val input = 12345L

        underTest.setPasscodeTimeout(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that passcode time out is decrypted`() = runTest {
        val expected = 12345L
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        underTest.monitorPasscodeTimeOut().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that last background time is encrypted`() = runTest {
        val input = 6543L

        underTest.setLastBackgroundTime(input)

        verifyBlocking(encryptData) { invoke(input.toString()) }
    }

    @Test
    internal fun `test that last background time is decrypted`() = runTest {
        val expected = 6543L
        decryptData.stub { onBlocking { invoke(any()) }.thenReturn(expected.toString()) }

        underTest.monitorLastBackgroundTime().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that null background time is encrypted as null`() = runTest {
        underTest.setLastBackgroundTime(null)

        verifyBlocking(encryptData) { invoke(null) }
    }

    @Test
    internal fun `test that null passcode is encrypted as null`() = runTest {
        underTest.setPasscode(null)

        verifyBlocking(encryptData) { invoke(null) }
    }

    @Test
    internal fun `test that null passcode timeout is encrypted as null`() = runTest {
        underTest.setPasscodeTimeout(null)

        verifyBlocking(encryptData) { invoke(null) }
    }
}