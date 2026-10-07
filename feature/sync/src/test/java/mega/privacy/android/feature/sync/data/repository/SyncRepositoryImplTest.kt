package mega.privacy.android.feature.sync.data.repository

import androidx.work.NetworkType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.data.mapper.sync.SyncTypeMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.gateway.SyncWorkManagerGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mapper.SyncByWifiToNetworkTypeMapper
import mega.privacy.android.feature.sync.data.mapper.stalledissue.StalledIssueTypeMapper
import mega.privacy.android.feature.sync.data.mapper.stalledissue.StalledIssuesMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncRepositoryImplTest {

    private lateinit var underTest: SyncRepositoryImpl
    private val syncGateway: SyncGateway = mock()
    private val syncStatsCacheGateway: SyncStatsCacheGateway = mock()
    private val syncWorkManagerGateway: SyncWorkManagerGateway = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val folderPairMapper: FolderPairMapper = FolderPairMapper(mock(), mock(), mock())
    private val stalledIssuesMapper: StalledIssuesMapper = StalledIssuesMapper(
        StalledIssueTypeMapper()
    )
    private val syncErrorMapper: SyncErrorMapper = mock()
    private val syncTypeMapper: SyncTypeMapper = mock()

    private val fakeGlobalUpdatesFlow = MutableSharedFlow<GlobalUpdate>()
    private val fakeSyncUpdatesFlow = MutableSharedFlow<MegaSyncListenerEvent>()
    private val scheduler = TestCoroutineScheduler()
    private val unconfinedTestDispatcher = UnconfinedTestDispatcher(scheduler)
    private val testScope = CoroutineScope(unconfinedTestDispatcher)
    private val syncByWifiToNetworkTypeMapper: SyncByWifiToNetworkTypeMapper = mock()


    @BeforeAll
    fun setUp() {
        whenever(syncGateway.syncUpdate).thenReturn(fakeSyncUpdatesFlow)
        whenever(megaApiGateway.globalUpdates).thenReturn(fakeGlobalUpdatesFlow)
        underTest = SyncRepositoryImpl(
            syncGateway = syncGateway,
            syncStatsCacheGateway = syncStatsCacheGateway,
            megaApiGateway = megaApiGateway,
            folderPairMapper = folderPairMapper,
            stalledIssuesMapper = stalledIssuesMapper,
            ioDispatcher = unconfinedTestDispatcher,
            syncErrorMapper = syncErrorMapper,
            syncTypeMapper = syncTypeMapper,
            syncWorkManagerGateway = syncWorkManagerGateway,
            syncByWifiToNetworkTypeMapper = syncByWifiToNetworkTypeMapper,
            appScope = testScope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            syncGateway,
            syncStatsCacheGateway,
        )
    }

    @ParameterizedTest(name = " if sync type is {0}")
    @MethodSource("provideSyncTypeMapperParametersDirect")
    fun `test that setupFolderPair invokes gateway syncFolderPair method`(
        syncTypeMapperOriginValue: SyncType,
        syncTypeMapperResultValue: MegaSync.SyncType
    ) = runTest {
        whenever(syncTypeMapper(syncTypeMapperOriginValue)).thenReturn(syncTypeMapperResultValue)
        underTest.setupFolderPair(
            syncType = syncTypeMapperOriginValue,
            name = "name",
            localPath = "localPath",
            remoteFolderId = 123,
        )
        verify(syncGateway).syncFolderPair(
            syncType = syncTypeMapperResultValue,
            name = "name",
            localPath = "localPath",
            remoteFolderId = 123,
        )
    }

    @Test
    fun `test that pauseSync invokes gateway pauseSync method`() = runTest {
        underTest.pauseSync(123)
        verify(syncGateway).pauseSync(123)
    }

    @Test
    fun `test that resumeSync invokes gateway resumeSync method`() = runTest {
        underTest.resumeSync(123)
        verify(syncGateway).resumeSync(123)
    }

    @Test
    fun `test that getFolderPairs invokes gateway getFolderPairs method`() = runTest {
        val megaSyncList = mock<MegaSyncList>()
        whenever(syncGateway.getFolderPairs()).thenReturn(megaSyncList)
        underTest.getFolderPairs()
        verify(syncGateway).getFolderPairs()
    }

    @Test
    fun `test that removeFolderPair invokes gateway removeFolderPair method`() = runTest {
        underTest.removeFolderPair(123)
        verify(syncGateway).removeFolderPair(123)
    }

    @Test
    fun `test that getSyncStalledIssues invokes gateway getSyncStalledIssues method`() = runTest {
        underTest.getSyncStalledIssues()
        verify(syncGateway).getSyncStalledIssues()
    }

    @Test
    fun `test that isNodeSyncableWithError returns sync error`() = runTest {
        val syncError = SyncError.UNSUPPORTED_FILE_SYSTEM
        val syncErrorCode = 10L
        val sdkErrorCode = 11L
        val errorString = "mock error"
        val megaError: MegaError = mock {
            on { it.errorCode } doReturn sdkErrorCode.toInt()
            on { it.syncError } doReturn syncErrorCode.toInt()
            on { it.errorString } doReturn errorString
        }
        val nodeId = 123L
        val megaNode: MegaNode = mock()
        val domainError =
            MegaSyncException(sdkErrorCode.toInt(), errorString, syncError = syncError)
        whenever(syncErrorMapper(syncErrorCode.toInt())).thenReturn(syncError)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId)).thenReturn(megaNode)
        whenever(syncGateway.isNodeSyncableWithError(megaNode)).thenReturn(megaError)

        val result = runCatching {
            underTest.tryNodeSync(NodeId(nodeId))
        }.exceptionOrNull() as MegaSyncException?

        assertAll(
            "Grouped Assertions of ${MegaException::class.simpleName}",
            { assertThat(result?.errorCode).isEqualTo(domainError.errorCode) },
            { assertThat(result?.errorString).isEqualTo(domainError.errorString) },
            { assertThat(result?.syncError).isEqualTo(domainError.syncError) },
        )
    }

    @Test
    fun `test that startSyncWorker invokes gateway enqueueSyncWorkerRequest method`() = runTest {
        val frequency = 15
        whenever(syncByWifiToNetworkTypeMapper(true)).thenReturn(NetworkType.UNMETERED)

        underTest.startSyncWorker(frequencyInMinutes = frequency, true)

        verify(syncWorkManagerGateway).enqueueSyncWorkerRequest(frequency, NetworkType.UNMETERED)
    }

    @Test
    fun `test that stopSyncWorker invokes gateway cancelSyncWorkerRequest method`() = runTest {
        underTest.stopSyncWorker()
        verify(syncWorkManagerGateway).cancelSyncWorkerRequest()
    }

    @Test
    fun `test that changeSyncLocalRoot calls changeSyncLocalRoot on MegaApi`() = runTest {
        val syncId = 123L
        val newLocalPath = "content://com.example.app/syncs/123"
        whenever(syncGateway.changeSyncLocalRoot(syncId, newLocalPath)).thenReturn(syncId)

        val result = underTest.changeSyncLocalRoot(syncId, newLocalPath)

        assertThat(result).isEqualTo(syncId)
    }

    @Test
    fun `test that rescanSync calls rescanSync on the gateway`() = runTest {
        val syncId = 123L

        underTest.rescanSync(syncId, reFingerprint = true)

        verify(syncGateway).rescanSync(syncId, true)
    }

    @Test
    fun `test that rescanSync passes the re-fingerprint flag through when it is turned off`() =
        runTest {
            val syncId = 123L

            underTest.rescanSync(syncId, reFingerprint = false)

            verify(syncGateway).rescanSync(syncId, false)
        }

    @Test
    fun `test that monitorStalledIssues does not delay sync updates when they arrive faster than the refresh delay`() =
        runTest {
            val syncUpdates = stubSyncEventSources()
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repository = createRepositoryForFlows(backgroundScope, dispatcher)
            backgroundScope.launch(dispatcher) { repository.monitorStalledIssues().collect {} }
            runCurrent()
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
            val event = MegaSyncListenerEvent.OnSyncStateChanged(mock())
            val timeBeforeBurst = currentTime

            repeat(BURST_SIZE) { syncUpdates.emit(event) }

            assertThat(currentTime).isEqualTo(timeBeforeBurst)
        }

    @Test
    fun `test that monitorStalledIssues refreshes with the latest change after a burst`() =
        runTest {
            val syncUpdates = stubSyncEventSources()
            val refreshTimes = mutableListOf<Long>()
            whenever(syncGateway.getSyncStalledIssues()).doSuspendableAnswer {
                refreshTimes += currentTime
                null
            }
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repository = createRepositoryForFlows(backgroundScope, dispatcher)
            backgroundScope.launch(dispatcher) { repository.monitorStalledIssues().collect {} }
            runCurrent()
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
            val event = MegaSyncListenerEvent.OnSyncStateChanged(mock())

            repeat(BURST_SIZE) { syncUpdates.emit(event) }
            advanceTimeBy(10 * REFRESH_DELAY_MILLIS)
            runCurrent()

            assertThat(refreshTimes)
                .containsExactly(0L, REFRESH_DELAY_MILLIS, 2 * REFRESH_DELAY_MILLIS)
                .inOrder()
        }

    @Test
    fun `test that monitorFolderPairChanges still emits for every sync update during a stalled issue refresh`() =
        runTest {
            val syncUpdates = stubSyncEventSources()
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repository = createRepositoryForFlows(backgroundScope, dispatcher)
            var folderPairEmissions = 0
            backgroundScope.launch(dispatcher) { repository.monitorStalledIssues().collect {} }
            backgroundScope.launch(dispatcher) {
                repository.monitorFolderPairChanges().collect { folderPairEmissions++ }
            }
            runCurrent()
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
            val event = MegaSyncListenerEvent.OnSyncStateChanged(mock())
            val timeBeforeBurst = currentTime

            backgroundScope.launch(dispatcher) {
                repeat(BURST_SIZE) { syncUpdates.emit(event) }
            }
            runCurrent()

            assertThat(currentTime).isEqualTo(timeBeforeBurst)
            assertThat(folderPairEmissions).isEqualTo(BURST_SIZE + 1)
        }

    private fun createRepositoryForFlows(
        appScope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher,
    ) = SyncRepositoryImpl(
        syncWorkManagerGateway = syncWorkManagerGateway,
        syncGateway = syncGateway,
        syncStatsCacheGateway = syncStatsCacheGateway,
        megaApiGateway = megaApiGateway,
        folderPairMapper = folderPairMapper,
        stalledIssuesMapper = stalledIssuesMapper,
        syncErrorMapper = syncErrorMapper,
        syncTypeMapper = syncTypeMapper,
        syncByWifiToNetworkTypeMapper = syncByWifiToNetworkTypeMapper,
        ioDispatcher = ioDispatcher,
        appScope = appScope,
    )

    private suspend fun stubSyncEventSources(): MutableSharedFlow<MegaSyncListenerEvent> {
        val syncUpdates = MutableSharedFlow<MegaSyncListenerEvent>()
        whenever(syncGateway.syncUpdate).thenReturn(syncUpdates)
        whenever(megaApiGateway.globalUpdates).thenReturn(MutableSharedFlow())
        whenever(syncGateway.getSyncStalledIssues()).thenReturn(null)
        val emptySyncList = mock<MegaSyncList> { on { size() } doReturn 0 }
        whenever(syncGateway.getFolderPairs()).thenReturn(emptySyncList)
        return syncUpdates
    }

    private fun provideSyncTypeMapperParametersDirect(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY, MegaSync.SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP, MegaSync.SyncType.TYPE_BACKUP),
        Arguments.of(SyncType.TYPE_UNKNOWN, MegaSync.SyncType.TYPE_UNKNOWN),
    )

    private companion object {
        // Must exceed the merge channel plus the shared buffer (64 + 64) to expose back-pressure.
        const val BURST_SIZE = 200
        const val REFRESH_DELAY_MILLIS = 5_000L
    }
}
