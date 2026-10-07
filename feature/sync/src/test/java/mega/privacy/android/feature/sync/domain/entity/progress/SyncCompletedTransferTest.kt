package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import org.junit.jupiter.api.Test

internal class SyncCompletedTransferTest {

    @Test
    fun `test that averageSpeed falls back to the total bytes when the mean speed is zero`() {
        assertThat(completed(meanSpeed = 0, total = 4_096).averageSpeed).isEqualTo(4_096)
    }

    @Test
    fun `test that averageSpeed returns the mean speed when the sdk reported one`() {
        assertThat(completed(meanSpeed = 700, total = 4_096).averageSpeed).isEqualTo(700)
    }

    private fun completed(meanSpeed: Long, total: Long) = SyncCompletedTransfer(
        entry = SyncTransferEntry(
            tag = 1,
            direction = SyncTransferDirection.Upload,
            state = TransferState.STATE_COMPLETED,
            fileName = "a.jpg",
            totalBytes = total,
            transferredBytes = total,
            speed = 0,
            meanSpeed = meanSpeed,
            priority = 1,
            notificationNumber = 1,
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Image,
        finishedAtMillis = 0,
        batchId = 0,
    )
}
