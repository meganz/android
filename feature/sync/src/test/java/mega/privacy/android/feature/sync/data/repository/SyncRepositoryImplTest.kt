package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mapper.StalledIssueTypeMapper
import mega.privacy.android.feature.sync.data.mapper.StalledIssuesMapper
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaSyncList
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncRepositoryImplTest {

    private lateinit var underTest: SyncRepositoryImpl
    private val syncGateway: SyncGateway = mock()
    private val syncStatsCacheGateway: SyncStatsCacheGateway = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val folderPairMapper: FolderPairMapper = FolderPairMapper(SyncStatusMapper())
    private val stalledIssuesMapper: StalledIssuesMapper = StalledIssuesMapper(
        StalledIssueTypeMapper()
    )

    private val fakeGlobalUpdatesFlow = MutableSharedFlow<GlobalUpdate>()
    private val fakeSyncUpdatesFlow = MutableSharedFlow<MegaSyncListenerEvent>()
    private val scheduler = TestCoroutineScheduler()
    private val unconfinedTestDispatcher = UnconfinedTestDispatcher(scheduler)
    private val testScope = CoroutineScope(unconfinedTestDispatcher)


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

    @Test
    fun `test that setupFolderPair invokes gateway syncFolderPair method`() = runTest {
        underTest.setupFolderPair("name", "localPath", 123)
        verify(syncGateway).syncFolderPair("name", "localPath", 123)
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

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

}
