package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentHashMap
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAvailableSyncTransferMovesUseCaseTest {

    private lateinit var underTest: GetAvailableSyncTransferMovesUseCase

    private val getSyncTransferListUseCase = GetSyncTransferListUseCase(
        GetSyncTransferRowStateUseCase(),
        GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase()),
        IsSyncTransferSearchMatchUseCase(),
    )

    @BeforeAll
    fun setUp() {
        underTest = GetAvailableSyncTransferMovesUseCase()
    }

    @Test
    fun `test that invoke hides top and up on the first displayed row`() {
        val displayed = threeQueued()
        val withRetrying = pending(retrying(R, 10), queued(Q, 20))

        assertThat(underTest(A, displayed))
            .containsExactly(SyncTransferMove.Down, SyncTransferMove.ToBottom)
        assertThat(underTest(R, withRetrying))
            .containsExactly(SyncTransferMove.Down, SyncTransferMove.ToBottom)
    }

    @Test
    fun `test that invoke hides down and bottom on the last displayed row`() {
        val displayed = threeQueued()

        assertThat(underTest(C, displayed))
            .containsExactly(SyncTransferMove.ToTop, SyncTransferMove.Up)
    }

    @Test
    fun `test that invoke offers every move in the middle of the band`() {
        val displayed = threeQueued()

        assertThat(underTest(B, displayed)).containsExactly(*ALL_MOVES)
    }

    @Test
    fun `test that invoke offers top and bottom to the first row of a lower band`() {
        val displayed = pending(queued(Q, 10), paused(P1, 20), paused(P2, 30))

        assertThat(underTest(P1, displayed)).containsExactly(
            SyncTransferMove.ToTop,
            SyncTransferMove.Down,
            SyncTransferMove.ToBottom,
        )
    }

    @Test
    fun `test that invoke offers bottom but not down to the last row of an upper band`() {
        val displayed = pending(queued(Q1, 10), queued(Q2, 20), paused(P, 30))

        assertThat(underTest(Q2, displayed)).containsExactly(
            SyncTransferMove.ToTop,
            SyncTransferMove.Up,
            SyncTransferMove.ToBottom,
        )
    }

    @Test
    fun `test that invoke offers no up or down without a same direction neighbour in the band`() {
        val displayed = pending(queued(U, 10), queued(D, 20, SyncTransferDirection.Download))

        assertThat(underTest(U, displayed)).containsExactly(SyncTransferMove.ToBottom)
        assertThat(underTest(D, displayed)).containsExactly(SyncTransferMove.ToTop)
    }

    @Test
    fun `test that invoke offers nothing when the list holds one row`() {
        val displayed = pending(queued(A, 10))

        assertThat(underTest(A, displayed)).isEmpty()
    }

    @Test
    fun `test that invoke offers nothing to a completing row`() {
        val displayed = pending(completing(C, 5), queued(Q, 10), queued(Q2, 20))

        assertThat(underTest(C, displayed)).isEmpty()
    }

    @Test
    fun `test that invoke counts a completing row as a displayed row`() {
        val displayed = pending(completing(C, 5), queued(Q1, 10), queued(Q2, 20))

        assertThat(underTest(Q1, displayed)).containsExactly(
            SyncTransferMove.ToTop,
            SyncTransferMove.Down,
            SyncTransferMove.ToBottom,
        )
    }

    @Test
    fun `test that invoke hides down but offers top and bottom to the last displayed row when more rows follow`() {
        val displayed = threeQueued()

        assertThat(underTest(C, displayed, totalCount = 10)).containsExactly(
            SyncTransferMove.ToTop,
            SyncTransferMove.Up,
            SyncTransferMove.ToBottom,
        )
    }

    @Test
    fun `test that invoke hides top only on the first displayed row when more rows follow`() {
        val displayed = threeQueued()

        assertThat(underTest(A, displayed, totalCount = 10))
            .containsExactly(SyncTransferMove.Down, SyncTransferMove.ToBottom)
        assertThat(underTest(B, displayed, totalCount = 10)).containsExactly(*ALL_MOVES)
    }

    @Test
    fun `test that invoke never returns a drop`() {
        val fixtures = listOf(
            threeQueued() to 3,
            threeQueued() to 10,
            pending(queued(Q, 10), paused(P1, 20), paused(P2, 30)) to 3,
            pending(queued(Q1, 10), queued(Q2, 20), paused(P, 30)) to 3,
            pending(queued(U, 10), queued(D, 20, SyncTransferDirection.Download)) to 2,
            pending(queued(A, 10)) to 1,
            pending(completing(C, 5), queued(Q1, 10), queued(Q2, 20)) to 3,
            pending(queued(A, 10), queued(B, 20)) + finished(F) to 3,
        )

        val moves = fixtures.flatMap { (displayed, totalCount) ->
            displayed.flatMap { underTest(it.tag, displayed, totalCount) }
        }

        assertThat(moves).isNotEmpty()
        assertThat(moves.filterIsInstance<SyncTransferMove.Drop>()).isEmpty()
    }

    @Test
    fun `test that invoke counts finished rows of a mixed list`() {
        val displayed = pending(queued(A, 10), queued(B, 20)) + finished(F)

        assertThat(underTest(B, displayed, totalCount = 3)).containsExactly(
            SyncTransferMove.ToTop,
            SyncTransferMove.Up,
            SyncTransferMove.ToBottom,
        )
    }

    private fun threeQueued() = pending(queued(A, 10), queued(B, 20), queued(C, 30))

    private fun pending(vararg rows: SyncTransferRow): List<SyncTransferListItem> =
        getSyncTransferListUseCase(
            state = SyncTransferStoreState.NotSeeded.copy(
                isSeeded = true,
                active = rows.associateBy { it.entry.tag }.toPersistentHashMap(),
            ),
            filter = SyncTransferFilter.Pending,
            context = SyncTransferRowContext(
                areTransfersPaused = false,
                isStorageOverQuota = false,
                isTransferOverQuota = false,
            ),
            limit = Int.MAX_VALUE,
        ).items

    private fun finished(tag: Int) = SyncTransferListItem.Finished(
        SyncCompletedTransfer(
            entry = entry(tag, 0, SyncTransferDirection.Upload, TransferState.STATE_COMPLETED),
            mediaType = SyncMediaType.Other,
            finishedAtMillis = 0L,
            batchId = 0L,
        )
    )

    private fun queued(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_QUEUED)

    private fun paused(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_PAUSED)

    private fun retrying(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_RETRYING)

    private fun completing(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_COMPLETING)

    private fun row(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection,
        state: TransferState,
    ) = SyncTransferRow(
        entry = entry(tag, priority, direction, state),
        mediaType = SyncMediaType.Other,
        optimisticPause = null,
    )

    private fun entry(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection,
        state: TransferState,
    ) = SyncTransferEntry(
        tag = tag,
        direction = direction,
        state = state,
        fileName = "f$tag.txt",
        totalBytes = 100,
        transferredBytes = 0,
        speed = 0,
        meanSpeed = 0,
        priority = priority,
        notificationNumber = tag.toLong(),
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private companion object {
        const val A = 1
        const val B = 2
        const val C = 3
        const val D = 4
        const val F = 5
        const val P = 6
        const val P1 = 7
        const val P2 = 8
        const val Q = 9
        const val Q1 = 10
        const val Q2 = 11
        const val U = 12
        const val R = 13

        val ALL_MOVES = arrayOf(
            SyncTransferMove.ToTop,
            SyncTransferMove.Up,
            SyncTransferMove.Down,
            SyncTransferMove.ToBottom,
        )
    }
}
