package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncTransferFilterCountsUseCaseTest {

    private lateinit var underTest: GetSyncTransferFilterCountsUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase())
    }

    @Test
    fun `test that invoke counts search matches per direction`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload, fileName = "a1.txt"),
                row(2, direction = SyncTransferDirection.Download, fileName = "a2.txt"),
                row(3, direction = SyncTransferDirection.Download, fileName = "b.txt"),
            ),
            completed = listOf(
                finished(4, finishedAt = 1_000, direction = SyncTransferDirection.Upload, fileName = "a4.txt"),
            ),
        )

        val actual = underTest(state, SyncTransferFilter.Search("a", SyncTransferDirection.Download))

        assertThat(actual.searchUploads).isEqualTo(2)
        assertThat(actual.searchDownloads).isEqualTo(1)
    }

    @Test
    fun `test that invoke counts search matches ignoring case`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload, fileName = "Report.PDF"),
                row(2, direction = SyncTransferDirection.Download, fileName = "notes.txt"),
            ),
            completed = listOf(
                finished(3, finishedAt = 1_000, direction = SyncTransferDirection.Download, fileName = "old-REPORT.txt"),
            ),
        )

        val actual = underTest(state, SyncTransferFilter.Search("rePort"))

        assertThat(actual.searchUploads).isEqualTo(1)
        assertThat(actual.searchDownloads).isEqualTo(1)
    }

    @Test
    fun `test that invoke counts search matches only in tracked and completed rows`() {
        val state = storeState(
            active = listOf(row(1, direction = SyncTransferDirection.Upload, fileName = "a.txt")),
            overflow = mapOf(2 to overflow(SyncTransferDirection.Upload, TransferState.STATE_QUEUED)),
        )

        val actual = underTest(state, SyncTransferFilter.Search("a"))

        assertThat(actual.searchUploads).isEqualTo(1)
        assertThat(actual.searchDownloads).isEqualTo(0)
    }

    @Test
    fun `test that invoke returns zero search counts for other filters`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload),
                row(2, direction = SyncTransferDirection.Download),
            ),
        )

        val actual = underTest(state, SyncTransferFilter.Pending)

        assertThat(actual.searchUploads).isEqualTo(0)
        assertThat(actual.searchDownloads).isEqualTo(0)
    }

    @Test
    fun `test that invoke returns the desktop tab counters from the store counts`() {
        val byMediaType = persistentMapOf(SyncMediaType.Image to 4, SyncMediaType.Document to 6)
        val counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(total = 9, pending = 7),
            lifetimeDownloads = SyncTransferTally(total = 5, pending = 3),
            byMediaType = byMediaType,
        )
        val state = storeState(active = listOf(row(1)), counts = counts)
        val expected = SyncTransferFilterCounts(
            pending = 10,
            pendingUploads = 7,
            pendingDownloads = 3,
            completed = 4,
            byMediaType = byMediaType,
            searchUploads = 0,
            searchDownloads = 0,
        )

        listOf(
            SyncTransferFilter.Pending,
            SyncTransferFilter.Direction(SyncTransferDirection.Download),
            SyncTransferFilter.Completed,
            SyncTransferFilter.Media(SyncMediaType.Video),
            SyncTransferFilter.Search("zzz"),
        ).forEach { filter ->
            assertThat(underTest(state, filter)).isEqualTo(expected)
        }
    }

    private fun entry(
        tag: Int,
        direction: SyncTransferDirection,
        state: TransferState,
        priority: Long,
        transferred: Long,
        fileName: String,
        notificationNumber: Long = tag.toLong(),
        path: String? = null,
    ) = SyncTransferEntry(
        tag = tag,
        direction = direction,
        state = state,
        fileName = fileName,
        totalBytes = 100,
        transferredBytes = transferred,
        speed = 0,
        meanSpeed = 0,
        priority = priority,
        notificationNumber = notificationNumber,
        lastError = null,
        isForeignOverQuota = false,
        path = path,
    )

    private fun row(
        tag: Int,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        state: TransferState = TransferState.STATE_QUEUED,
        priority: Long = tag.toLong(),
        transferred: Long = 0,
        fileName: String = "f$tag.txt",
        mediaType: SyncMediaType = SyncMediaType.Document,
        optimisticPause: Boolean? = null,
    ) = SyncTransferRow(
        entry = entry(tag, direction, state, priority, transferred, fileName),
        mediaType = mediaType,
        optimisticPause = optimisticPause,
    )

    private fun finished(
        tag: Int,
        finishedAt: Long,
        n: Long = tag.toLong(),
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        fileName: String = "f$tag.txt",
        mediaType: SyncMediaType = SyncMediaType.Document,
        path: String? = null,
    ) = SyncCompletedTransfer(
        entry = entry(
            tag = tag,
            direction = direction,
            state = TransferState.STATE_COMPLETED,
            priority = tag.toLong(),
            transferred = 100,
            fileName = fileName,
            notificationNumber = n,
            path = path,
        ),
        mediaType = mediaType,
        finishedAtMillis = finishedAt,
        batchId = 0,
    )

    private fun overflow(
        direction: SyncTransferDirection,
        state: TransferState,
        mediaType: SyncMediaType = SyncMediaType.Document,
    ) = SyncTransferOverflowEntry(direction, state, mediaType)

    private fun storeState(
        active: List<SyncTransferRow> = emptyList(),
        overflow: Map<Int, SyncTransferOverflowEntry> = emptyMap(),
        completed: List<SyncCompletedTransfer> = emptyList(),
        counts: SyncTransferCounts = SyncTransferCounts.Empty,
        isSeeded: Boolean = true,
    ) = SyncTransferStoreState(
        isSeeded = isSeeded,
        active = active.associateBy { it.entry.tag }.toPersistentHashMap(),
        overflow = overflow.toPersistentHashMap(),
        completed = completed.toPersistentList(),
        counts = counts,
        reseedRemoved = persistentHashSetOf(),
    )
}
