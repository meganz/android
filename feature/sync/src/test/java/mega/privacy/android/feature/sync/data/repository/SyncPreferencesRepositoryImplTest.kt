package mega.privacy.android.feature.sync.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import mega.privacy.android.feature.sync.data.gateway.SyncPreferencesDatastore
import mega.privacy.android.feature.sync.data.gateway.UserPausedSyncGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncPreferencesRepositoryImplTest {

    private lateinit var underTest: SyncPreferencesRepositoryImpl
    private val syncPreferencesDatastore = mock<SyncPreferencesDatastore>()
    private val userPausedSyncGateway = mock<UserPausedSyncGateway>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setUp() {
        underTest = SyncPreferencesRepositoryImpl(
            syncPreferencesDatastore,
            userPausedSyncGateway,
            ioDispatcher
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesDatastore, userPausedSyncGateway)
    }

    @Test
    fun `test that setSyncByWiFi calls setSyncOnlyByWiFi on datastore`() = runTest {
        underTest.setSyncByWiFi(true)

        verify(syncPreferencesDatastore).setSyncOnlyByWiFi(true)
    }

    @Test
    fun `test that monitorSyncByWiFi returns flow from datastore`() = runTest {
        val flow = flowOf(true)
        whenever(syncPreferencesDatastore.monitorSyncOnlyByWiFi()).thenReturn(flow)

        val result = underTest.monitorSyncByWiFi()

        result.test {
            assertThat(awaitItem()).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setOnboardingShown calls setOnboardingShown on datastore`() = runTest {
        underTest.setOnboardingShown(true)

        verify(syncPreferencesDatastore).setOnboardingShown(true)
    }

    @Test
    fun `test that getOnboardingShown returns value from datastore`() = runTest {
        whenever(syncPreferencesDatastore.getOnboardingShown()).thenReturn(true)

        val result = underTest.getOnboardingShown()

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `test that setUserPausedSync calls setUserPausedSync on gateway`() = runTest {
        val syncId = 123L
        underTest.setUserPausedSync(syncId)

        verify(userPausedSyncGateway).setUserPausedSync(syncId)
    }

    @Test
    fun `test that deleteUserPausedSync calls deleteUserPausedSync on gateway`() = runTest {
        val syncId = 123L
        underTest.deleteUserPausedSync(syncId)

        verify(userPausedSyncGateway).deleteUserPausedSync(syncId)
    }

    @Test
    fun `test that getIsSyncPausedByTheUser returns value from gateway`() = runTest {
        val syncId = 123L
        whenever(userPausedSyncGateway.getUserPausedSync(syncId)).thenReturn(
            UserPausedSyncEntity(
                syncId
            )
        )

        val result = underTest.isSyncPausedByTheUser(syncId)

        assertThat(result).isTrue()
    }
}
