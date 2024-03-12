package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.BroadcastFolderDestinationUpdateUseCase
import javax.inject.Inject

/**
 * Set the camera uploads secondary handle in local preferences
 */
class SetSecondaryNodeIdUseCase @Inject constructor(
    private val setupMediaUploadsSyncHandleUseCase: SetupMediaUploadsSyncHandleUseCase,
    private val broadcastFolderDestinationUpdateUseCase: BroadcastFolderDestinationUpdateUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(nodeId: NodeId) {
        setupMediaUploadsSyncHandleUseCase(nodeId.longValue)
        broadcastFolderDestinationUpdateUseCase(nodeHandle = nodeId.longValue, isSecondary = true)
    }
}
