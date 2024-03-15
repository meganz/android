package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.BroadcastFolderDestinationUpdateUseCase
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import javax.inject.Inject

/**
 * Set the camera uploads primary nodeId in local preferences
 */
class SetPrimaryNodeIdUseCase @Inject constructor(
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase,
    private val broadcastFolderDestinationUpdateUseCase: BroadcastFolderDestinationUpdateUseCase,
) {

    /**
     * Invoke
     *
     * @param nodeId the NodeId to set
     */
    suspend operator fun invoke(nodeId: NodeId) {
        setupCameraUploadsSyncHandleUseCase(nodeId.longValue)
        broadcastFolderDestinationUpdateUseCase(nodeHandle = nodeId.longValue, isSecondary = false)
    }
}
