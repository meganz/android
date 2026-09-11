package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to stop tracking a Camera Uploads record whose node copy has finished
 * as a Camera Uploads in progress transfer
 */
class RemoveCameraUploadsCopyInProgressTransferUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     *
     * @param record the [CameraUploadsRecord] whose copy has finished
     */
    suspend operator fun invoke(record: CameraUploadsRecord) =
        cameraUploadsRepository.removeCameraUploadsCopyInProgressTransfer(record)
}
