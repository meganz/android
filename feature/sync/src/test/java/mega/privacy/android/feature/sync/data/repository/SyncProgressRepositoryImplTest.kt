package mega.privacy.android.feature.sync.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.progress.SyncRunStateMapper
import mega.privacy.android.feature.sync.data.mapper.progress.SyncStatsSampleMapper
import mega.privacy.android.feature.sync.data.mapper.progress.SyncWorkStateMapper
import mega.privacy.android.feature.sync.data.mapper.progress.mockMegaSync
import mega.privacy.android.feature.sync.data.mapper.progress.mockMegaSyncList
import mega.privacy.android.feature.sync.data.mapper.progress.mockMegaSyncStats
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.usecase.progress.AdvanceSyncProgressEpisodeUseCase
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStats
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncProgressRepositoryImplTest {

    private val syncGateway = mock<SyncGateway>()
    private val syncStatsCacheGateway = mock<SyncStatsCacheGateway>()
    private val syncErrorMapper = mock<SyncErrorMapper>()

    private lateinit var syncUpdates: MutableSharedFlow<MegaSyncListenerEvent>
    private var now = 0L

    private val running = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
    private val suspended = MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue()
    private val disabled = MegaSync.SyncRunningState.RUNSTATE_DISABLED.swigValue()
    private val interruptionHoldMillis =
        AdvanceSyncProgressEpisodeUseCase.INTERRUPTION_HOLD.inWholeMilliseconds

    @BeforeEach
    fun resetMocks() {
        reset(syncGateway, syncStatsCacheGateway, syncErrorMapper)
        syncUpdates = MutableSharedFlow(extraBufferCapacity = 64)
        now = 0L
        whenever(syncGateway.syncUpdate).thenReturn(syncUpdates)
        whenever(syncErrorMapper(any())).thenReturn(SyncError.NO_SYNC_ERROR)
        whenever(syncStatsCacheGateway.getAllSyncStats()).thenReturn(emptyList())
        stubSyncList(mockMegaSyncList())
    }

    private fun stubSyncList(list: MegaSyncList) {
        syncGateway.stub { onBlocking { getFolderPairs() } doReturn list }
    }

    private fun stubCachedStats(vararg stats: MegaSyncStats) {
        whenever(syncStatsCacheGateway.getAllSyncStats()).thenReturn(stats.toList())
    }

    private fun TestScope.createUnderTest() = SyncProgressRepositoryImpl(
        syncGateway = syncGateway,
        syncStatsCacheGateway = syncStatsCacheGateway,
        syncWorkStateMapper = SyncWorkStateMapper(SyncRunStateMapper(), syncErrorMapper),
        syncStatsSampleMapper = SyncStatsSampleMapper(),
        advanceSyncProgressEpisodeUseCase = AdvanceSyncProgressEpisodeUseCase(),
        currentTimeProvider = { now },
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        appScope = backgroundScope,
    )

    private fun TestScope.startCollecting(underTest: SyncProgressRepositoryImpl) {
        backgroundScope.launch { underTest.monitorSyncRunSnapshot().collect {} }
        runCurrent()
    }

    private suspend fun SyncProgressRepositoryImpl.current(): SyncRunSnapshot =
        monitorSyncRunSnapshot().first()

    private suspend fun emit(event: MegaSyncListenerEvent) = syncUpdates.emit(event)

    private fun stats(backupId: Long, uploads: Int) =
        MegaSyncListenerEvent.OnSyncStatsUpdated(mockMegaSyncStats(backupId, uploads = uploads))

    private fun stateChanged(backupId: Long, runState: Int, error: Int = 0) =
        MegaSyncListenerEvent.OnSyncStateChanged(mockMegaSync(backupId, runState, error))

    @Test
    fun `test that building the repository does not start the run snapshot collection`() = runTest {
        createUnderTest()
        runCurrent()

        assertThat(syncUpdates.subscriptionCount.value).isEqualTo(0)
        verify(syncGateway, never()).getFolderPairs()
        verifyNoInteractions(syncStatsCacheGateway)
    }

    @Test
    fun `test that resetSyncRunSnapshot before the first collection does not start the run snapshot collection`() =
        runTest {
            val underTest = createUnderTest()

            underTest.resetSyncRunSnapshot()
            runCurrent()

            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(0)
            verify(syncGateway, never()).getFolderPairs()
            verifyNoInteractions(syncStatsCacheGateway)

            startCollecting(underTest)
            verify(syncGateway, times(1)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncRunSnapshot emits NotSeeded before the sync list is read`() = runTest {
        val gate = CompletableDeferred<MegaSyncList>()
        var subscriptionsAtRead = -1
        syncGateway.stub {
            onBlocking { getFolderPairs() } doSuspendableAnswer {
                subscriptionsAtRead = syncUpdates.subscriptionCount.value
                gate.await()
            }
        }
        val underTest = createUnderTest()

        underTest.monitorSyncRunSnapshot().test {
            assertThat(awaitItem()).isEqualTo(SyncRunSnapshot.NotSeeded)
            runCurrent()
            expectNoEvents()

            gate.complete(mockMegaSyncList())
            assertThat(awaitItem().isSeeded).isTrue()
        }
        assertThat(subscriptionsAtRead).isEqualTo(1)
    }

    @Test
    fun `test that monitorSyncRunSnapshot is seeded with no syncs when the sync list is empty`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)

            val actual = underTest.current()
            assertThat(actual.isSeeded).isTrue()
            assertThat(actual.syncs).isEmpty()
        }

    @Test
    fun `test that monitorSyncRunSnapshot seeds running syncs from the cached stats`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
        val underTest = createUnderTest()
        startCollecting(underTest)

        val actual = underTest.current()
        assertThat(actual.syncs.single().stats?.pendingItems).isEqualTo(5)
        assertThat(actual.syncs.single().runningSinceMillis).isNull()
        assertThat(actual.progress.pendingItems).isEqualTo(5)
    }

    @Test
    fun `test that monitorSyncRunSnapshot ignores cached stats of syncs that are not running`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(2L, runState = suspended)))
            stubCachedStats(mockMegaSyncStats(2L, uploads = 5))
            val underTest = createUnderTest()
            startCollecting(underTest)

            assertThat(underTest.current().syncs.single().stats).isNull()
        }

    @Test
    fun `test that monitorSyncRunSnapshot advances the episode when stats arrive`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stats(1L, uploads = 4))
        runCurrent()

        assertThat(underTest.current().progress.processedItems).isEqualTo(6)
    }

    @Test
    fun `test that monitorSyncRunSnapshot re-reads the sync list when stats arrive for an unknown sync`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            verify(syncGateway, times(2)).getFolderPairs()
            val second = underTest.current().syncs.single { it.syncId == 2L }
            assertThat(second.stats?.pendingUploads).isEqualTo(3)
        }

    @Test
    fun `test that monitorSyncRunSnapshot does not re-read the sync list for stats of a known sync`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stats(1L, uploads = 3))
            runCurrent()

            verify(syncGateway, times(1)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncRunSnapshot does not read cached stats after the seed`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
        emit(stats(2L, uploads = 3))
        runCurrent()

        verify(syncGateway, times(2)).getFolderPairs()
        verify(syncStatsCacheGateway, times(1)).getAllSyncStats()
    }

    @Test
    fun `test that monitorSyncRunSnapshot keeps the stats of a running sync when the sync list is re-read`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
            val underTest = createUnderTest()
            startCollecting(underTest)
            val before = underTest.current().syncs.single()

            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            val first = underTest.current().syncs.single { it.syncId == 1L }
            assertThat(first.stats).isEqualTo(before.stats)
            assertThat(first.stats?.pendingUploads).isEqualTo(5)
        }

    @Test
    fun `test that monitorSyncRunSnapshot ignores stats of a sync that is not running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
        val underTest = createUnderTest()
        startCollecting(underTest)
        val before = underTest.current()

        emit(stats(1L, uploads = 3))
        runCurrent()

        val actual = underTest.current()
        assertThat(actual.syncs.single().stats).isNull()
        assertThat(actual.progress).isEqualTo(before.progress)
    }

    @Test
    fun `test that monitorSyncRunSnapshot advances the episode on an event that does not change the syncs`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
            val underTest = createUnderTest()
            startCollecting(underTest)
            emit(stats(1L, uploads = 6))
            now = 1_000L
            emit(stateChanged(1L, suspended))
            runCurrent()
            assertThat(underTest.current().progress.interruptedItems).isEqualTo(6)

            now = 1_000L + interruptionHoldMillis
            emit(stats(1L, uploads = 6))
            runCurrent()

            assertThat(underTest.current().progress.interruptedItems).isEqualTo(0)
        }

    @Test
    fun `test that monitorSyncRunSnapshot drops the stats of a sync that leaves running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stateChanged(1L, suspended))
        runCurrent()

        val actual = underTest.current().syncs.single()
        assertThat(actual.runState).isEqualTo(SyncRunState.Suspended)
        assertThat(actual.stats).isNull()
        assertThat(actual.runningSinceMillis).isNull()
    }

    @Test
    fun `test that monitorSyncRunSnapshot records when a sync enters running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        now = 1_000L
        emit(stateChanged(1L, running))
        runCurrent()

        val actual = underTest.current().syncs.single()
        assertThat(actual.runState).isEqualTo(SyncRunState.Running)
        assertThat(actual.runningSinceMillis).isEqualTo(1_000L)
        assertThat(actual.stats).isNull()
    }

    @Test
    fun `test that monitorSyncRunSnapshot keeps the running since time while the sync stays running`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
            val underTest = createUnderTest()
            startCollecting(underTest)
            now = 1_000L
            emit(stateChanged(1L, running))
            emit(stats(1L, uploads = 4))
            runCurrent()
            val before = underTest.current().syncs.single()

            now = 2_000L
            emit(stateChanged(1L, running))
            runCurrent()

            val actual = underTest.current().syncs.single()
            assertThat(actual.runningSinceMillis).isEqualTo(1_000L)
            assertThat(actual.stats).isEqualTo(before.stats)
            assertThat(actual.stats?.pendingUploads).isEqualTo(4)
        }

    @Test
    fun `test that monitorSyncRunSnapshot adds a sync reported by a state change it did not know`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stateChanged(2L, running))
            runCurrent()

            assertThat(underTest.current().syncs.map { it.syncId }).containsExactly(2L)
            verify(syncGateway, times(1)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncRunSnapshot lists the syncs sorted by id`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(3L), mockMegaSync(1L)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stateChanged(2L, running))
        runCurrent()

        assertThat(underTest.current().syncs.map { it.syncId })
            .containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `test that monitorSyncRunSnapshot gives a running sync first found by a re-read the current time as running since`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            now = 5_000L
            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            val syncs = underTest.current().syncs
            assertThat(syncs.single { it.syncId == 2L }.runningSinceMillis).isEqualTo(5_000L)
            assertThat(syncs.single { it.syncId == 1L }.runningSinceMillis).isNull()
        }

    @Test
    fun `test that monitorSyncRunSnapshot removes a deleted sync`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(MegaSyncListenerEvent.OnSyncDeleted(mockMegaSync(1L)))
        runCurrent()

        assertThat(underTest.current().syncs.map { it.syncId }).containsExactly(2L)
    }

    @Test
    fun `test that monitorSyncRunSnapshot marks a sync over storage quota from its error`() = runTest {
        whenever(syncErrorMapper(any())).thenReturn(SyncError.STORAGE_OVERQUOTA)
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, error = 22)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        assertThat(underTest.current().syncs.single().isStorageOverQuota).isTrue()
    }

    @Test
    fun `test that monitorSyncRunSnapshot reports the error of a sync from its state change`() =
        runTest {
            whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
                .thenReturn(SyncError.UNKNOWN_ERROR)
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)
            assertThat(underTest.current().syncs.single().hasError).isFalse()

            emit(stateChanged(1L, disabled, error = MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            runCurrent()
            val withError = underTest.current().syncs.single()
            assertThat(withError.runState).isEqualTo(SyncRunState.Disabled)
            assertThat(withError.hasError).isTrue()

            emit(stateChanged(1L, disabled, error = MegaSync.Error.NO_SYNC_ERROR.swigValue()))
            runCurrent()
            assertThat(underTest.current().syncs.single().hasError).isFalse()
        }

    @Test
    fun `test that monitorSyncRunSnapshot emits when only the error of a sync changes`() = runTest {
        whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            .thenReturn(SyncError.UNKNOWN_ERROR)
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
        val underTest = createUnderTest()
        startCollecting(underTest)

        underTest.monitorSyncRunSnapshot().test {
            val seeded = awaitItem().syncs.single()

            emit(stateChanged(1L, running, error = MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            runCurrent()

            val actual = awaitItem().syncs.single()
            assertThat(actual.hasError).isTrue()
            assertThat(actual.stats).isEqualTo(seeded.stats)
            assertThat(actual.runningSinceMillis).isEqualTo(seeded.runningSinceMillis)
            expectNoEvents()
        }
    }

    @Test
    fun `test that monitorSyncRunSnapshot reports every sync suspended after all syncs are paused`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 3), mockMegaSyncStats(2L, uploads = 4))
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stateChanged(1L, suspended))
            emit(stateChanged(2L, suspended))
            runCurrent()

            val actual = underTest.current()
            assertThat(actual.syncs.map { it.runState })
                .containsExactly(SyncRunState.Suspended, SyncRunState.Suspended)
            assertThat(actual.syncs.all { it.stats == null && !it.hasError }).isTrue()
            assertThat(actual.progress.interruptedItems).isEqualTo(7)
        }

    @Test
    fun `test that monitorSyncRunSnapshot stays not seeded when the sync list read fails`() = runTest {
        syncGateway.stub { onBlocking { getFolderPairs() }.thenThrow(RuntimeException("read failed")) }
        val underTest = createUnderTest()
        startCollecting(underTest)

        assertThat(underTest.current()).isEqualTo(SyncRunSnapshot.NotSeeded)
        assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
    }

    @Test
    fun `test that monitorSyncRunSnapshot retries the seed read on the next event when not seeded`() =
        runTest {
            syncGateway.stub {
                onBlocking { getFolderPairs() }
                    .thenThrow(RuntimeException("read failed"))
                    .thenReturn(mockMegaSyncList(mockMegaSync(1L)))
            }
            val underTest = createUnderTest()
            startCollecting(underTest)
            assertThat(underTest.current().isSeeded).isFalse()

            emit(stats(1L, uploads = 2))
            runCurrent()

            val actual = underTest.current()
            assertThat(actual.isSeeded).isTrue()
            assertThat(actual.syncs.map { it.syncId }).containsExactly(1L)
        }

    @Test
    fun `test that monitorSyncRunSnapshot shares one episode baseline across collectors`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        underTest.monitorSyncRunSnapshot().test {
            assertThat(awaitItem().isSeeded).isTrue()
            emit(stats(1L, uploads = 4))
            runCurrent()
            assertThat(expectMostRecentItem().progress.processedItems).isEqualTo(6)

            underTest.monitorSyncRunSnapshot().test {
                assertThat(awaitItem().progress.processedItems).isEqualTo(6)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(underTest.monitorSyncRunSnapshot())
                .isSameInstanceAs(underTest.monitorSyncRunSnapshot())
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncRunSnapshot does not emit when an event changes nothing`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        underTest.monitorSyncRunSnapshot().test {
            assertThat(awaitItem().isSeeded).isTrue()
            emit(stats(1L, uploads = 6))
            runCurrent()
            assertThat(awaitItem().progress.fraction).isWithin(0.0001f).of(0.4f)

            emit(stats(1L, uploads = 6))
            runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `test that resetSyncRunSnapshot returns an empty episode`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)
        emit(stats(1L, uploads = 4))
        runCurrent()
        assertThat(underTest.current().progress.processedItems).isEqualTo(6)

        underTest.resetSyncRunSnapshot()
        runCurrent()

        val actual = underTest.current().progress
        assertThat(actual.processedItems).isEqualTo(0)
        assertThat(actual.episodeId).isEqualTo(0)
    }

    @Test
    fun `test that resetSyncRunSnapshot re-reads the sync list`() = runTest {
        val underTest = createUnderTest()
        startCollecting(underTest)

        underTest.resetSyncRunSnapshot()
        runCurrent()

        verify(syncGateway, times(2)).getFolderPairs()
        assertThat(underTest.current().isSeeded).isTrue()
    }

    @Test
    fun `test that resetSyncRunSnapshot called twice in a row leaves one seeded initial episode`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
            val underTest = createUnderTest()
            startCollecting(underTest)
            emit(stats(1L, uploads = 4))
            runCurrent()

            underTest.resetSyncRunSnapshot()
            underTest.resetSyncRunSnapshot()
            runCurrent()

            val actual = underTest.current()
            assertThat(actual.isSeeded).isTrue()
            assertThat(actual.progress.episodeId).isEqualTo(0)
            assertThat(actual.progress.processedItems).isEqualTo(0)
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
            verify(syncGateway, times(3)).getFolderPairs()
        }

    @Test
    fun `test that resetSyncRunSnapshot while no account is logged in seeds an empty run snapshot`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
            val underTest = createUnderTest()
            startCollecting(underTest)

            stubSyncList(mockMegaSyncList())
            underTest.resetSyncRunSnapshot()
            runCurrent()

            val actual = underTest.current()
            assertThat(actual.isSeeded).isTrue()
            assertThat(actual.syncs).isEmpty()
            assertThat(actual.progress.episodeId).isEqualTo(0)
        }
}
