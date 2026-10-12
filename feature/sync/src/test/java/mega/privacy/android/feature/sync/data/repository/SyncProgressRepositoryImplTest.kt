package mega.privacy.android.feature.sync.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkStatesUpdate
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
    private var listenerSubscriptions = 0

    private val running = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
    private val suspended = MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue()
    private val disabled = MegaSync.SyncRunningState.RUNSTATE_DISABLED.swigValue()

    private val updates = mutableListOf<SyncWorkStatesUpdate>()
    private val seededSnapshot = SyncRunSnapshot(
        isSeeded = true,
        syncs = emptyList(),
        progress = SyncEpisodeProgress.None.copy(pendingItems = 3),
    )

    @BeforeEach
    fun resetMocks() {
        reset(syncGateway, syncStatsCacheGateway, syncErrorMapper)
        syncUpdates = MutableSharedFlow(extraBufferCapacity = 64)
        updates.clear()
        now = 0L
        listenerSubscriptions = 0
        whenever(syncGateway.syncUpdate).thenReturn(
            flow {
                listenerSubscriptions++
                emitAll(syncUpdates)
            }
        )
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

    private fun TestScope.createUnderTest(
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ) = SyncProgressRepositoryImpl(
        syncGateway = syncGateway,
        syncStatsCacheGateway = syncStatsCacheGateway,
        syncWorkStateMapper = SyncWorkStateMapper(SyncRunStateMapper(), syncErrorMapper),
        syncStatsSampleMapper = SyncStatsSampleMapper(),
        currentTimeProvider = { now },
        ioDispatcher = dispatcher,
    )

    private fun TestScope.startCollecting(underTest: SyncProgressRepositoryImpl) {
        backgroundScope.launch { underTest.monitorSyncWorkStates().toList(updates) }
        runCurrent()
    }

    private fun current(): List<SyncWorkState> = updates.last().syncs

    private suspend fun SyncProgressRepositoryImpl.snapshot(): SyncRunSnapshot =
        monitorSyncRunSnapshot().first()

    private suspend fun emit(event: MegaSyncListenerEvent) = syncUpdates.emit(event)

    private fun stats(backupId: Long, uploads: Int) =
        MegaSyncListenerEvent.OnSyncStatsUpdated(mockMegaSyncStats(backupId, uploads = uploads))

    private fun stateChanged(backupId: Long, runState: Int, error: Int = 0) =
        MegaSyncListenerEvent.OnSyncStateChanged(mockMegaSync(backupId, runState, error))

    @Test
    fun `test that building the repository does not start the sync listener collection`() =
        runTest {
            val underTest = createUnderTest()
            underTest.monitorSyncRunSnapshot()
            underTest.monitorSyncWorkStates()
            runCurrent()

            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(0)
            verify(syncGateway, never()).getFolderPairs()
            verifyNoInteractions(syncStatsCacheGateway)
        }

    @Test
    fun `test that resetSyncRunSnapshot before the first collection does not start the sync listener collection`() =
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
    fun `test that monitorSyncRunSnapshot returns the same StateFlow to every caller`() = runTest {
        val underTest = createUnderTest()

        assertThat(underTest.monitorSyncRunSnapshot())
            .isSameInstanceAs(underTest.monitorSyncRunSnapshot())
    }

    @Test
    fun `test that monitorSyncRunSnapshot emits NotSeeded before anything is published`() =
        runTest {
            val underTest = createUnderTest()

            assertThat(underTest.snapshot()).isEqualTo(SyncRunSnapshot.NotSeeded)
        }

    @Test
    fun `test that publishSyncRunSnapshot publishes a snapshot of the current generation`() =
        runTest {
            val underTest = createUnderTest()

            underTest.publishSyncRunSnapshot(seededSnapshot, generation = 0L)

            assertThat(underTest.snapshot()).isEqualTo(seededSnapshot)
        }

    @Test
    fun `test that publishSyncRunSnapshot drops a snapshot of an older generation`() = runTest {
        val underTest = createUnderTest()
        underTest.resetSyncRunSnapshot()

        underTest.publishSyncRunSnapshot(seededSnapshot, generation = 0L)

        assertThat(underTest.snapshot()).isEqualTo(SyncRunSnapshot.NotSeeded)
    }

    @Test
    fun `test that publishSyncRunSnapshot does not emit an equal snapshot again`() = runTest {
        val underTest = createUnderTest()
        underTest.publishSyncRunSnapshot(seededSnapshot, generation = 0L)

        underTest.monitorSyncRunSnapshot().test {
            assertThat(awaitItem()).isEqualTo(seededSnapshot)

            underTest.publishSyncRunSnapshot(seededSnapshot.copy(), generation = 0L)

            expectNoEvents()
        }
    }

    @Test
    fun `test that a publish racing a reset never leaves a snapshot of the older generation`() =
        runTest {
            repeat(RACE_ROUNDS) {
                val underTest = createUnderTest()

                withContext(Dispatchers.Default) {
                    coroutineScope {
                        launch { underTest.publishSyncRunSnapshot(seededSnapshot, generation = 0L) }
                        launch { underTest.resetSyncRunSnapshot() }
                    }
                }

                assertThat(underTest.snapshot()).isEqualTo(SyncRunSnapshot.NotSeeded)
            }
        }

    @Test
    fun `test that resetSyncRunSnapshot publishes NotSeeded at once without a collector`() =
        runTest {
            val underTest = createUnderTest()
            underTest.publishSyncRunSnapshot(seededSnapshot, generation = 0L)

            underTest.monitorSyncRunSnapshot().test {
                assertThat(awaitItem()).isEqualTo(seededSnapshot)

                underTest.resetSyncRunSnapshot()

                assertThat(awaitItem()).isEqualTo(SyncRunSnapshot.NotSeeded)
            }
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(0)
        }

    @Test
    fun `test that monitorSyncWorkStates subscribes to the sync listener before the sync list is read`() =
        runTest {
            val gate = CompletableDeferred<MegaSyncList>()
            var subscriptionsAtRead = -1
            syncGateway.stub {
                onBlocking { getFolderPairs() } doSuspendableAnswer {
                    subscriptionsAtRead = syncUpdates.subscriptionCount.value
                    gate.await()
                }
            }
            val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
            backgroundScope.launch { underTest.monitorSyncWorkStates().toList(updates) }
            runCurrent()
            assertThat(subscriptionsAtRead).isEqualTo(1)

            emit(stateChanged(2L, running))
            gate.complete(mockMegaSyncList(mockMegaSync(1L)))
            runCurrent()

            assertThat(current().map { it.syncId }).containsExactly(1L, 2L)
        }

    @Test
    fun `test that monitorSyncWorkStates sends no syncs when the sync list is empty`() = runTest {
        val underTest = createUnderTest()
        startCollecting(underTest)

        assertThat(updates).containsExactly(SyncWorkStatesUpdate(0L, emptyList()))
    }

    @Test
    fun `test that monitorSyncWorkStates seeds running syncs from the cached stats`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
        val underTest = createUnderTest()
        startCollecting(underTest)

        val actual = current().single()
        assertThat(actual.stats?.pendingItems).isEqualTo(5)
        assertThat(actual.runningSinceMillis).isNull()
    }

    @Test
    fun `test that monitorSyncWorkStates ignores cached stats of syncs that are not running`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(2L, runState = suspended)))
            stubCachedStats(mockMegaSyncStats(2L, uploads = 5))
            val underTest = createUnderTest()
            startCollecting(underTest)

            assertThat(current().single().stats).isNull()
        }

    @Test
    fun `test that monitorSyncWorkStates applies the stats of a running sync`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stats(1L, uploads = 4))
        runCurrent()

        assertThat(updates).hasSize(2)
        assertThat(current().single().stats?.pendingItems).isEqualTo(4)
    }

    @Test
    fun `test that monitorSyncWorkStates re-reads the sync list when stats arrive for an unknown sync`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            verify(syncGateway, times(2)).getFolderPairs()
            val second = current().single { it.syncId == 2L }
            assertThat(second.stats?.pendingUploads).isEqualTo(3)
        }

    @Test
    fun `test that monitorSyncWorkStates does not re-read the sync list for stats of a known sync`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stats(1L, uploads = 3))
            runCurrent()

            verify(syncGateway, times(1)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncWorkStates does not read cached stats after the seed`() = runTest {
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
    fun `test that monitorSyncWorkStates keeps the stats of a running sync when the sync list is re-read`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
            val underTest = createUnderTest()
            startCollecting(underTest)
            val before = current().single()

            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            val first = current().single { it.syncId == 1L }
            assertThat(first.stats).isEqualTo(before.stats)
            assertThat(first.stats?.pendingUploads).isEqualTo(5)
        }

    @Test
    fun `test that monitorSyncWorkStates ignores stats of a sync that is not running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stats(1L, uploads = 3))
        runCurrent()

        assertThat(current().single().stats).isNull()
    }

    @Test
    fun `test that monitorSyncWorkStates sends an update for an event that does not change the syncs`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(MegaSyncListenerEvent.OnGlobalSyncStateChanged)
            runCurrent()

            assertThat(updates).hasSize(2)
            assertThat(updates[1]).isEqualTo(updates[0])
        }

    @Test
    fun `test that monitorSyncWorkStates drops the stats of a sync that leaves running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(stateChanged(1L, suspended))
        runCurrent()

        val actual = current().single()
        assertThat(actual.runState).isEqualTo(SyncRunState.Suspended)
        assertThat(actual.stats).isNull()
        assertThat(actual.runningSinceMillis).isNull()
    }

    @Test
    fun `test that monitorSyncWorkStates records when a sync enters running`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)

        now = 1_000L
        emit(stateChanged(1L, running))
        runCurrent()

        val actual = current().single()
        assertThat(actual.runState).isEqualTo(SyncRunState.Running)
        assertThat(actual.runningSinceMillis).isEqualTo(1_000L)
        assertThat(actual.stats).isNull()
    }

    @Test
    fun `test that monitorSyncWorkStates keeps the running since time while the sync stays running`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L, runState = suspended)))
            val underTest = createUnderTest()
            startCollecting(underTest)
            now = 1_000L
            emit(stateChanged(1L, running))
            emit(stats(1L, uploads = 4))
            runCurrent()
            val before = current().single()

            now = 2_000L
            emit(stateChanged(1L, running))
            runCurrent()

            val actual = current().single()
            assertThat(actual.runningSinceMillis).isEqualTo(1_000L)
            assertThat(actual.stats).isEqualTo(before.stats)
            assertThat(actual.stats?.pendingUploads).isEqualTo(4)
        }

    @Test
    fun `test that monitorSyncWorkStates adds a sync reported by a state change it did not know`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stateChanged(2L, running))
            runCurrent()

            assertThat(current().map { it.syncId }).containsExactly(2L)
            verify(syncGateway, times(1)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncWorkStates gives a running sync first found by a re-read the current time as running since`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            now = 5_000L
            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            emit(stats(2L, uploads = 3))
            runCurrent()

            val syncs = current()
            assertThat(syncs.single { it.syncId == 2L }.runningSinceMillis).isEqualTo(5_000L)
            assertThat(syncs.single { it.syncId == 1L }.runningSinceMillis).isNull()
        }

    @Test
    fun `test that monitorSyncWorkStates removes a deleted sync`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        emit(MegaSyncListenerEvent.OnSyncDeleted(mockMegaSync(1L)))
        runCurrent()

        assertThat(current().map { it.syncId }).containsExactly(2L)
    }

    @Test
    fun `test that monitorSyncWorkStates marks a sync over storage quota from its error`() = runTest {
        whenever(syncErrorMapper(any())).thenReturn(SyncError.STORAGE_OVERQUOTA)
        stubSyncList(mockMegaSyncList(mockMegaSync(1L, error = 22)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        assertThat(current().single().isStorageOverQuota).isTrue()
    }

    @Test
    fun `test that monitorSyncWorkStates reports the error of a sync from its state change`() =
        runTest {
            whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
                .thenReturn(SyncError.UNKNOWN_ERROR)
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)
            assertThat(current().single().hasError).isFalse()

            emit(stateChanged(1L, disabled, error = MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            runCurrent()
            val withError = current().single()
            assertThat(withError.runState).isEqualTo(SyncRunState.Disabled)
            assertThat(withError.hasError).isTrue()

            emit(stateChanged(1L, disabled, error = MegaSync.Error.NO_SYNC_ERROR.swigValue()))
            runCurrent()
            assertThat(current().single().hasError).isFalse()
        }

    @Test
    fun `test that monitorSyncWorkStates keeps the stats when only the error of a sync changes`() =
        runTest {
            whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
                .thenReturn(SyncError.UNKNOWN_ERROR)
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 5))
            val underTest = createUnderTest()
            startCollecting(underTest)
            val seeded = current().single()

            emit(stateChanged(1L, running, error = MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            runCurrent()

            val actual = current().single()
            assertThat(actual.hasError).isTrue()
            assertThat(actual.stats).isEqualTo(seeded.stats)
            assertThat(actual.runningSinceMillis).isEqualTo(seeded.runningSinceMillis)
        }

    @Test
    fun `test that monitorSyncWorkStates reports every sync suspended after all syncs are paused`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L), mockMegaSync(2L)))
            stubCachedStats(mockMegaSyncStats(1L, uploads = 3), mockMegaSyncStats(2L, uploads = 4))
            val underTest = createUnderTest()
            startCollecting(underTest)

            emit(stateChanged(1L, suspended))
            emit(stateChanged(2L, suspended))
            runCurrent()

            val actual = current()
            assertThat(actual.map { it.runState })
                .containsExactly(SyncRunState.Suspended, SyncRunState.Suspended)
            assertThat(actual.all { it.stats == null && !it.hasError }).isTrue()
        }

    @Test
    fun `test that monitorSyncWorkStates sends nothing when the sync list read fails`() = runTest {
        syncGateway.stub { onBlocking { getFolderPairs() }.thenThrow(RuntimeException("read failed")) }
        val underTest = createUnderTest()
        startCollecting(underTest)

        assertThat(updates).isEmpty()
        assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
    }

    @Test
    fun `test that monitorSyncWorkStates retries the seed read on the next event when not seeded`() =
        runTest {
            syncGateway.stub {
                onBlocking { getFolderPairs() }
                    .thenThrow(RuntimeException("read failed"))
                    .thenReturn(mockMegaSyncList(mockMegaSync(1L)))
            }
            val underTest = createUnderTest()
            startCollecting(underTest)
            assertThat(updates).isEmpty()

            emit(stats(1L, uploads = 2))
            runCurrent()

            assertThat(current().map { it.syncId }).containsExactly(1L)
        }

    @Test
    fun `test that monitorSyncWorkStates stamps each update with the current generation`() =
        runTest {
            val underTest = createUnderTest()
            underTest.resetSyncRunSnapshot()
            startCollecting(underTest)

            emit(stateChanged(1L, running))
            runCurrent()

            assertThat(updates.map { it.generation }).containsExactly(1L, 1L)
        }

    @Test
    fun `test that resetSyncRunSnapshot reads the sync list again in a new generation`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        val underTest = createUnderTest()
        startCollecting(underTest)

        stubSyncList(mockMegaSyncList(mockMegaSync(2L)))
        underTest.resetSyncRunSnapshot()
        runCurrent()

        verify(syncGateway, times(2)).getFolderPairs()
        assertThat(updates.last()).isEqualTo(SyncWorkStatesUpdate(1L, current()))
        assertThat(current().map { it.syncId }).containsExactly(2L)
        assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
    }

    @Test
    fun `test that resetSyncRunSnapshot reads the seed cached stats again`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        stubCachedStats(mockMegaSyncStats(1L, uploads = 10))
        val underTest = createUnderTest()
        startCollecting(underTest)
        emit(stats(1L, uploads = 4))
        runCurrent()

        underTest.resetSyncRunSnapshot()
        runCurrent()

        verify(syncStatsCacheGateway, times(2)).getAllSyncStats()
        assertThat(current().single().stats?.pendingUploads).isEqualTo(10)
    }

    @Test
    fun `test that monitorSyncWorkStates does not apply an event queued before a reset`() = runTest {
        val blockedRead = CompletableDeferred<MegaSyncList>()
        var reads = 0
        syncGateway.stub {
            onBlocking { getFolderPairs() } doSuspendableAnswer {
                reads++
                if (reads == 2) blockedRead.await() else mockMegaSyncList(mockMegaSync(1L))
            }
        }
        val underTest = createUnderTest()
        startCollecting(underTest)
        emit(stats(5L, uploads = 1))
        runCurrent()
        emit(stateChanged(9L, running))
        runCurrent()

        underTest.resetSyncRunSnapshot()
        runCurrent()

        val newGeneration = updates.filter { it.generation == 1L }
        assertThat(newGeneration).isNotEmpty()
        assertThat(newGeneration.flatMap { it.syncs }.map { it.syncId }).doesNotContain(9L)
        assertThat(current().map { it.syncId }).containsExactly(1L)
    }

    @Test
    fun `test that resetSyncRunSnapshot keeps one sync listener subscription across resets`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            underTest.resetSyncRunSnapshot()
            runCurrent()
            underTest.resetSyncRunSnapshot()
            runCurrent()

            assertThat(listenerSubscriptions).isEqualTo(1)
            assertThat(updates.last().generation).isEqualTo(2L)
            verify(syncGateway, times(3)).getFolderPairs()
        }

    @Test
    fun `test that monitorSyncWorkStates applies sync events after a reset`() = runTest {
        stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
        val underTest = createUnderTest()
        startCollecting(underTest)
        underTest.resetSyncRunSnapshot()
        runCurrent()

        emit(stateChanged(2L, running))
        runCurrent()

        assertThat(updates.last().generation).isEqualTo(1L)
        assertThat(current().map { it.syncId }).containsExactly(1L, 2L)
    }

    @Test
    fun `test that resetSyncRunSnapshot called twice in a row leaves one subscription in the latest generation`() =
        runTest {
            stubSyncList(mockMegaSyncList(mockMegaSync(1L)))
            val underTest = createUnderTest()
            startCollecting(underTest)

            underTest.resetSyncRunSnapshot()
            underTest.resetSyncRunSnapshot()
            runCurrent()

            assertThat(updates.last().generation).isEqualTo(2L)
            assertThat(syncUpdates.subscriptionCount.value).isEqualTo(1)
        }

    companion object {
        private const val RACE_ROUNDS = 1_000
    }
}
