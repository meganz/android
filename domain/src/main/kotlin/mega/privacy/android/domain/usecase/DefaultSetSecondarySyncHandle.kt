package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSyncHandleUseCase
import javax.inject.Inject

/**
 * Default implementation of [SetSecondarySyncHandle]
 */
class DefaultSetSecondarySyncHandle @Inject constructor(
    private val setupMediaUploadsSyncHandleUseCase: SetupMediaUploadsSyncHandleUseCase,
    private val broadcastFolderDestinationUpdateUseCase: BroadcastFolderDestinationUpdateUseCase,
) : SetSecondarySyncHandle {
    override suspend fun invoke(newSecondaryHandle: Long) {
        setupMediaUploadsSyncHandleUseCase(newSecondaryHandle)
        broadcastFolderDestinationUpdateUseCase(nodeHandle = newSecondaryHandle, isSecondary = true)
    }
}
