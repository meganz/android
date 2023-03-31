package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import javax.inject.Inject

/**
 * Use Case that performs the following functions:
 *
 * 1. Retrieve the Camera Uploads Sync Handles from the API
 * 2. Set the Sync Handles to the local Database
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getCameraUploadsSyncHandlesUseCase [GetCameraUploadsSyncHandlesUseCase]
 * @property isNodeInRubbishOrDeleted [IsNodeInRubbishOrDeleted]
 * @property resetCameraUploadTimelines [ResetCameraUploadTimelines]
 * @property setPrimarySyncHandle [SetPrimarySyncHandle]
 * @property setSecondarySyncHandle [SetSecondarySyncHandle]
 */
class EstablishCameraUploadsSyncHandlesUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
    private val isNodeInRubbishOrDeleted: IsNodeInRubbishOrDeleted,
    private val resetCameraUploadTimelines: ResetCameraUploadTimelines,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        getCameraUploadsSyncHandlesUseCase()?.let {
            processSyncHandle(handle = it.first, isSecondary = false)
            processSyncHandle(handle = it.second, isSecondary = true)
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
     * @param isSecondary true if the Sync Handle belongs to the Secondary Folder (Media Uploads),
     * and false if it belongs to the Primary Folder (Camera Uploads)
     */
    private suspend fun processSyncHandle(handle: Long, isSecondary: Boolean) {
        if (!isNodeInRubbishOrDeleted(handle)) {
            resetCameraUploadTimelines(
                handleInAttribute = handle,
                isSecondary = isSecondary,
            )
            if (!isSecondary) {
                setPrimarySyncHandle(handle)
            } else {
                setSecondarySyncHandle(handle)
            }
        }
    }
}