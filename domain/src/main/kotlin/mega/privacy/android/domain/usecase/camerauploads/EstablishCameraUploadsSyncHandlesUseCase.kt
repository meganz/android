package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Use Case that performs the following functions:
 *
 * 1. Retrieve the Camera Uploads Sync Handles from the API
 * 2. Set the Sync Handles to the local Database
 */
class EstablishCameraUploadsSyncHandlesUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val setPrimaryNodeIdUseCase: SetPrimaryNodeIdUseCase,
    private val setSecondaryNodeIdUseCase: SetSecondaryNodeIdUseCase,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        getCameraUploadsSyncHandlesUseCase()?.let { (primaryHandle, secondaryHandle) ->
            processSyncHandle(handle = primaryHandle, CameraUploadFolderType.Primary)
            processSyncHandle(handle = secondaryHandle, CameraUploadFolderType.Secondary)
        } ?: run {
            // When there are no handles received from the API, invalidate both Primary and
            // Secondary Sync Handles
            setPrimaryNodeIdUseCase(NodeId(cameraUploadRepository.getInvalidHandle()))
            setSecondaryNodeIdUseCase(NodeId(cameraUploadRepository.getInvalidHandle()))
        }
    }

    /**
     * Process the Camera Uploads Sync Handle from the API
     *
     * @param handle The Camera Uploads Sync Handle
     * @param cameraUploadFolderType [CameraUploadFolderType]
     */
    private suspend fun processSyncHandle(
        handle: Long,
        cameraUploadFolderType: CameraUploadFolderType,
    ) {
        if (!isNodeInRubbishOrDeletedUseCase(handle)) {
            when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary -> setPrimaryNodeIdUseCase(NodeId(handle))
                CameraUploadFolderType.Secondary -> setSecondaryNodeIdUseCase(NodeId(handle))
            }
        }
    }
}
