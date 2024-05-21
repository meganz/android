package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Save a list of [CameraUploadsRecord] in the database
 */
class SaveCameraUploadsRecordUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Save a list of [CameraUploadsRecord] in the database
     *
     * @param records the list to save in the database
     */
    suspend operator fun invoke(records: List<CameraUploadsRecord>) =
        cameraUploadsRepository.insertOrUpdateCameraUploadsRecords(records)
}
