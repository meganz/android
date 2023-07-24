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
}