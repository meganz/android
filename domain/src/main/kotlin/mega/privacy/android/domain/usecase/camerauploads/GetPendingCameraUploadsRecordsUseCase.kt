package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get the records to upload through Camera Uploads
 *
 * @param cameraUploadRepository
 */
class GetPendingCameraUploadsRecordsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Get the records with upload status PENDING, STARTED and FAILED from the database
     *
     * @param types a list of types (Photos, Videos or both) to filter the list with
     * @return the list of CameraUploadsRecord with status PENDING, STARTED and FAILED and matching the type
     */
    suspend operator fun invoke(
        types: List<SyncRecordType>,
    ): List<CameraUploadsRecord> =
        cameraUploadRepository.getCameraUploadsRecordByUploadStatusAndTypes(
            uploadStatus = listOf(
                CameraUploadsRecordUploadStatus.PENDING,
                CameraUploadsRecordUploadStatus.STARTED,
                CameraUploadsRecordUploadStatus.FAILED,
            ),
            types = types,
        )
}
