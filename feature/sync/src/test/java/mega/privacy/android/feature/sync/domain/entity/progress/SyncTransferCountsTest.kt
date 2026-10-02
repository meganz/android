package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SyncTransferCountsTest {

    @Test
    fun `test that pendingSyncTransfers sums both directions`() {
        val counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(total = 4, pending = 2),
            lifetimeDownloads = SyncTransferTally(total = 6, pending = 3),
        )

        assertThat(counts.pendingSyncTransfers).isEqualTo(5)
    }

    @Test
    fun `test that completedTransfers is the lifetime total minus pending`() {
        val counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(total = 5, pending = 2),
            lifetimeDownloads = SyncTransferTally(total = 3, pending = 3),
        )

        assertThat(counts.completedTransfers).isEqualTo(3)
    }

    @Test
    fun `test that completed is the tally total minus pending`() {
        assertThat(SyncTransferTally(total = 7, pending = 4).completed).isEqualTo(3)
    }

    @Test
    fun `test that Empty has batch id zero and no media or evicted counts`() {
        val empty = SyncTransferCounts.Empty
        val zero = SyncTransferTally(total = 0, pending = 0)

        assertThat(empty.batchId).isEqualTo(0L)
        assertThat(empty.byMediaType).isEmpty()
        assertThat(empty.lifetimeUploads).isEqualTo(zero)
        assertThat(empty.lifetimeDownloads).isEqualTo(zero)
        assertThat(empty.batchUploads).isEqualTo(zero)
        assertThat(empty.batchDownloads).isEqualTo(zero)
        assertThat(empty.batchEvictedUploads).isEqualTo(0)
        assertThat(empty.batchEvictedDownloads).isEqualTo(0)
    }
}
