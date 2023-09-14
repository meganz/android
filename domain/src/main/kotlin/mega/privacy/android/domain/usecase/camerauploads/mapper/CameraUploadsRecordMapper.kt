package mega.privacy.android.domain.usecase.camerauploads.mapper

import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.usecase.GetDeviceCurrentNanoTimeUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import javax.inject.Inject

/**
 * Mapper for converting a [CameraUploadsMedia] into [CameraUploadsRecord].
 */
class CameraUploadsRecordMapper @Inject constructor(
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val getDeviceCurrentNanoTimeUseCase: GetDeviceCurrentNanoTimeUseCase,
) {

    /**
     * Mapper for converting a [CameraUploadsMedia] into [CameraUploadsRecord].
     *
     * @param media
     * @param folderType
     * @param fileType
     * @param tempRoot
     * @return [CameraUploadsRecord]
     */
    suspend operator fun invoke(
        media: CameraUploadsMedia,
        folderType: CameraUploadFolderType,
        fileType: SyncRecordType,
        tempRoot: String,
    ): CameraUploadsRecord? {
        val fingerprint = getFingerprintUseCase(media.filePath) ?: return null

        val extension = media.displayName.substringAfterLast('.', "")

        return CameraUploadsRecord(
            mediaId = media.mediaId,
            fileName = media.displayName,
            filePath = media.filePath,
            timestamp = media.timestamp,
            folderType = folderType,
            type = fileType,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            originalFingerprint = fingerprint,
            generatedFingerprint = null,
            tempFilePath = "$tempRoot${getDeviceCurrentNanoTimeUseCase()}.$extension"
        )
    }
}
