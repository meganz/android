package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

class MonitorCameraUploadsInProgressTransfersUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository
) {
    /**
     * Invoke
     *
     * @return a flow of Map. Being the key an [Int] representing the Camera Uploads transfer uniqueId and [InProgressTransfer] as its value.
     */
    operator fun invoke() = cameraUploadsRepository.monitorCameraUploadsInProgressTransfers()
}