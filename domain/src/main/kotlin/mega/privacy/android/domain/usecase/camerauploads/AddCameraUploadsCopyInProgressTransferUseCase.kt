package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to track a Camera Uploads record being copied from an existing node
 * as a Camera Uploads in progress transfer
 */
class AddCameraUploadsCopyInProgressTransferUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     *
     * @param record the [CameraUploadsRecord] being copied
     */
    suspend operator fun invoke(record: CameraUploadsRecord) =
        cameraUploadsRepository.addCameraUploadsCopyInProgressTransfer(record)
}
