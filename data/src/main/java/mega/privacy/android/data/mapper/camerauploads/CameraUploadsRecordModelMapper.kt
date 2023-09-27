package mega.privacy.android.data.mapper.camerauploads

import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class CameraUploadsRecordModelMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(cameraUploadsRecordEntity: CameraUploadsRecordEntity): CameraUploadsRecord {
        coroutineContext.ensureActive()

        val mediaId = decryptData(cameraUploadsRecordEntity.encryptedMediaId)?.toLong()
        requireNotNull(mediaId)
        val timestamp = decryptData(cameraUploadsRecordEntity.encryptedTimestamp)?.toLong()
        requireNotNull(timestamp)

        return CameraUploadsRecord(
            mediaId = mediaId,
            timestamp = timestamp,
            folderType = cameraUploadsRecordEntity.folderType,
            fileName = decryptData(cameraUploadsRecordEntity.encryptedFileName) ?: "",
            filePath = decryptData(cameraUploadsRecordEntity.encryptedFilePath) ?: "",
            type = cameraUploadsRecordEntity.fileType,
            uploadStatus = cameraUploadsRecordEntity.uploadStatus,
            originalFingerprint =
            decryptData(cameraUploadsRecordEntity.encryptedOriginalFingerprint) ?: "",
            generatedFingerprint =
            decryptData(cameraUploadsRecordEntity.encryptedGeneratedFingerprint),
            tempFilePath = decryptData(cameraUploadsRecordEntity.encryptedTempFilePath) ?: ""
        )
    }
}
