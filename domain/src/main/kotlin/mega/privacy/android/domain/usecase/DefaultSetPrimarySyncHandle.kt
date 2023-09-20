package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import javax.inject.Inject

/**
 * Default implementation of [SetPrimarySyncHandle]
 */
class DefaultSetPrimarySyncHandle @Inject constructor(
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetPrimarySyncHandle {
    override suspend fun invoke(newPrimaryHandle: Long) {
        setupCameraUploadsSyncHandleUseCase(newPrimaryHandle)
        updateFolderIconBroadcast(nodeHandle = newPrimaryHandle, isSecondary = false)
        updateFolderDestinationBroadcast(nodeHandle = newPrimaryHandle, isSecondary = false)
    }
}
