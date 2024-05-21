package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Use Case that retrieves the Camera Uploads Folder Node in Cloud Drive
 *
 * @property cameraUploadsRepository Repository containing all Camera Uploads related operations
 * @property getNodeByIdUseCase Gets the specific Node from a given ID
 * @property getPrimarySyncHandleUseCase Gets the Node Handle of the Camera Uploads Cloud Drive Folder
 * @property setupCameraUploadsSyncHandleUseCase Sets the Camera Uploads Cloud Drive Folder Sync
 * Handle
 */
class GetPrimaryFolderNodeUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase,
) {
    /**
     * Invocation function
     *
     * @param nodeId A nullable Primary Folder Node Handle. It defaults to null if no [NodeId] was
     * passed
     *
     * @return The Primary Folder Node, or null if it does not exist
     */
    suspend operator fun invoke(nodeId: NodeId? = null): TypedNode? {
        val actualNodeId = nodeId ?: NodeId(getPrimarySyncHandleUseCase())
        val invalidHandle = NodeId(cameraUploadsRepository.getInvalidHandle())

        return if (actualNodeId == invalidHandle) {
            null
        } else {
            getNodeByIdUseCase(actualNodeId).also {
                if (it == null) {
                    setupCameraUploadsSyncHandleUseCase(invalidHandle.longValue)
                }
            }
        }
    }
}