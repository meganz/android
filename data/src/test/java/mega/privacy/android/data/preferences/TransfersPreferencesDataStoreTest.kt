package mega.privacy.android.data.preferences

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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersPreferencesDataStoreTest {

    private lateinit var underTest: TransfersPreferencesDataStore

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
        underTest = TransfersPreferencesDataStore(
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
    internal fun `test that request files permission denied is encrypted as true`() = runTest {
        underTest.setRequestFilesPermissionDenied()
        verifyBlocking(encryptData) { invoke(true.toString()) }
    }

    @Test
    internal fun `test that request files permission denied is decrypted`() = runTest {
        decryptData.stub { onBlocking { invoke(any()) } doReturn true.toString() }
        underTest.monitorRequestFilesPermissionDenied().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    internal fun `test that request files permission denied is encrypted as false when clearPreferences is invoked`() =
        runTest {
            underTest.clearPreferences()
            verifyBlocking(encryptData) { invoke(false.toString()) }
        }
}