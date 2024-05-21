package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Use Case that retrieves the Media Uploads Folder Node in Cloud Drive
 *
 * @property cameraUploadsRepository Repository containing all Camera Uploads related operations
 * @property getNodeByIdUseCase Gets the specific Node from a given ID
 * @property getSecondarySyncHandleUseCase Gets the Node Handle of the Media Uploads Cloud Drive Folder
 * @property setupMediaUploadsSyncHandleUseCase Sets the Media Uploads Cloud Drive Folder Sync Handle
 */
class GetSecondaryFolderNodeUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val setupMediaUploadsSyncHandleUseCase: SetupMediaUploadsSyncHandleUseCase,
) {
    /**
     * Invocation function
     *
     * @param nodeId A nullable Secondary Folder Node Handle. It defaults to null if no [NodeId] was
     * passed
     *
     * @return The Secondary Folder Node, or null if it does not exist
     */
    suspend operator fun invoke(nodeId: NodeId? = null): TypedNode? {
        val actualNodeId = nodeId ?: NodeId(getSecondarySyncHandleUseCase())
        val invalidHandle = NodeId(cameraUploadsRepository.getInvalidHandle())

        return if (actualNodeId == invalidHandle) {
            null
        } else {
            getNodeByIdUseCase(actualNodeId).also {
                if (it == null) {
                    setupMediaUploadsSyncHandleUseCase(invalidHandle.longValue)
                }
            }
        }
    }
}