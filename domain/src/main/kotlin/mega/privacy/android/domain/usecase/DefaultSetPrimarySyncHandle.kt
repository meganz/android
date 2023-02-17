package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetPrimarySyncHandle]
 */
class DefaultSetPrimarySyncHandle @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetPrimarySyncHandle {
    override suspend fun invoke(newPrimaryHandle: Long) {
        cameraUploadRepository.setPrimarySyncHandle(newPrimaryHandle)
        updateFolderIconBroadcast(nodeHandle = newPrimaryHandle, isSecondary = false)
        updateFolderDestinationBroadcast(nodeHandle = newPrimaryHandle, isSecondary = false)
    }
}
