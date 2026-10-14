package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveSyncTransferUseCaseTest {

    private lateinit var underTest: MoveSyncTransferUseCase

    private val monitorSyncTransferListUseCase = mock<MonitorSyncTransferListUseCase>()
    private val moveTransferToFirstByTagUseCase = mock<MoveTransferToFirstByTagUseCase>()
    private val moveTransferToLastByTagUseCase = mock<MoveTransferToLastByTagUseCase>()
    private val moveTransferBeforeByTagUseCase = mock<MoveTransferBeforeByTagUseCase>()

    private val getSyncTransferListUseCase = GetSyncTransferListUseCase(
        GetSyncTransferRowStateUseCase(),
        GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase()),
        IsSyncTransferSearchMatchUseCase(),
    )

    @BeforeAll
    fun setUp() {
        underTest = MoveSyncTransferUseCase(
            monitorSyncTransferListUseCase = monitorSyncTransferListUseCase,
            moveTransferToFirstByTagUseCase = moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase = moveTransferToLastByTagUseCase,
            moveTransferBeforeByTagUseCase = moveTransferBeforeByTagUseCase,
            resolveSyncTransferMoveUseCase = ResolveSyncTransferMoveUseCase(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncTransferListUseCase,
            moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase,
            moveTransferBeforeByTagUseCase,
        )
        wheneverBlocking { moveTransferToFirstByTagUseCase(any()) }.thenReturn(Unit)
        wheneverBlocking { moveTransferToLastByTagUseCase(any()) }.thenReturn(Unit)
        wheneverBlocking { moveTransferBeforeByTagUseCase(any(), any()) }.thenReturn(Unit)
    }

    @Test
    fun `test that invoke moves the transfer to the first position for top`() = runTest {
        stubPending(queued(A, 10), queued(B, 20))

        assertThat(underTest(B, SyncTransferMove.ToTop)).isTrue()
        verify(moveTransferToFirstByTagUseCase).invoke(B)
    }

    @Test
    fun `test that invoke moves the transfer to the last position for bottom`() = runTest {
        stubPending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.ToBottom)).isTrue()
        verify(moveTransferToLastByTagUseCase).invoke(A)
    }

    @Test
    fun `test that invoke moves the transfer before the resolved anchor for up`() = runTest {
        stubPending(queued(A, 10), queued(B, 20))

        assertThat(underTest(B, SyncTransferMove.Up)).isTrue()
        verify(moveTransferBeforeByTagUseCase).invoke(B, A)
    }

    @Test
    fun `test that invoke resolves a drop from the displayed order`() = runTest {
        stubPending(queued(A, 10), queued(B, 20), queued(C, 30), queued(D, 40))

        assertThat(underTest(D, SyncTransferMove.Drop(listOf(A, D, B, C)))).isTrue()
        verify(moveTransferBeforeByTagUseCase).invoke(D, B)
    }

    @Test
    fun `test that invoke resolves against every pending row beyond the visible window`() =
        runTest {
            stubPending(queued(A, 10), queued(B, 20))

            underTest(B, SyncTransferMove.Up)

            verify(monitorSyncTransferListUseCase).invoke(SyncTransferFilter.Pending, Int.MAX_VALUE)
        }

    @Test
    fun `test that invoke sends nothing and returns false when the move is a no-op`() = runTest {
        stubPending(queued(A, 10), queued(B, 20))

        assertThat(underTest(A, SyncTransferMove.Up)).isFalse()
        verifyNoInteractions(
            moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase,
            moveTransferBeforeByTagUseCase,
        )
    }

    @Test
    fun `test that invoke propagates a failed request`() = runTest {
        stubPending(queued(A, 10), queued(B, 20))
        wheneverBlocking { moveTransferBeforeByTagUseCase(any(), any()) }
            .thenThrow(RuntimeException("move failed"))

        assertThrows<RuntimeException> { underTest(B, SyncTransferMove.Up) }
    }

    private fun stubPending(vararg rows: SyncTransferRow) {
        val items = getSyncTransferListUseCase(
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
        val list = SyncTransferList(
            isSeeded = true,
            items = items,
            totalCount = items.size,
            counts = SyncTransferFilterCounts.Empty,
        )
        whenever(monitorSyncTransferListUseCase(SyncTransferFilter.Pending, Int.MAX_VALUE))
            .thenReturn(flowOf(list))
    }

    private fun queued(tag: Int, priority: Long) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = SyncTransferDirection.Upload,
            state = TransferState.STATE_QUEUED,
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
        ),
        mediaType = SyncMediaType.Other,
        optimisticPause = null,
    )

    private companion object {
        const val A = 1
        const val B = 2
        const val C = 3
        const val D = 4
    }
}
