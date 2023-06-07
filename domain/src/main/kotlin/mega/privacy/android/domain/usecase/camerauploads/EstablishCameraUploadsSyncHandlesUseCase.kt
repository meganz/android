package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Use Case that performs the following functions:
 *
 * 1. Retrieve the Camera Uploads Sync Handles from the API
 * 2. Set the Sync Handles to the local Database
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getCameraUploadsSyncHandlesUseCase [GetCameraUploadsSyncHandlesUseCase]
 * @property isNodeInRubbishOrDeletedUseCase [IsNodeInRubbishOrDeletedUseCase]
 * @property resetCameraUploadTimelines [ResetCameraUploadTimelines]
 * @property setPrimarySyncHandle [SetPrimarySyncHandle]
 * @property setSecondarySyncHandle [SetSecondarySyncHandle]
 */
class EstablishCameraUploadsSyncHandlesUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val resetCameraUploadTimelines: ResetCameraUploadTimelines,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        getCameraUploadsSyncHandlesUseCase()?.let {
            processSyncHandle(handle = it.first, CameraUploadFolderType.Primary)
            processSyncHandle(handle = it.second, CameraUploadFolderType.Secondary)
        } ?: run {
            // When there are no handles received from the API, invalidate both Primary and
            // Secondary Sync Handles
            setPrimarySyncHandle(cameraUploadRepository.getInvalidHandle())
            setSecondarySyncHandle(cameraUploadRepository.getInvalidHandle())
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
            resetCameraUploadTimelines(
                handleInAttribute = handle,
                cameraUploadFolderType = cameraUploadFolderType,
            )
            when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary -> setPrimarySyncHandle(handle)
                CameraUploadFolderType.Secondary -> setSecondarySyncHandle(handle)
            }
        }
    }
}
