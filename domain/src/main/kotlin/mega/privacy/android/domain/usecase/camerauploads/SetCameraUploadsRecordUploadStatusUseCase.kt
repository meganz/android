package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Set the upload status for the camera uploads record
 */
class SetCameraUploadsRecordUploadStatusUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Set the upload status for the camera uploads record
     *
     * @param mediaId the id of the record
     * @param timestamp the timestamp of the record
     * @param folderType the folder type of the record
     * @param uploadStatus the upload status to set
     */
    suspend operator fun invoke(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        uploadStatus: CameraUploadsRecordUploadStatus,
    ) = cameraUploadRepository.setRecordUploadStatus(
        mediaId = mediaId,
        timestamp = timestamp,
        folderType = folderType,
        uploadStatus = uploadStatus,
    )
}
