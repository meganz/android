package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlanSyncTransferClearUseCaseTest {

    private lateinit var underTest: PlanSyncTransferClearUseCase

    private val completed = listOf(
        completedRow(1, SyncTransferDirection.Upload, SyncMediaType.Image, n = 9L, batchId = 2L),
        completedRow(2, SyncTransferDirection.Download, SyncMediaType.Video, n = 8L, batchId = 2L),
        completedRow(3, SyncTransferDirection.Upload, SyncMediaType.Audio, n = 7L, batchId = 1L),
    )

    @BeforeAll
    fun setUp() {
        underTest = PlanSyncTransferClearUseCase()
    }

    @Test
    fun `test that invoke clears the selected rows in list order and never the evicted ones`() {
        val plan = underTest(completed, setOf(3, 1), counts(batchEvictedUploads = 2, batchEvictedDownloads = 1))

        assertThat(plan.kept).containsExactly(completed[1])
        assertThat(plan.transitions).containsExactly(
            SyncTransferTransition.Cleared(SyncTransferDirection.Upload, SyncMediaType.Image, 9L, 2L),
            SyncTransferTransition.Cleared(SyncTransferDirection.Upload, SyncMediaType.Audio, 7L, 1L),
        ).inOrder()
    }

    @Test
    fun `test that invoke clears every row and then the evicted ones of each direction`() {
        val plan = underTest(completed, tags = null, counts(batchEvictedUploads = 2, batchEvictedDownloads = 1))

        assertThat(plan.kept).isEmpty()
        assertThat(plan.transitions).containsExactly(
            SyncTransferTransition.Cleared(SyncTransferDirection.Upload, SyncMediaType.Image, 9L, 2L),
            SyncTransferTransition.Cleared(SyncTransferDirection.Download, SyncMediaType.Video, 8L, 2L),
            SyncTransferTransition.Cleared(SyncTransferDirection.Upload, SyncMediaType.Audio, 7L, 1L),
            SyncTransferTransition.ClearedEvicted(SyncTransferDirection.Upload, Long.MAX_VALUE),
            SyncTransferTransition.ClearedEvicted(SyncTransferDirection.Download, Long.MAX_VALUE),
        ).inOrder()
    }

    @Test
    fun `test that invoke clears the evicted rows of only the directions that have any`() {
        val plan = underTest(emptyList(), tags = null, counts(batchEvictedUploads = 0, batchEvictedDownloads = 3))

        assertThat(plan.kept).isNull()
        assertThat(plan.transitions)
            .containsExactly(SyncTransferTransition.ClearedEvicted(SyncTransferDirection.Download, Long.MAX_VALUE))
    }

    @ParameterizedTest(name = "{0} over {1} rows removes nothing")
    @MethodSource("selectionsThatRemoveNothing")
    fun `test that invoke keeps the list instance when nothing is cleared`(
        tags: Set<Int>?,
        rows: List<SyncCompletedTransfer>,
    ) {
        val plan = underTest(rows, tags, SyncTransferCounts.Empty)

        assertThat(plan.kept).isNull()
        assertThat(plan.transitions).isEmpty()
    }

    private fun selectionsThatRemoveNothing(): Stream<Arguments> = Stream.of(
        Arguments.of(emptySet<Int>(), completed),
        Arguments.of(setOf(99), completed),
        Arguments.of(null, emptyList<SyncCompletedTransfer>()),
    )

    private fun counts(batchEvictedUploads: Int, batchEvictedDownloads: Int) = SyncTransferCounts.Empty.copy(
        batchEvictedUploads = batchEvictedUploads,
        batchEvictedDownloads = batchEvictedDownloads,
    )

    private fun completedRow(
        tag: Int,
        direction: SyncTransferDirection,
        mediaType: SyncMediaType,
        n: Long,
        batchId: Long,
    ) = SyncCompletedTransfer(
        entry = SyncTransferEntry(
            tag = tag,
            direction = direction,
            state = TransferState.STATE_COMPLETED,
            fileName = "f$tag",
            totalBytes = 100L,
            transferredBytes = 100L,
            speed = 0L,
            meanSpeed = 0L,
            priority = tag.toLong(),
            notificationNumber = n,
            lastError = null,
            isForeignOverQuota = false,
            path = "/sync/f$tag",
        ),
        mediaType = mediaType,
        finishedAtMillis = 0L,
        batchId = batchId,
    )
}
