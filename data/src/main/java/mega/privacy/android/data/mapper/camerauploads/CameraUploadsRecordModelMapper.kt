package mega.privacy.android.data.mapper.camerauploads

import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class CameraUploadsRecordModelMapper @Inject constructor() {
    suspend operator fun invoke(cameraUploadsRecordEntity: CameraUploadsRecordEntity): CameraUploadsRecord {
        coroutineContext.ensureActive()

        return with(cameraUploadsRecordEntity) {
            CameraUploadsRecord(
                mediaId = mediaId,
                timestamp = timestamp,
                folderType = folderType,
                fileName = fileName,
                filePath = filePath,
                type = fileType,
                uploadStatus = uploadStatus,
                originalFingerprint = originalFingerprint,
                generatedFingerprint = generatedFingerprint,
                tempFilePath = tempFilePath,
            )
        }
    }
}
