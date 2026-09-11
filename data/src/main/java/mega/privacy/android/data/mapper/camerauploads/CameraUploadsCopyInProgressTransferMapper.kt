package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import java.math.BigInteger
import javax.inject.Inject

/**
 * Maps a [CameraUploadsRecord] being copied from an existing node into an [InProgressTransfer.Upload]
 * so that it can be listed along with the Camera Uploads SDK transfers.
 *
 * SDK transfer unique ids and tags are positive, so the negated media id is used as key
 * to guarantee it never collides with a real transfer.
 */
internal class CameraUploadsCopyInProgressTransferMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param record the [CameraUploadsRecord] being copied
     * @return the queued [InProgressTransfer.Upload] representing the copy
     */
    operator fun invoke(record: CameraUploadsRecord): InProgressTransfer.Upload {
        val uniqueId = -record.mediaId
        return InProgressTransfer.Upload(
            uniqueId = uniqueId,
            tag = uniqueId.toInt(),
            totalBytes = record.fileSize,
            isPaused = false,
            fileName = record.generatedFileName ?: record.fileName,
            speed = 0L,
            state = TransferState.STATE_QUEUED,
            priority = BigInteger.ZERO,
            progress = Progress(0f),
            localPath = record.filePath,
        )
    }
}
