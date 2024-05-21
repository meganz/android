package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Send broadcast to update folder destination for camera upload
 *
 */
class BroadcastFolderDestinationUpdateUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invoke
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary folder
     */
    suspend operator fun invoke(nodeHandle: Long, isSecondary: Boolean) =
        cameraUploadsRepository.sendUpdateFolderDestinationBroadcast(nodeHandle, isSecondary)
}
