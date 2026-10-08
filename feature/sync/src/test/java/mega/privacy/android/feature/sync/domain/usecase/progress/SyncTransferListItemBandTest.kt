package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SyncTransferListItemBandTest {

    @Test
    fun `test that band returns the finished band when the row is completed`() {
        val item = SyncTransferListItem.Finished(createFinished(tag = 1, finishedAt = 1_000))

        assertThat(item.band).isEqualTo(3)
    }

    @ParameterizedTest
    @EnumSource(SyncTransferStatus::class)
    fun `test that band returns the band of the status when the row is active`(
        status: SyncTransferStatus,
    ) {
        val item = SyncTransferListItem.Active(createRow(tag = 1), status, isOwnPause = false)

        assertThat(item.band).isEqualTo(expectedBands.getValue(status))
    }

    private val expectedBands = mapOf(
        SyncTransferStatus.Completing to 0,
        SyncTransferStatus.Starting to 1,
        SyncTransferStatus.Transferring to 1,
        SyncTransferStatus.Queued to 1,
        SyncTransferStatus.Retrying to 1,
        SyncTransferStatus.OutOfTransferQuota to 1,
        SyncTransferStatus.OutOfStorageSpace to 1,
        SyncTransferStatus.Paused to 2,
        SyncTransferStatus.PausedInQueue to 2,
        SyncTransferStatus.Completed to 3,
    )

    private fun createEntry(tag: Int, state: TransferState) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "f$tag.txt",
        totalBytes = 100,
        transferredBytes = 0,
        speed = 0,
        meanSpeed = 0,
        priority = tag.toLong(),
        notificationNumber = tag.toLong(),
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private fun createRow(tag: Int) = SyncTransferRow(
        entry = createEntry(tag, TransferState.STATE_QUEUED),
        mediaType = SyncMediaType.Document,
        optimisticPause = null,
    )

    private fun createFinished(tag: Int, finishedAt: Long) = SyncCompletedTransfer(
        entry = createEntry(tag, TransferState.STATE_COMPLETED),
        mediaType = SyncMediaType.Document,
        finishedAtMillis = finishedAt,
        batchId = 0,
    )
}
