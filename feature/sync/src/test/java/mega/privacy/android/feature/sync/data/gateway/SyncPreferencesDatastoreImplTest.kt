package mega.privacy.android.feature.sync.data.gateway

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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncPreferencesDatastoreImplTest {

    private lateinit var underTest: SyncPreferencesDatastoreImpl

    private val preferences = mock<Preferences>()
    private val dataStore = mock<DataStore<Preferences>> {
        on { data }.thenReturn(flow {
            emit(preferences)
            awaitCancellation()
        })
    }

    @BeforeAll
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = SyncPreferencesDatastoreImpl(dataStore)
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(preferences)
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @ParameterizedTest(name = "onboarding shown state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that getOnboardingShown returns correct value`(
        isOnboardingShown: Boolean,
    ) = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(isOnboardingShown)

        val result = underTest.getOnboardingShown()

        assertThat(result).isEqualTo(isOnboardingShown)
    }

    @Test
    internal fun `test that getOnboardingShown returns null when not set`() = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(null)

        val result = underTest.getOnboardingShown()

        assertThat(result).isNull()
    }

    @ParameterizedTest(name = "sync only by WiFi state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that monitorSyncOnlyByWiFi emits correct value`(
        syncOnlyByWiFi: Boolean,
    ) = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(syncOnlyByWiFi)

        underTest.monitorSyncOnlyByWiFi().test {
            assertThat(awaitItem()).isEqualTo(syncOnlyByWiFi)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that monitorSyncOnlyByWiFi emits null when not set`() = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(null)

        underTest.monitorSyncOnlyByWiFi().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "sync only by charging state: {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that monitorSyncOnlyByCharging emits correct value`(
        syncOnlyByCharging: Boolean,
    ) = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(syncOnlyByCharging)

        underTest.monitorSyncOnlyByCharging().test {
            assertThat(awaitItem()).isEqualTo(syncOnlyByCharging)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that monitorSyncOnlyByCharging emits null when not set`() = runTest {
        whenever(preferences.get<Boolean>(any())).thenReturn(null)

        underTest.monitorSyncOnlyByCharging().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that getSyncFrequencyMinutes returns correct value`() = runTest {
        val expectedFrequency = 30
        whenever(preferences.get<Int>(any())).thenReturn(expectedFrequency)

        val result = underTest.getSyncFrequencyMinutes()

        assertThat(result).isEqualTo(expectedFrequency)
    }

    @Test
    internal fun `test that getSyncFrequencyMinutes returns null when not set`() = runTest {
        whenever(preferences.get<Int>(any())).thenReturn(null)

        val result = underTest.getSyncFrequencyMinutes()

        assertThat(result).isNull()
    }
}
