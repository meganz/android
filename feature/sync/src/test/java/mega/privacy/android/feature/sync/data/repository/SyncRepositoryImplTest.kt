package mega.privacy.android.feature.sync.data.repository

import androidx.work.NetworkType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncRepositoryImplTest {

    private lateinit var underTest: SyncRepositoryImpl
    private val syncGateway: SyncGateway = mock()
    private val syncStatsCacheGateway: SyncStatsCacheGateway = mock()
    private val syncWorkManagerGateway: SyncWorkManagerGateway = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val folderPairMapper: FolderPairMapper = FolderPairMapper(mock(), mock())
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

    private fun provideSyncTypeMapperParametersDirect(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY, MegaSync.SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP, MegaSync.SyncType.TYPE_BACKUP),
        Arguments.of(SyncType.TYPE_UNKNOWN, MegaSync.SyncType.TYPE_UNKNOWN),
    )
}
