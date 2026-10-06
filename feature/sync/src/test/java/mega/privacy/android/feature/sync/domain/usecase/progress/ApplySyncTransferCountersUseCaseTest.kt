package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentMap
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType.Image
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferBatchProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection.Download
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection.Upload
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition
import mega.privacy.android.feature.sync.domain.entity.progress.batchProgress
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplySyncTransferCountersUseCaseTest {

    private lateinit var underTest: ApplySyncTransferCountersUseCase

    @BeforeAll
    fun setUp() {
        underTest = ApplySyncTransferCountersUseCase()
    }

    @Test
    fun `test that invoke counts an admitted transfer in lifetime and batch`() {
        val result = underTest(SyncTransferCounts.Empty, listOf(admitted(n = 1)))

        assertThat(result.lifetimeUploads).isEqualTo(tally(1, 1))
        assertThat(result.batchUploads).isEqualTo(tally(1, 1))
        assertThat(result.byMediaType[Image]).isEqualTo(1)
    }

    @Test
    fun `test that invoke keeps the media counts on completion`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(n = 1), completed(n = 2)),
        )

        assertThat(result.byMediaType[Image]).isEqualTo(1)
    }

    @Test
    fun `test that invoke resets the batch when no transfer is pending in either direction`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(n = 1), completed(n = 2)),
        )

        assertThat(result.batchUploads).isEqualTo(tally(0, 0))
        assertThat(result.batchDownloads).isEqualTo(tally(0, 0))
        assertThat(result.batchId).isEqualTo(1)
        assertThat(result.lifetimeUploads).isEqualTo(tally(1, 0))
    }

    @Test
    fun `test that invoke does not reset the batch while the other direction is pending`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(d = Upload, n = 1), admitted(d = Download, n = 2), completed(d = Upload, n = 3)),
        )

        assertThat(result.batchUploads).isEqualTo(tally(1, 0))
        assertThat(result.batchDownloads).isEqualTo(tally(1, 1))
        assertThat(result.batchId).isEqualTo(0)
    }

    @Test
    fun `test that invoke keeps the batch open while a paused transfer is pending`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(n = 1), admitted(n = 2), completed(n = 3), admitted(n = 4)),
        )

        assertThat(result.batchUploads).isEqualTo(tally(3, 2))
        assertThat(result.batchId).isEqualTo(0)
    }

    @Test
    fun `test that invoke applies transitions in notification number order`() {
        val withB = counts(lifetimeUp = 1 to 1, batchUp = 1 to 1, batchId = 4, media = mapOf(Image to 1))

        val result = underTest(withB, listOf(completed(n = 2), admitted(n = 1)))

        assertThat(result.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 2, total = 2))
        assertThat(result.batchId).isEqualTo(4)
    }

    @Test
    fun `test that invoke keeps the caller order for equal notification numbers`() {
        val inOrder = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(n = 5), completed(n = 5)),
        )
        val reversed = underTest(
            SyncTransferCounts.Empty,
            listOf(completed(n = 5), admitted(n = 5)),
        )

        assertThat(inOrder.lifetimeUploads).isEqualTo(tally(1, 0))
        assertThat(inOrder.batchId).isEqualTo(1)
        assertThat(reversed.lifetimeUploads).isEqualTo(tally(1, 1))
        assertThat(reversed.batchUploads).isEqualTo(tally(1, 1))
    }

    @ParameterizedTest
    @EnumSource(SyncTransferDirection::class)
    fun `test that invoke rolls back lifetime batch and media counts`(direction: SyncTransferDirection) {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(d = direction, n = 1), rolledBack(d = direction, n = 2)),
        )

        assertThat(result.lifetimeUploads).isEqualTo(tally(0, 0))
        assertThat(result.lifetimeDownloads).isEqualTo(tally(0, 0))
        assertThat(result.batchUploads).isEqualTo(tally(0, 0))
        assertThat(result.batchDownloads).isEqualTo(tally(0, 0))
        assertThat(result.byMediaType).isEmpty()
    }

    @Test
    fun `test that invoke resets the batch after a roll back empties the queue`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(n = 1), admitted(n = 2), completed(n = 3), rolledBack(n = 4)),
        )

        assertThat(result.batchUploads).isEqualTo(tally(0, 0))
        assertThat(result.batchId).isEqualTo(1)
        assertThat(result.lifetimeUploads).isEqualTo(tally(1, 0))
    }

    @Test
    fun `test that invoke lowers the batch when a cleared row belongs to the current batch`() {
        val before = counts(lifetimeUp = 10 to 8, batchUp = 10 to 8, batchId = 3, media = mapOf(Image to 10))

        val result = underTest(
            before,
            listOf(cleared(n = 1, batchId = 3), cleared(n = 2, batchId = 3)),
        )

        assertThat(result.batchUploads).isEqualTo(tally(8, 8))
        assertThat(result.lifetimeUploads.total).isEqualTo(8)
        assertThat(result.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 1, total = 8))
    }

    @Test
    fun `test that invoke leaves the batch when a cleared row belongs to an older batch`() {
        val before = counts(lifetimeUp = 5 to 4, batchUp = 4 to 4, batchId = 3, media = mapOf(Image to 5))

        val result = underTest(before, listOf(cleared(n = 1, batchId = 2)))

        assertThat(result.batchUploads).isEqualTo(tally(4, 4))
        assertThat(result.lifetimeUploads.total).isEqualTo(4)
        assertThat(result.byMediaType[Image]).isEqualTo(4)
    }

    @Test
    fun `test that invoke leaves the batch when a row is evicted`() {
        val before = counts(lifetimeUp = 4 to 3, batchUp = 4 to 3, batchId = 2, media = mapOf(Image to 4))

        val result = underTest(before, listOf(evicted(n = 1, batchId = 2)))

        assertThat(result.batchUploads).isEqualTo(tally(4, 3))
        assertThat(result.lifetimeUploads.total).isEqualTo(3)
        assertThat(result.byMediaType[Image]).isEqualTo(3)
    }

    @Test
    fun `test that invoke counts an evicted completion of the current batch`() {
        val before = counts(lifetimeUp = 5 to 1, batchUp = 5 to 1, batchId = 2, media = mapOf(Image to 5))

        val result = underTest(
            before,
            listOf(evicted(d = Upload, n = 1, batchId = 2), evicted(d = Upload, n = 2, batchId = 2)),
        )

        assertThat(result.batchEvictedUploads).isEqualTo(2)
        assertThat(result.batchEvictedDownloads).isEqualTo(0)
    }

    @Test
    fun `test that invoke does not count an evicted completion of an older batch`() {
        val before = counts(lifetimeUp = 5 to 1, batchUp = 3 to 1, batchId = 2, media = mapOf(Image to 5))

        val result = underTest(before, listOf(evicted(n = 1, batchId = 1)))

        assertThat(result.batchEvictedUploads).isEqualTo(0)
        assertThat(result.batchEvictedDownloads).isEqualTo(0)
        assertThat(result.lifetimeUploads.total).isEqualTo(4)
    }

    @Test
    fun `test that invoke lowers the batch by the evicted completions when they are cleared`() {
        val before = counts(
            lifetimeUp = 7 to 2,
            batchUp = 10 to 2,
            media = mapOf(Image to 7),
            evictedUp = 5,
        )

        val result = underTest(before, listOf(clearedEvicted(d = Upload, n = 1)))

        assertThat(result.batchUploads).isEqualTo(tally(5, 2))
        assertThat(result.batchEvictedUploads).isEqualTo(0)
        assertThat(result.lifetimeUploads).isEqualTo(before.lifetimeUploads)
        assertThat(result.lifetimeDownloads).isEqualTo(before.lifetimeDownloads)
        assertThat(result.byMediaType).isEqualTo(before.byMediaType)
    }

    @Test
    fun `test that invoke clears the evicted tally of one direction only`() {
        val before = counts(
            lifetimeUp = 2 to 2,
            lifetimeDown = 1 to 1,
            batchUp = 10 to 2,
            batchDown = 6 to 1,
            evictedUp = 3,
            evictedDown = 2,
        )

        val result = underTest(before, listOf(clearedEvicted(d = Upload, n = 1)))

        assertThat(result.batchEvictedUploads).isEqualTo(0)
        assertThat(result.batchEvictedDownloads).isEqualTo(2)
        assertThat(result.batchDownloads).isEqualTo(tally(6, 1))
    }

    @Test
    fun `test that invoke ignores a cleared evicted tally of zero`() {
        val before = counts(lifetimeDown = 3 to 1, batchDown = 3 to 1, media = mapOf(Image to 3))

        val result = underTest(before, listOf(clearedEvicted(d = Download, n = 1)))

        assertThat(result).isEqualTo(before)
    }

    @Test
    fun `test that invoke reproduces the desktop clear all after evictions`() {
        val before = counts(
            lifetimeUp = 1000 to 500,
            batchUp = 5000 to 500,
            batchId = 7,
            media = mapOf(Image to 1000),
            evictedUp = 4000,
        )
        val clearAll = (1L..500L).map { cleared(n = it, batchId = 7) } +
            clearedEvicted(d = Upload, n = 501)

        val result = underTest(before, clearAll)

        assertThat(result.batchUploads).isEqualTo(tally(500, 500))
        assertThat(result.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 1, total = 500))
        assertThat(result.lifetimeUploads).isEqualTo(tally(500, 500))
    }

    @Test
    fun `test that invoke resets the evicted tallies with the batch`() {
        val before = counts(
            lifetimeUp = 10 to 1,
            batchUp = 10 to 1,
            batchId = 2,
            media = mapOf(Image to 10),
            evictedUp = 4,
            evictedDown = 1,
        )

        val result = underTest(before, listOf(completed(n = 1)))

        assertThat(result.batchEvictedUploads).isEqualTo(0)
        assertThat(result.batchEvictedDownloads).isEqualTo(0)
        assertThat(result.batchId).isEqualTo(3)
    }

    @Test
    fun `test that invoke does not reset the batch on a clear or an eviction`() {
        val before = counts(lifetimeUp = 2 to 0, batchUp = 2 to 0, batchId = 4, media = mapOf(Image to 2))

        val result = underTest(
            before,
            listOf(
                cleared(n = 1, batchId = 4),
                evicted(n = 2, batchId = 4),
                clearedEvicted(d = Upload, n = 3),
            ),
        )

        assertThat(result.batchId).isEqualTo(4)
    }

    @Test
    fun `test that invoke removes a media type whose count reaches zero`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(t = Image, n = 1), rolledBack(t = Image, n = 2)),
        )

        assertThat(result.byMediaType).doesNotContainKey(Image)
    }

    @Test
    fun `test that invoke never lowers a count below zero`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(
                rolledBack(d = Upload, n = 1),
                rolledBack(d = Download, n = 2),
                cleared(d = Upload, n = 3, batchId = 1),
                cleared(d = Download, n = 4, batchId = 1),
                evicted(d = Upload, n = 5, batchId = 1),
                evicted(d = Download, n = 6, batchId = 1),
                clearedEvicted(d = Upload, n = 7),
                clearedEvicted(d = Download, n = 8),
            ),
        )

        assertThat(result.lifetimeUploads).isEqualTo(tally(0, 0))
        assertThat(result.lifetimeDownloads).isEqualTo(tally(0, 0))
        assertThat(result.batchUploads).isEqualTo(tally(0, 0))
        assertThat(result.batchDownloads).isEqualTo(tally(0, 0))
        assertThat(result.batchEvictedUploads).isEqualTo(0)
        assertThat(result.batchEvictedDownloads).isEqualTo(0)
        assertThat(result.byMediaType).isEmpty()
    }

    @Test
    fun `test that invoke returns the same counts for no transitions`() {
        val before = counts(lifetimeUp = 3 to 1, batchUp = 3 to 1, media = mapOf(Image to 3))

        assertThat(underTest(before, emptyList())).isSameInstanceAs(before)
    }

    @Test
    fun `test that invoke counts each direction separately`() {
        val result = underTest(
            SyncTransferCounts.Empty,
            listOf(admitted(d = Upload, n = 1), admitted(d = Upload, n = 2), admitted(d = Download, n = 3)),
        )

        assertThat(result.lifetimeUploads).isEqualTo(tally(2, 2))
        assertThat(result.lifetimeDownloads).isEqualTo(tally(1, 1))
    }

    private fun tally(total: Int, pending: Int) = SyncTransferTally(total = total, pending = pending)

    private fun admitted(d: SyncTransferDirection = Upload, t: SyncMediaType = Image, n: Long) =
        SyncTransferTransition.Admitted(direction = d, mediaType = t, notificationNumber = n)

    private fun completed(d: SyncTransferDirection = Upload, n: Long) =
        SyncTransferTransition.Completed(direction = d, notificationNumber = n)

    private fun rolledBack(d: SyncTransferDirection = Upload, t: SyncMediaType = Image, n: Long) =
        SyncTransferTransition.RolledBack(direction = d, mediaType = t, notificationNumber = n)

    private fun cleared(
        d: SyncTransferDirection = Upload,
        t: SyncMediaType = Image,
        n: Long,
        batchId: Long,
    ) = SyncTransferTransition.Cleared(direction = d, mediaType = t, notificationNumber = n, batchId = batchId)

    private fun evicted(
        d: SyncTransferDirection = Upload,
        t: SyncMediaType = Image,
        n: Long,
        batchId: Long,
    ) = SyncTransferTransition.Evicted(direction = d, mediaType = t, notificationNumber = n, batchId = batchId)

    private fun clearedEvicted(d: SyncTransferDirection, n: Long) =
        SyncTransferTransition.ClearedEvicted(direction = d, notificationNumber = n)

    private fun counts(
        lifetimeUp: Pair<Int, Int> = 0 to 0,
        lifetimeDown: Pair<Int, Int> = 0 to 0,
        batchUp: Pair<Int, Int> = 0 to 0,
        batchDown: Pair<Int, Int> = 0 to 0,
        batchId: Long = 0,
        media: Map<SyncMediaType, Int> = emptyMap(),
        evictedUp: Int = 0,
        evictedDown: Int = 0,
    ) = SyncTransferCounts(
        lifetimeUploads = tally(lifetimeUp.first, lifetimeUp.second),
        lifetimeDownloads = tally(lifetimeDown.first, lifetimeDown.second),
        batchUploads = tally(batchUp.first, batchUp.second),
        batchDownloads = tally(batchDown.first, batchDown.second),
        batchId = batchId,
        byMediaType = media.toPersistentMap(),
        batchEvictedUploads = evictedUp,
        batchEvictedDownloads = evictedDown,
    )
}
