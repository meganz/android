package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentHashMap
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.usecase.progress.ResolveSyncTransferMoveUseCase.ResolvedMove
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveSyncTransferMoveUseCaseTest {

    private lateinit var underTest: ResolveSyncTransferMoveUseCase

    private val getSyncTransferListUseCase = GetSyncTransferListUseCase(
        GetSyncTransferRowStateUseCase(),
        GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase()),
        IsSyncTransferSearchMatchUseCase(),
    )

    @BeforeAll
    fun setUp() {
        underTest = ResolveSyncTransferMoveUseCase()
    }

    @Test
    fun `test that invoke moves up past the previous row in the same band`() {
        val list = pending(queued(A, 10), queued(B, 30), paused(P, 20))
        val withRetrying = pending(retrying(R, 10), queued(Q, 20))

        assertThat(underTest(B, SyncTransferMove.Up, list)).isEqualTo(ResolvedMove.Before(A))
        assertThat(underTest(Q, SyncTransferMove.Up, withRetrying))
            .isEqualTo(ResolvedMove.Before(R))
    }

    @Test
    fun `test that invoke moves up one visible step past hidden transfers`() {
        // A hidden Camera Uploads upload sits at priority 20 in the SDK and is not listed.
        val list = pending(queued(A, 10), queued(B, 30))

        assertThat(underTest(B, SyncTransferMove.Up, list)).isEqualTo(ResolvedMove.Before(A))
    }

    @Test
    fun `test that invoke moves down before the row two below`() {
        // P follows A in priority order, so only the band's second row below gives B.
        val list = pending(queued(R, 10), queued(A, 20), paused(P, 30), queued(B, 40))

        assertThat(underTest(R, SyncTransferMove.Down, list)).isEqualTo(ResolvedMove.Before(B))
    }

    @Test
    fun `test that invoke moves down to the priority anchor when one row is below in the band`() {
        val list = pending(queued(R, 10), queued(A, 20), paused(P, 30))

        assertThat(underTest(R, SyncTransferMove.Down, list)).isEqualTo(ResolvedMove.Before(P))
    }

    @Test
    fun `test that invoke moves down to the last position when one row is below and nothing follows it`() {
        val list = pending(queued(R, 10), queued(A, 20))

        assertThat(underTest(R, SyncTransferMove.Down, list)).isEqualTo(ResolvedMove.ToLast)
    }

    @Test
    fun `test that invoke returns null for up on the first row of the band`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.Up, list)).isNull()
    }

    @Test
    fun `test that invoke returns null for down on the last row of the band`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(B, SyncTransferMove.Down, list)).isNull()
    }

    @Test
    fun `test that invoke moves to the first position for top`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(B, SyncTransferMove.ToTop, list)).isEqualTo(ResolvedMove.ToFirst)
    }

    @Test
    fun `test that invoke moves the first row of a lower band to the first position for top`() {
        val list = pending(queued(Q, 10), paused(P1, 20), paused(P2, 30))

        assertThat(underTest(P1, SyncTransferMove.ToTop, list)).isEqualTo(ResolvedMove.ToFirst)
    }

    @Test
    fun `test that invoke moves to the first position for top when the row already leads its direction`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.ToTop, list)).isEqualTo(ResolvedMove.ToFirst)
    }

    @Test
    fun `test that invoke moves to the last position for bottom`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.ToBottom, list)).isEqualTo(ResolvedMove.ToLast)
    }

    @Test
    fun `test that invoke moves the last row of an upper band to the last position for bottom`() {
        val list = pending(queued(Q1, 10), queued(Q2, 20), paused(P, 5))

        assertThat(underTest(Q2, SyncTransferMove.ToBottom, list)).isEqualTo(ResolvedMove.ToLast)
    }

    @Test
    fun `test that invoke ignores rows of the other direction`() {
        val list = pending(
            queued(U1, 10),
            queued(D1, 15, SyncTransferDirection.Download),
            queued(U2, 20),
        )

        assertThat(underTest(U2, SyncTransferMove.Up, list)).isEqualTo(ResolvedMove.Before(U1))
    }

    @Test
    fun `test that invoke moves within the paused band`() {
        val list = pending(paused(P1, 5), paused(P2, 8), queued(Q, 1))

        assertThat(underTest(P2, SyncTransferMove.Up, list)).isEqualTo(ResolvedMove.Before(P1))
    }

    @Test
    fun `test that invoke treats every pausable row as one band under the app wide pause`() {
        val rows = arrayOf(queued(Q, 10), paused(P, 15), active(A, 20))

        assertThat(underTest(A, SyncTransferMove.Up, pending(*rows, areTransfersPaused = true)))
            .isEqualTo(ResolvedMove.Before(P))
        assertThat(underTest(A, SyncTransferMove.Up, pending(*rows, areTransfersPaused = false)))
            .isEqualTo(ResolvedMove.Before(Q))
    }

    @Test
    fun `test that invoke returns null for a completing row`() {
        val list = pending(completing(C, 5), queued(Q, 10))

        assertThat(underTest(C, SyncTransferMove.ToBottom, list)).isNull()
        assertThat(underTest(C, SyncTransferMove.ToTop, list)).isNull()
    }

    @Test
    fun `test that invoke skips completing rows as targets`() {
        val list = pending(queued(R, 10), queued(A, 20), completing(C, 25), paused(P, 30))

        assertThat(underTest(R, SyncTransferMove.Down, list)).isEqualTo(ResolvedMove.Before(P))
    }

    @Test
    fun `test that invoke accepts an active row as the anchor although it can turn completing within one chunk`() {
        // The store can be one chunk stale, so the anchor may already be completing in the SDK.
        // Skipping active anchors would change where every move lands, so the race is accepted.
        val list = pending(active(A, 10), queued(B, 20))

        assertThat(underTest(B, SyncTransferMove.Up, list)).isEqualTo(ResolvedMove.Before(A))
    }

    @Test
    fun `test that invoke never moves or targets a row in the none state`() {
        val list = pending(queued(R, 10), none(N, 20), queued(A, 30))

        assertThat(underTest(R, SyncTransferMove.Down, list)).isEqualTo(ResolvedMove.ToLast)
        assertThat(underTest(N, SyncTransferMove.Up, list)).isNull()
        assertThat(underTest(N, SyncTransferMove.ToTop, list)).isNull()
    }

    @Test
    fun `test that invoke returns null for a tag not in the list`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(MISSING, SyncTransferMove.Up, list)).isNull()
        assertThat(underTest(MISSING, SyncTransferMove.ToTop, list)).isNull()
    }

    @Test
    fun `test that invoke drop picks the next same direction row below the drop in the same band`() {
        val list = pending(queued(A, 10), queued(B, 20), queued(C, 30), queued(D, 40))
        // P follows A in priority order, so only the next shown row gives B.
        val withPaused = pending(queued(A, 10), queued(B, 30), queued(C, 40), paused(P, 20))

        assertThat(underTest(D, SyncTransferMove.Drop(listOf(A, D, B, C)), list))
            .isEqualTo(ResolvedMove.Before(B))
        assertThat(underTest(C, SyncTransferMove.Drop(listOf(A, C, B, P)), withPaused))
            .isEqualTo(ResolvedMove.Before(B))
    }

    @Test
    fun `test that invoke drop skips rows of the other direction below the drop`() {
        val list = pending(
            queued(A, 10),
            queued(B, 20),
            queued(C, 30),
            queued(X, 25, SyncTransferDirection.Download),
        )

        assertThat(underTest(A, SyncTransferMove.Drop(listOf(B, A, X, C)), list))
            .isEqualTo(ResolvedMove.Before(C))
    }

    @Test
    fun `test that invoke drop anchors on priority order when the row closes its band`() {
        val list = pending(queued(A, 10), queued(B, 20), queued(C, 30), paused(P, 40))

        assertThat(underTest(A, SyncTransferMove.Drop(listOf(B, C, A, P)), list))
            .isEqualTo(ResolvedMove.Before(P))
    }

    @Test
    fun `test that invoke drop ignores rows of another band`() {
        val list = pending(queued(A, 10), queued(B, 20), paused(P, 15))

        assertThat(underTest(A, SyncTransferMove.Drop(listOf(B, A, P)), list))
            .isEqualTo(ResolvedMove.ToLast)
    }

    @Test
    fun `test that invoke drop returns ToLast when nothing follows`() {
        val list = pending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.Drop(listOf(B, A)), list))
            .isEqualTo(ResolvedMove.ToLast)
    }

    @Test
    fun `test that invoke drop anchors on a row beyond the displayed window`() {
        val list = pending(queued(A, 10), queued(B, 20), queued(C, 30), queued(D, 40))

        assertThat(underTest(A, SyncTransferMove.Drop(listOf(B, C, A)), list))
            .isEqualTo(ResolvedMove.Before(D))
    }

    @Test
    fun `test that invoke drop returns null at the row's own position`() {
        val list = pending(queued(A, 10), queued(B, 20), queued(C, 30))

        assertThat(underTest(B, SyncTransferMove.Drop(listOf(A, B, C)), list)).isNull()
    }

    @Test
    fun `test that invoke drop returns null when the row only moved past rows of the other direction`() {
        val list = pending(
            queued(U1, 10),
            queued(D1, 15, SyncTransferDirection.Download),
            queued(U2, 20),
        )

        assertThat(underTest(U2, SyncTransferMove.Drop(listOf(U1, U2, D1)), list)).isNull()
    }

    @Test
    fun `test that invoke drop returns null when the dragged tag is not displayed`() {
        val list = pending(queued(A, 10), queued(B, 20), queued(C, 30))

        assertThat(underTest(B, SyncTransferMove.Drop(listOf(A, C)), list)).isNull()
    }

    @Test
    fun `test that invoke drop ignores tags that are no longer pending`() {
        // B leads the priority line, so a drop of B below A sends it to the end.
        val list = pending(queued(A, 20), queued(B, 10))

        val withFinishedTag = underTest(B, SyncTransferMove.Drop(listOf(A, MISSING, B)), list)

        assertThat(withFinishedTag).isEqualTo(underTest(B, SyncTransferMove.Drop(listOf(A, B)), list))
        assertThat(withFinishedTag).isEqualTo(ResolvedMove.ToLast)
    }

    private fun pending(
        vararg rows: SyncTransferRow,
        areTransfersPaused: Boolean = false,
    ): List<SyncTransferListItem> = getSyncTransferListUseCase(
        state = store(rows.toList()),
        filter = SyncTransferFilter.Pending,
        context = SyncTransferRowContext(
            areTransfersPaused = areTransfersPaused,
            isStorageOverQuota = false,
            isTransferOverQuota = false,
        ),
        limit = Int.MAX_VALUE,
    ).items

    private fun store(rows: List<SyncTransferRow>) = SyncTransferStoreState.NotSeeded.copy(
        isSeeded = true,
        active = rows.associateBy { it.entry.tag }.toPersistentHashMap(),
    )

    private fun queued(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_QUEUED)

    private fun active(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        transferred: Long = 10,
    ) = row(tag, priority, direction, TransferState.STATE_ACTIVE, transferred)

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

    private fun none(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = row(tag, priority, direction, TransferState.STATE_NONE)

    private fun row(
        tag: Int,
        priority: Long,
        direction: SyncTransferDirection,
        state: TransferState,
        transferred: Long = 0,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = direction,
            state = state,
            fileName = "f$tag.txt",
            totalBytes = 100,
            transferredBytes = transferred,
            speed = 0,
            meanSpeed = 0,
            priority = priority,
            notificationNumber = tag.toLong(),
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Other,
        optimisticPause = null,
    )

    private companion object {
        const val A = 1
        const val B = 2
        const val C = 3
        const val D = 4
        const val R = 5
        const val P = 6
        const val P1 = 7
        const val P2 = 8
        const val Q = 9
        const val Q1 = 10
        const val Q2 = 11
        const val U1 = 12
        const val U2 = 13
        const val D1 = 14
        const val X = 15
        const val N = 16
        const val MISSING = 99
    }
}
