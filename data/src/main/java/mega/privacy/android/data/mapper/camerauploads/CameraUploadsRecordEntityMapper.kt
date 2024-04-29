package mega.privacy.android.data.mapper.camerauploads

import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class CameraUploadsRecordEntityMapper @Inject constructor() {
    suspend operator fun invoke(cameraUploadsRecord: CameraUploadsRecord): CameraUploadsRecordEntity {
        coroutineContext.ensureActive()

        return with(cameraUploadsRecord) {
            CameraUploadsRecordEntity(
                mediaId = mediaId,
                timestamp = timestamp,
                folderType = cameraUploadsRecord.folderType,
                fileName = cameraUploadsRecord.fileName,
                filePath = cameraUploadsRecord.filePath,
                fileType = cameraUploadsRecord.type,
                uploadStatus = cameraUploadsRecord.uploadStatus,
                originalFingerprint = cameraUploadsRecord.originalFingerprint,
                generatedFingerprint = cameraUploadsRecord.generatedFingerprint,
                tempFilePath = cameraUploadsRecord.tempFilePath,
            )

        }
    }
}
