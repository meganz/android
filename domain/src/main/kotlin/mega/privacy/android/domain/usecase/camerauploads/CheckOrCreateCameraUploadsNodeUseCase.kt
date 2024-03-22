package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import javax.inject.Inject

/**
 * Check or create the Upload Node for Camera Uploads.
 * - Check the existence of the Upload Node stored in local preferences
 * - If Upload Node not valid, check the existence of the Default Upload Node and set the new Upload Node to local preferences
 * - If Default Node not valid, create a new Default Upload Node and set the new Upload node in user attributes and local preferences
 */
class CheckOrCreateCameraUploadsNodeUseCase @Inject constructor(
    private val isCameraUploadsNodeValidUseCase: IsCameraUploadsNodeValidUseCase,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase,
    private val setPrimaryNodeIdUseCase: SetPrimaryNodeIdUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
    private val setSecondaryNodeIdUseCase: SetSecondaryNodeIdUseCase,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke(folderName: String, folderType: CameraUploadFolderType) {
        val nodeId = NodeId(getUploadFolderHandleUseCase(folderType))
        if (!isCameraUploadsNodeValidUseCase(nodeId)) {
            val defaultNodeId = NodeId(getDefaultNodeHandleUseCase(folderName))
            when (isCameraUploadsNodeValidUseCase(defaultNodeId)) {
                true -> setNodeId(defaultNodeId, folderType)
                false -> createNode(folderName, folderType)
            }
        }
    }

    /**
     *  Set the new Upload NodeId in local preferences
     *
     *  @param nodeId new Upload Node NodeId
     *  @param folderType Primary or Secondary
     */
    private suspend fun setNodeId(nodeId: NodeId, folderType: CameraUploadFolderType) {
        when (folderType) {
            CameraUploadFolderType.Primary -> setPrimaryNodeIdUseCase(nodeId)
            CameraUploadFolderType.Secondary -> setSecondaryNodeIdUseCase(nodeId)
        }

    }

    /**
     *  Create a new Upload Node set the new Upload NodeId in user attributes and local preferences
     *
     *  @param folderName the name of the new Node
     *  @param folderType Primary or Secondary
     */
    private suspend fun createNode(folderName: String, folderType: CameraUploadFolderType) {
        when (folderType) {
            CameraUploadFolderType.Primary ->
                setupPrimaryFolderUseCase(createFolderNodeUseCase(folderName).longValue)

            CameraUploadFolderType.Secondary ->
                setupSecondaryFolderUseCase(createFolderNodeUseCase(folderName).longValue)
        }
    }
}
