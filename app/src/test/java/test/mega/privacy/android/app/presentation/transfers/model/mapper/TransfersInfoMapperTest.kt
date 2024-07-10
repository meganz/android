package test.mega.privacy.android.app.presentation.transfers.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TransfersInfoMapperTest {
    private val underTest = TransfersInfoMapper()

    @Nested
    @DisplayName("test that correct progress is returned")
    inner class CorrectProgress {
        @Test
        fun `test that the correct progress is returned when the transfer is in progress`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.completedProgress).isEqualTo(0.5f)
        }

        @Test
        fun `test that the completed Progress is 0 when totalSizeToTransfer is 0`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 1,
                totalSizeToTransfer = 0L,
                totalSizeTransferred = 0L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.completedProgress).isEqualTo(0)
        }

    }

    @Nested
    @DisplayName("test that correct status is returned")
    inner class CorrectStatus {

        @Test
        fun `test that if there are no transfers then returns not transferring status`() {
            val result = underTest(
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.NotTransferring)
        }

        @Test
        fun `test that paused status is returned regardless of errors or over quota when transfer is paused`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = true,
                isTransferError = true,
                isTransferOverQuota = true,
                isStorageOverQuota = true,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.Paused)
        }

        @Test
        fun `test that transfer error status is returned when transfer has error`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = true,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.TransferError)
        }

        @Test
        fun `test that transfer over quota status is returned when there is storage over quota and pending uploads`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = true,
                isTransferOverQuota = false,
                isStorageOverQuota = true,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.OverQuota)
        }

        @Test
        fun `test that Transferring status is returned when there is storage over quota but no pending uploads`() {
            val result = underTest(
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = true,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.Transferring)
        }

        @Test
        fun `test that transfer over quota status is returned when there is transfer over quota and pending downloads`() {
            val result = underTest(
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = true,
                isStorageOverQuota = false,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.OverQuota)
        }

        @Test
        fun `test that Transferring status is returned when there is transfer over quota but no pending downloads`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = true,
                isStorageOverQuota = false,
            )
            assertThat(result.status).isEqualTo(TransfersStatus.Transferring)
        }
    }

    @Nested
    @DisplayName("test that correct uploading flag is returned")
    inner class CorrectUpload {

        @Test
        fun `test that the upload flag is true when upload status is uploading and there are pending uploads`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.uploading).isTrue()
        }

        @Test
        fun `test that the upload flag is false when upload status is uploading but there are no pending uploads`() {
            val result = underTest(
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 0,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.uploading).isFalse()
        }

        @Test
        fun `test that the upload flag is false when there are downloads`() {
            val result = underTest(
                numPendingUploads = 0,
                numPendingDownloadsNonBackground = 1,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.uploading).isFalse()
        }

        @Test
        fun `test that the upload flag is true when there are uploads and no downloads`() {
            val result = underTest(
                numPendingUploads = 1,
                numPendingDownloadsNonBackground = 1,
                totalSizeToTransfer = 10L,
                totalSizeTransferred = 5L,
                areTransfersPaused = false,
                isTransferError = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
            assertThat(result.uploading).isTrue()
        }
    }
}