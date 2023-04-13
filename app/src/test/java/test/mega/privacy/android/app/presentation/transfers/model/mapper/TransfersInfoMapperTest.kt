package test.mega.privacy.android.app.presentation.transfers.model.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.domain.entity.TransfersStatus
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TransfersInfoMapperTest {
    private val underTest = TransfersInfoMapper()

    @Nested
    @DisplayName("test that correct progress is returned")
    inner class CorrectProgress {
        @Test
        fun `test that when transfer is in progress then correct progress is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.completedProgress).isEqualTo(0.5f)
        }

        @Test
        fun `test that if totalSizePendingTransfer is 0 then completed Progress is 0`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 0L,
                totalSizeTransferred = 0L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.completedProgress).isEqualTo(0)
        }

    }

    @Nested
    @DisplayName("test that correct status is returned")
    inner class CorrectStatus {

        @Test
        fun `test that if there are no transfers then returns not transferring status`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.NotTransferring)
        }

        @Test
        fun `test that when transfer is paused then paused status is returned regardless of errors or over quota`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = true,
                isTransferError = true,
                isTransferOverQuota = true,
                isStorageOverQuota = true,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.Paused)
        }

        @Test
        fun `test that when transfer has error then transfer error status is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = true,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.TransferError)
        }

        @Test
        fun `test that when there is storage over quota and pending uploads then transfer over quota status is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = true,
                isTransferOverQuota = false,
                isStorageOverQuota = true,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.OverQuota)
        }

        @Test
        fun `test that when there is storage over quota but no pending uploads then Transferring status is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = true,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.Transferring)
        }

        @Test
        fun `test that when there is transfer over quota and pending downloads then transfer over quota status is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = true,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.OverQuota)
        }

        @Test
        fun `test that when there is transfer over quota but no pending downloads then Transferring status is returned`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = true,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.status).isEqualTo(TransfersStatus.Transferring)
        }
    }

    @Nested
    @DisplayName("test that correct uploading flag is returned")
    inner class CorrectUpload {

        @Test
        fun `test that when upload status is uploading and there are pending uploads then upload flag is true`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.uploading).isTrue()
        }

        @Test
        fun `test that when upload status is uploading but there are no pending uploads then upload flag is false`() {
            val result = underTest(
                transferType = TransferType.TYPE_UPLOAD,
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 0,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.uploading).isFalse()
        }

        @Test
        fun `test that when upload status is downloading and there are downloads then upload flag is false`() {
            val result = underTest(
                transferType = TransferType.TYPE_DOWNLOAD,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.uploading).isFalse()
        }

        @Test
        fun `test that when upload status is none and there are downloads then upload flag is false`() {
            val result = underTest(
                transferType = TransferType.NONE,
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.uploading).isFalse()
        }

        @Test
        fun `test that when upload status is none and there are uploads and no downloads then upload flag is true`() {
            val result = underTest(
                transferType = TransferType.NONE,
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 1,
                totalSizePendingTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            Truth.assertThat(result.uploading).isTrue()
        }
    }
}