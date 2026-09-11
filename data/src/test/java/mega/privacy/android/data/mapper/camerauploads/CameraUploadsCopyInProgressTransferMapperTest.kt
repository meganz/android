package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsCopyInProgressTransferMapperTest {

    private val underTest = CameraUploadsCopyInProgressTransferMapper()

    @Test
    fun `test that mapper returns a queued upload keyed by the negated media id`() {
        val record = createRecord(generatedFileName = "generated.jpg")

        val expected = InProgressTransfer.Upload(
            uniqueId = -record.mediaId,
            tag = (-record.mediaId).toInt(),
            totalBytes = record.fileSize,
            isPaused = false,
            fileName = "generated.jpg",
            speed = 0L,
            state = TransferState.STATE_QUEUED,
            priority = BigInteger.ZERO,
            progress = Progress(0f),
            localPath = record.filePath,
        )

        assertThat(underTest(record)).isEqualTo(expected)
    }

    @Test
    fun `test that mapper falls back to the record file name when the generated file name is null`() {
        val record = createRecord(generatedFileName = null)

        assertThat(underTest(record).fileName).isEqualTo(record.fileName)
    }

    private fun createRecord(generatedFileName: String?) = CameraUploadsRecord(
        mediaId = 1234L,
        fileName = "original.jpg",
        filePath = "/storage/DCIM/original.jpg",
        timestamp = 1L,
        folderType = CameraUploadFolderType.Primary,
        type = CameraUploadsRecordType.TYPE_PHOTO,
        uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
        originalFingerprint = "fingerprint",
        generatedFingerprint = null,
        tempFilePath = "/cache/original.jpg",
        generatedFileName = generatedFileName,
        fileSize = 2048L,
    )
}
