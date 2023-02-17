package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetSecondarySyncHandle]
 */
class DefaultSetSecondarySyncHandle @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetSecondarySyncHandle {
    override suspend fun invoke(newSecondaryHandle: Long) {
        cameraUploadRepository.setSecondarySyncHandle(newSecondaryHandle)
        updateFolderIconBroadcast(nodeHandle = newSecondaryHandle, isSecondary = true)
        updateFolderDestinationBroadcast(nodeHandle = newSecondaryHandle, isSecondary = true)
    }
}
