package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClearVisibleCompletedSyncTransfersUseCaseTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }

    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val monitorSyncProgressBlockersUseCase = mock<MonitorSyncProgressBlockersUseCase>()
    private val syncTransferStoreRepository = mock<SyncTransferStoreRepository>()

    private val isSyncTransferSearchMatchUseCase = IsSyncTransferSearchMatchUseCase()
    private val getSyncTransferListUseCase = GetSyncTransferListUseCase(
        getSyncTransferRowStateUseCase = GetSyncTransferRowStateUseCase(),
        getSyncTransferFilterCountsUseCase =
            GetSyncTransferFilterCountsUseCase(isSyncTransferSearchMatchUseCase),
        isSyncTransferSearchMatchUseCase = isSyncTransferSearchMatchUseCase,
    )

    private lateinit var underTest: ClearVisibleCompletedSyncTransfersUseCase

    @BeforeAll
    fun setUp() {
        underTest = ClearVisibleCompletedSyncTransfersUseCase(
            monitorSyncTransferListUseCase = MonitorSyncTransferListUseCase(
                monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
                monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
                monitorSyncProgressBlockersUseCase = monitorSyncProgressBlockersUseCase,
                getSyncTransferListUseCase = getSyncTransferListUseCase,
                defaultDispatcher = extension.testDispatcher,
            ),
            syncTransferStoreRepository = syncTransferStoreRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncTransferStoreUseCase,
            monitorPausedTransfersUseCase,
            monitorSyncProgressBlockersUseCase,
            syncTransferStoreRepository,
        )
    }

    @Test
    fun `test that invoke clears every completed row of a media filter`() = runTest {
        val images = (1001..1490).map { finished(it, finishedAt = 10_000L - it, SyncMediaType.Image) }
        val documents = (2001..2010).map { finished(it, finishedAt = 5_000L - it, SyncMediaType.Document) }
        val state = storeState(
            active = (1..20).map { row(it, SyncMediaType.Image) },
            completed = images + documents,
        )
        stubStore(state)
        val window = getSyncTransferListUseCase(
            state = state,
            filter = SyncTransferFilter.Media(SyncMediaType.Image),
            context = SyncTransferRowContext(false, false, false),
        )

        underTest(SyncTransferFilter.Media(SyncMediaType.Image))

        assertThat(window.items.filterIsInstance<SyncTransferListItem.Finished>()).hasSize(480)
        verify(syncTransferStoreRepository).clearCompleted((1001..1490).toSet())
    }

    @Test
    fun `test that invoke clears only the completed rows that match the search`() = runTest {
        stubStore(
            storeState(
                completed = listOf(
                    finished(1, finishedAt = 2_000, fileName = "a.jpg"),
                    finished(2, finishedAt = 1_000, fileName = "b.jpg"),
                ),
            )
        )

        underTest(SyncTransferFilter.Search("a"))

        verify(syncTransferStoreRepository).clearCompleted(setOf(1))
    }

    @Test
    fun `test that invoke leaves the completed rows of other media types`() = runTest {
        stubStore(
            storeState(
                completed = listOf(
                    finished(1, finishedAt = 2_000, SyncMediaType.Image),
                    finished(2, finishedAt = 1_000, SyncMediaType.Document),
                ),
            )
        )

        underTest(SyncTransferFilter.Media(SyncMediaType.Image))

        val captor = argumentCaptor<Set<Int>>()
        verify(syncTransferStoreRepository).clearCompleted(captor.capture())
        assertThat(captor.firstValue).doesNotContain(2)
        assertThat(captor.firstValue).containsExactly(1)
    }

    @Test
    fun `test that invoke does nothing when the filter shows no completed row`() = runTest {
        stubStore(
            storeState(
                active = listOf(row(1, SyncMediaType.Video)),
                completed = listOf(finished(2, finishedAt = 1_000, SyncMediaType.Image)),
            )
        )

        underTest(SyncTransferFilter.Media(SyncMediaType.Video))

        verify(syncTransferStoreRepository, never()).clearCompleted(any())
    }

    @Test
    fun `test that invoke does nothing for the pending filter`() = runTest {
        stubStore(
            storeState(
                active = listOf(row(1, SyncMediaType.Image)),
                completed = listOf(finished(2, finishedAt = 1_000, SyncMediaType.Image)),
            )
        )

        underTest(SyncTransferFilter.Pending)

        verify(syncTransferStoreRepository, never()).clearCompleted(any())
    }

    private fun stubStore(state: SyncTransferStoreState) {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(flowOf(state))
        whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(false))
        whenever(monitorSyncProgressBlockersUseCase()).thenReturn(
            flowOf(
                SyncProgressBlockers(
                    isOffline = false,
                    isStorageOverQuota = false,
                    isTransferOverQuota = false,
                    areTransfersPaused = false,
                    stalledIssueCount = 0,
                )
            )
        )
    }

    private fun entry(tag: Int, state: TransferState, fileName: String) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = fileName,
        totalBytes = 100,
        transferredBytes = 10,
        speed = 0,
        meanSpeed = 0,
        priority = tag.toLong(),
        notificationNumber = tag.toLong(),
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private fun row(tag: Int, mediaType: SyncMediaType) = SyncTransferRow(
        entry = entry(tag, TransferState.STATE_ACTIVE, "f$tag.jpg"),
        mediaType = mediaType,
        optimisticPause = null,
    )

    private fun finished(
        tag: Int,
        finishedAt: Long,
        mediaType: SyncMediaType = SyncMediaType.Image,
        fileName: String = "f$tag.jpg",
    ) = SyncCompletedTransfer(
        entry = entry(tag, TransferState.STATE_COMPLETED, fileName),
        mediaType = mediaType,
        finishedAtMillis = finishedAt,
        batchId = 0,
    )

    private fun storeState(
        active: List<SyncTransferRow> = emptyList(),
        completed: List<SyncCompletedTransfer> = emptyList(),
    ) = SyncTransferStoreState(
        isSeeded = true,
        active = active.associateBy { it.entry.tag }.toPersistentHashMap(),
        overflow = persistentHashMapOf(),
        completed = completed.toPersistentList(),
        counts = SyncTransferCounts.Empty,
        reseedRemoved = persistentHashSetOf(),
    )
}
