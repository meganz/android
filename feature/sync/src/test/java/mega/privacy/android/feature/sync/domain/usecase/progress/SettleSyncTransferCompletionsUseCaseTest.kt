package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferBatchProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition
import mega.privacy.android.feature.sync.domain.entity.progress.batchProgress
import mega.privacy.android.feature.sync.domain.usecase.progress.SettleSyncTransferCompletionsUseCase.Completion
import mega.privacy.android.feature.sync.domain.usecase.progress.SettleSyncTransferCompletionsUseCase.Emission
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettleSyncTransferCompletionsUseCaseTest {

    private lateinit var underTest: SettleSyncTransferCompletionsUseCase

    private val applySyncTransferCountersUseCase = ApplySyncTransferCountersUseCase()

    @BeforeAll
    fun setUp() {
        underTest = SettleSyncTransferCompletionsUseCase(applySyncTransferCountersUseCase)
    }

    @Test
    fun `test that invoke returns the same state for an empty journal`() {
        val state = stateOf(countsAfter(admitted(n = 1L)))

        val settled = underTest(state, emptyList(), nowMillis = 0L, completedLimit = 2)

        assertThat(settled).isSameInstanceAs(state)
    }

    @Test
    fun `test that invoke keeps the completed list instance when nothing finished`() {
        val completed = listOf(SyncCompletedTransfer(entry(1, 1L), SyncMediaType.Image, 0L, 0L))
        val state = stateOf(SyncTransferCounts.Empty, completed)

        val settled = underTest(state, listOf(Emission(admitted(n = 1L))), nowMillis = 0L, completedLimit = 2)

        assertThat(settled.completed).isSameInstanceAs(state.completed)
        assertThat(settled.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
    }

    @Test
    fun `test that invoke gives a completed row the batch it finished in`() {
        val state = stateOf(countsAfter(admitted(n = 1L)))

        val settled = underTest(state, listOf(completion(1, n = 2L)), nowMillis = 1_000L, completedLimit = 2)

        val row = settled.completed.single()
        assertThat(row.entry.tag).isEqualTo(1)
        assertThat(row.batchId).isEqualTo(0L)
        assertThat(row.finishedAtMillis).isEqualTo(1_000L)
        assertThat(settled.counts.batchId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke applies the journal in notification number order`() {
        val state = stateOf(countsAfter(admitted(n = 5L)))

        val settled = underTest(
            state,
            listOf(completion(2, n = 7L), Emission(admitted(n = 6L))),
            nowMillis = 0L,
            completedLimit = 2,
        )

        assertThat(settled.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 2, total = 2))
        assertThat(settled.counts.batchId).isEqualTo(0L)
        assertThat(settled.completed.single().batchId).isEqualTo(0L)
    }

    @Test
    fun `test that invoke puts new rows first newest first and cuts the list at the limit`() {
        val counts = countsAfter(
            admitted(n = 1L),
            SyncTransferTransition.Completed(SyncTransferDirection.Upload, 2L),
            admitted(n = 3L),
            admitted(n = 4L),
        )
        val old = SyncCompletedTransfer(entry(10, n = 2L), SyncMediaType.Image, finishedAtMillis = 0L, batchId = 0L)

        val settled = underTest(
            stateOf(counts, listOf(old)),
            listOf(completion(1, n = 5L), completion(2, n = 6L)),
            nowMillis = 0L,
            completedLimit = 2,
        )

        assertThat(settled.completed.map { it.entry.tag }).containsExactly(2, 1).inOrder()
        assertThat(settled.counts.completedTransfers).isEqualTo(2)
        assertThat(settled.counts.batchEvictedUploads).isEqualTo(0)
    }

    @Test
    fun `test that invoke tallies an evicted row that finished in the current batch`() {
        val state = stateOf(countsAfter(admitted(n = 1L), admitted(n = 2L), admitted(n = 3L)))

        val settled = underTest(
            state,
            listOf(completion(1, n = 4L), completion(2, n = 5L)),
            nowMillis = 0L,
            completedLimit = 1,
        )

        assertThat(settled.completed.map { it.entry.tag }).containsExactly(2)
        assertThat(settled.counts.batchEvictedUploads).isEqualTo(1)
        assertThat(settled.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 2, pending = 1))
        assertThat(settled.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 3, total = 3))
    }

    private fun stateOf(counts: SyncTransferCounts, completed: List<SyncCompletedTransfer> = emptyList()) =
        SyncTransferStoreState.NotSeeded.copy(counts = counts, completed = completed.toPersistentList())

    private fun countsAfter(vararg transitions: SyncTransferTransition): SyncTransferCounts =
        transitions.fold(SyncTransferCounts.Empty) { counts, transition ->
            applySyncTransferCountersUseCase(counts, listOf(transition))
        }

    private fun admitted(n: Long) =
        SyncTransferTransition.Admitted(SyncTransferDirection.Upload, SyncMediaType.Image, n)

    private fun completion(tag: Int, n: Long) = Emission(
        SyncTransferTransition.Completed(SyncTransferDirection.Upload, n),
        Completion(entry(tag, n), SyncMediaType.Image),
    )

    private fun entry(tag: Int, n: Long) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = TransferState.STATE_COMPLETED,
        fileName = "f$tag.jpg",
        totalBytes = 100L,
        transferredBytes = 100L,
        speed = 0L,
        meanSpeed = 0L,
        priority = tag.toLong(),
        notificationNumber = n,
        lastError = null,
        isForeignOverQuota = false,
        path = "/sync/f$tag.jpg",
    )
}
