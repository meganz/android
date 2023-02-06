package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetupPrimaryFolder]
 */
class DefaultSetupPrimaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetupPrimaryFolder {
    override suspend fun invoke(primaryHandle: Long) {
        cameraUploadRepository.setupPrimaryFolder(primaryHandle)
            .takeIf { it != cameraUploadRepository.getInvalidHandle() }
            ?.let {
                resetPrimaryTimeline()
                cameraUploadRepository.setPrimarySyncHandle(it)
                updateFolderIconBroadcast(it, false)
                updateFolderDestinationBroadcast(it, false)
            }
    }
}
