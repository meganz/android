package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Set the generated fingerprint for the camera uploads record
 */
class SetCameraUploadsRecordGeneratedFingerprintUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Set the generated fingerprint for the camera uploads record
     *
     * @param mediaId the id of the record
     * @param timestamp the timestamp of the record
     * @param folderType the folder type of the record
     * @param generatedFingerprint the fingerprint computed from the generated file
     */
    suspend operator fun invoke(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    ) = cameraUploadRepository.setRecordGeneratedFingerprint(
        mediaId = mediaId,
        timestamp = timestamp,
        folderType = folderType,
        generatedFingerprint = generatedFingerprint,
    )
}
