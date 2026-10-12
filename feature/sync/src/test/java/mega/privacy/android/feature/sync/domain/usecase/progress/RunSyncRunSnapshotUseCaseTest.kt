package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkStatesUpdate
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RunSyncRunSnapshotUseCaseTest {
    private lateinit var underTest: RunSyncRunSnapshotUseCase

    private val syncProgressRepository = mock<SyncProgressRepository>()

    private val published = mutableListOf<Published>()
    private var clock: () -> Long = { 0L }

    private val interruptionHoldMillis =
        AdvanceSyncProgressEpisodeUseCase.INTERRUPTION_HOLD.inWholeMilliseconds

    @BeforeAll
    fun setUp() {
        underTest = RunSyncRunSnapshotUseCase(
            syncProgressRepository = syncProgressRepository,
            advanceSyncProgressEpisodeUseCase = AdvanceSyncProgressEpisodeUseCase(),
            currentTimeProvider = { clock() },
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncProgressRepository)
        published.clear()
        clock = { 0L }
        syncProgressRepository.stub {
            onBlocking { publishSyncRunSnapshot(any(), any()) } doAnswer { invocation ->
                published += Published(invocation.getArgument(0), invocation.getArgument(1))
                Unit
            }
        }
    }

    @Test
    fun `test that invoke publishes a seeded snapshot of the syncs sorted by id`() = runTest {
        stubUpdates(SyncWorkStatesUpdate(0L, listOf(running(3L, 0), suspended(1L))))

        underTest()

        val snapshot = published.single().snapshot
        assertThat(snapshot.isSeeded).isTrue()
        assertThat(snapshot.syncs.map { it.syncId }).containsExactly(1L, 3L).inOrder()
    }

    @Test
    fun `test that invoke advances the episode on each update`() = runTest {
        stubUpdates(
            SyncWorkStatesUpdate(0L, listOf(running(1L, 10))),
            SyncWorkStatesUpdate(0L, listOf(running(1L, 4))),
        )

        underTest()

        assertThat(published[0].snapshot.progress.pendingItems).isEqualTo(10)
        val progress = published.last().snapshot.progress
        assertThat(progress.processedItems).isEqualTo(6)
        assertThat(progress.pendingItems).isEqualTo(4)
    }

    @Test
    fun `test that invoke advances the episode on an update that does not change the syncs`() =
        runTest {
            var now = 0L
            clock = { now }
            whenever(syncProgressRepository.monitorSyncWorkStates()).thenReturn(
                flow {
                    emit(SyncWorkStatesUpdate(0L, listOf(running(1L, 10))))
                    emit(SyncWorkStatesUpdate(0L, listOf(running(1L, 6))))
                    now = 1_000L
                    emit(SyncWorkStatesUpdate(0L, listOf(suspended(1L))))
                    now = 1_000L + interruptionHoldMillis
                    emit(SyncWorkStatesUpdate(0L, listOf(suspended(1L))))
                }
            )

            underTest()

            assertThat(published[2].snapshot.progress.interruptedItems).isEqualTo(6)
            assertThat(published[3].snapshot.progress.interruptedItems).isEqualTo(0)
        }

    @Test
    fun `test that invoke publishes each snapshot with the generation of its update`() = runTest {
        stubUpdates(
            SyncWorkStatesUpdate(3L, listOf(running(1L, 10))),
            SyncWorkStatesUpdate(3L, listOf(running(1L, 4))),
            SyncWorkStatesUpdate(4L, listOf(running(1L, 4))),
        )

        underTest()

        assertThat(published.map { it.generation }).containsExactly(3L, 3L, 4L).inOrder()
    }

    @Test
    fun `test that invoke restarts the episode when the generation changes`() = runTest {
        stubUpdates(
            SyncWorkStatesUpdate(0L, listOf(running(1L, 10))),
            SyncWorkStatesUpdate(0L, listOf(running(1L, 4))),
            SyncWorkStatesUpdate(1L, listOf(running(1L, 4))),
        )

        underTest()

        assertThat(published[1].snapshot.progress.processedItems).isEqualTo(6)
        val progress = published.last().snapshot.progress
        assertThat(progress.processedItems).isEqualTo(0)
        assertThat(progress.pendingItems).isEqualTo(4)
        assertThat(progress.episodeId).isEqualTo(0L)
    }

    @Test
    fun `test that invoke drops an update of an older generation`() = runTest {
        stubUpdates(
            SyncWorkStatesUpdate(1L, listOf(running(1L, 10))),
            SyncWorkStatesUpdate(0L, listOf(running(1L, 4))),
        )

        underTest()

        assertThat(published).hasSize(1)
        assertThat(published.single().snapshot.progress.pendingItems).isEqualTo(10)
    }

    @Test
    fun `test that invoke skips an update whose clock read throws and keeps the episode`() =
        runTest {
            var calls = 0
            clock = {
                calls++
                if (calls == 2) throw IllegalStateException("clock failed")
                0L
            }
            stubUpdates(
                SyncWorkStatesUpdate(0L, listOf(running(1L, 10))),
                SyncWorkStatesUpdate(0L, listOf(running(1L, 4))),
                SyncWorkStatesUpdate(0L, listOf(running(1L, 8))),
            )

            underTest()

            assertThat(published).hasSize(2)
            assertThat(published.last().snapshot.progress.processedItems).isEqualTo(2)
        }

    @Test
    fun `test that invoke rethrows a cancellation from the fold`() = runTest {
        clock = { throw CancellationException("cancelled") }
        stubUpdates(SyncWorkStatesUpdate(0L, listOf(running(1L, 10))))

        val actual = runCatching { underTest() }.exceptionOrNull()

        assertThat(actual).isInstanceOf(CancellationException::class.java)
        assertThat(published).isEmpty()
    }

    @Test
    fun `test that invoke propagates a failure of the work states flow`() = runTest {
        whenever(syncProgressRepository.monitorSyncWorkStates()).thenReturn(
            flow {
                emit(SyncWorkStatesUpdate(0L, listOf(running(1L, 10))))
                throw IOException("work states failed")
            }
        )

        val actual = runCatching { underTest() }.exceptionOrNull()

        assertThat(actual).isInstanceOf(IOException::class.java)
        assertThat(published).hasSize(1)
    }

    @Test
    fun `test that invoke stops when it is cancelled`() = runTest {
        whenever(syncProgressRepository.monitorSyncWorkStates()).thenReturn(
            flow {
                emit(SyncWorkStatesUpdate(0L, listOf(running(1L, 10))))
                awaitCancellation()
            }
        )
        val job = launch { underTest() }
        runCurrent()

        job.cancel()
        runCurrent()

        assertThat(job.isCancelled).isTrue()
        assertThat(job.isCompleted).isTrue()
        assertThat(published).hasSize(1)
    }

    private fun stubUpdates(vararg updates: SyncWorkStatesUpdate) {
        whenever(syncProgressRepository.monitorSyncWorkStates()).thenReturn(flowOf(*updates))
    }

    private fun running(syncId: Long, pendingUploads: Int) = SyncWorkState(
        syncId = syncId,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = SyncStatsSample(
            isScanning = false,
            isSyncing = pendingUploads > 0,
            pendingUploads = pendingUploads,
            pendingDownloads = 0,
        ),
        runningSinceMillis = 0L,
    )

    private fun suspended(syncId: Long) = SyncWorkState(
        syncId = syncId,
        runState = SyncRunState.Suspended,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = null,
    )

    private data class Published(val snapshot: SyncRunSnapshot, val generation: Long)
}
