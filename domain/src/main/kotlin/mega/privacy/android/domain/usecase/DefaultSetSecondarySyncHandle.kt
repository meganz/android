package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSyncHandleUseCase
import javax.inject.Inject

/**
 * Default implementation of [SetSecondarySyncHandle]
 */
class DefaultSetSecondarySyncHandle @Inject constructor(
    private val setupMediaUploadsSyncHandleUseCase: SetupMediaUploadsSyncHandleUseCase,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetSecondarySyncHandle {
    override suspend fun invoke(newSecondaryHandle: Long) {
        setupMediaUploadsSyncHandleUseCase(newSecondaryHandle)
        updateFolderIconBroadcast(nodeHandle = newSecondaryHandle, isSecondary = true)
        updateFolderDestinationBroadcast(nodeHandle = newSecondaryHandle, isSecondary = true)
    }
}
