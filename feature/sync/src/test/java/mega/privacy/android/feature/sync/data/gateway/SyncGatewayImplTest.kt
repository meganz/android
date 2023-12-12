package mega.privacy.android.feature.sync.data.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaListenerInterface
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStallList
import nz.mega.sdk.MegaSyncStats
import nz.mega.sdk.StalledIssuesReceiver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncGatewayImplTest {

    private lateinit var underTest: SyncGatewayImpl
    private val megaApi = mock<MegaApiAndroid>()
    private val appScope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeEach
    fun setUp() {
        underTest = SyncGatewayImpl(megaApi, appScope)
    }

    @AfterEach
    fun tearDown() {
        reset(megaApi)
    }

    @Test
    fun `test that removeFolderPair calls removeSync on MegaApi`() = runTest {
        val folderPairId = 123L

        underTest.removeFolderPair(folderPairId)

        verify(megaApi).removeSync(folderPairId)
    }

    @Test
    fun `test that resumeSync calls resumeSync on MegaApi`() = runTest {
        val folderPairId = 123L

        underTest.resumeSync(folderPairId)

        verify(megaApi).resumeSync(folderPairId)
    }

    @Test
    fun `test that pauseSync calls pauseSync on MegaApi`() = runTest {
        val folderPairId = 123L

        underTest.pauseSync(folderPairId)

        verify(megaApi).pauseSync(folderPairId)
    }

    @Test
    fun `test that getFolderPairs returns mega sync list from the API`() = runTest {
        val megaSyncList: MegaSyncList = mock()
        whenever(megaApi.syncs).thenReturn(megaSyncList)

        val actual = underTest.getFolderPairs()

        assertThat(actual).isEqualTo(megaSyncList)
    }

    @Test
    fun `test that monitorOnSyncDeleted emits OnSyncDeleted events`() = runTest {
        val sync: MegaSync = mock()
        val megaApiJava: MegaApiJava = mock()
        val listenerCaptor = argumentCaptor<MegaListenerInterface>()

        whenever(megaApi.addListener(listenerCaptor.capture())).thenAnswer {
            listenerCaptor.firstValue.onSyncDeleted(megaApiJava, sync)
            null
        }

        underTest.monitorOnSyncDeleted().test {
            assertThat(awaitItem()).isEqualTo(sync)
        }
    }

    @Test
    fun `test that monitorOnSyncStatsUpdated emits OnSyncStatsUpdated events`() = runTest {
        val syncStats: MegaSyncStats = mock()
        val megaApiJava: MegaApiJava = mock()
        val listenerCaptor = argumentCaptor<MegaListenerInterface>()

        whenever(megaApi.addListener(listenerCaptor.capture())).thenAnswer {
            listenerCaptor.firstValue.onSyncStatsUpdated(megaApiJava, syncStats)
            null
        }

        underTest.monitorOnSyncStatsUpdated().test {
            assertThat(awaitItem()).isEqualTo(syncStats)
        }
    }

    @Test
    fun `test that monitorOnSyncStateChanged emits OnSyncStateChanged events`() = runTest {
        val sync: MegaSync = mock()
        val megaApiJava: MegaApiJava = mock()
        val listenerCaptor = argumentCaptor<MegaListenerInterface>()
        whenever(megaApi.addListener(listenerCaptor.capture())).thenAnswer {
            listenerCaptor.firstValue.onSyncStateChanged(megaApiJava, sync)
            null
        }

        underTest.monitorOnSyncStateChanged().test {
            assertThat(awaitItem()).isEqualTo(sync)
        }
    }

    @Test
    fun `test that getSyncStalledIssues returns stalled issues list when sync is stalled`() =
        runTest {
            val expectedStallList = mock<MegaSyncStallList>()
            val request: MegaRequest = mock()
            val error: MegaError = mock()
            whenever(request.type).thenReturn(MegaRequest.TYPE_GET_SYNC_STALL_LIST)
            whenever(request.megaSyncStallList).thenReturn(expectedStallList)
            whenever(megaApi.isSyncStalled).thenReturn(true)
            val stalledIssuesReceiverCaptor = argumentCaptor<StalledIssuesReceiver>()
            whenever(megaApi.requestMegaSyncStallList(stalledIssuesReceiverCaptor.capture())).thenAnswer {
                stalledIssuesReceiverCaptor.firstValue.onRequestFinish(megaApi, request, error)
            }

            val result = underTest.getSyncStalledIssues()

            assertThat(result).isEqualTo(expectedStallList)
        }

    @Test
    fun `test that getSyncStalledIssues returns null when sync is not stalled`() = runTest {
        whenever(megaApi.isSyncStalled).thenReturn(false)
        val result = underTest.getSyncStalledIssues()
        assertThat(result).isNull()
    }
}
