package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncTransferBatchProgressTest {

    @ParameterizedTest
    @MethodSource("provideDesktopPillSteps")
    fun `test that batchProgress counts one ongoing transfer per pending direction`(
        batchUploads: Pair<Int, Int>,
        batchDownloads: Pair<Int, Int>,
        expected: SyncTransferBatchProgress?,
    ) {
        val counts = counts(batchUp = batchUploads, batchDown = batchDownloads)

        assertThat(counts.batchProgress()).isEqualTo(expected)
    }

    @Test
    fun `test that batchProgress is null while the batch is empty`() {
        assertThat(SyncTransferCounts.Empty.batchProgress()).isNull()
    }

    @Test
    fun `test that batchProgress lowers both numbers when completed rows of the batch are cleared`() {
        assertThat(counts(batchUp = 10 to 8).batchProgress())
            .isEqualTo(SyncTransferBatchProgress(current = 3, total = 10))
        assertThat(counts(batchUp = 8 to 8).batchProgress())
            .isEqualTo(SyncTransferBatchProgress(current = 1, total = 8))
    }

    @Test
    fun `test that batchProgress keeps evicted completions until they are cleared`() {
        val counts = counts(batchUp = 5000 to 500, evictedUp = 4000)

        assertThat(counts.batchProgress())
            .isEqualTo(SyncTransferBatchProgress(current = 4501, total = 5000))
    }

    @Test
    fun `test that batchProgress counts completed rows of both directions`() {
        val counts = counts(batchUp = 3 to 1, batchDown = 2 to 0)

        assertThat(counts.batchProgress())
            .isEqualTo(SyncTransferBatchProgress(current = 5, total = 5))
    }

    private fun counts(
        batchUp: Pair<Int, Int> = 0 to 0,
        batchDown: Pair<Int, Int> = 0 to 0,
        evictedUp: Int = 0,
    ) = SyncTransferCounts.Empty.copy(
        batchUploads = SyncTransferTally(total = batchUp.first, pending = batchUp.second),
        batchDownloads = SyncTransferTally(total = batchDown.first, pending = batchDown.second),
        batchEvictedUploads = evictedUp,
    )

    private fun provideDesktopPillSteps() = listOf(
        Arguments.of(3 to 3, 0 to 0, SyncTransferBatchProgress(current = 1, total = 3)),
        Arguments.of(3 to 2, 0 to 0, SyncTransferBatchProgress(current = 2, total = 3)),
        Arguments.of(3 to 1, 0 to 0, SyncTransferBatchProgress(current = 3, total = 3)),
        Arguments.of(0 to 0, 0 to 0, null),
        Arguments.of(2 to 2, 2 to 2, SyncTransferBatchProgress(current = 2, total = 4)),
    )
}
