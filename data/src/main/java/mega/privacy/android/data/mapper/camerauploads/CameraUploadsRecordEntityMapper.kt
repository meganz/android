package mega.privacy.android.data.mapper.camerauploads

import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class CameraUploadsRecordEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(cameraUploadsRecord: CameraUploadsRecord): CameraUploadsRecordEntity {
        coroutineContext.ensureActive()

        val mediaId = encryptData(cameraUploadsRecord.mediaId.toString())
        requireNotNull(mediaId)
        val timestamp = encryptData(cameraUploadsRecord.timestamp.toString())
        requireNotNull(timestamp)

        return CameraUploadsRecordEntity(
            encryptedMediaId = mediaId,
            encryptedTimestamp = timestamp,
            folderType = cameraUploadsRecord.folderType,
            encryptedFileName = encryptData(cameraUploadsRecord.fileName),
            encryptedFilePath = encryptData(cameraUploadsRecord.filePath),
            fileType = cameraUploadsRecord.type,
            uploadStatus = cameraUploadsRecord.uploadStatus,
            encryptedOriginalFingerprint = encryptData(cameraUploadsRecord.originalFingerprint),
            encryptedGeneratedFingerprint = encryptData(cameraUploadsRecord.generatedFingerprint),
            encryptedTempFilePath = encryptData(cameraUploadsRecord.tempFilePath),
        )
    }
}
