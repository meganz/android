package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import javax.inject.Inject

/**
 * Default implementation of [EstablishCameraUploadsSyncHandles]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getCameraUploadsSyncHandles [GetCameraUploadsSyncHandles]
 * @property isNodeInRubbishOrDeleted [IsNodeInRubbishOrDeleted]
 * @property resetCameraUploadTimelines [ResetCameraUploadTimelines]
 * @property setPrimarySyncHandle [SetPrimarySyncHandle]
 * @property setSecondarySyncHandle [SetSecondarySyncHandle]
 */
class DefaultEstablishCameraUploadsSyncHandles @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadsSyncHandles: GetCameraUploadsSyncHandles,
    private val isNodeInRubbishOrDeleted: IsNodeInRubbishOrDeleted,
    private val resetCameraUploadTimelines: ResetCameraUploadTimelines,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
) : EstablishCameraUploadsSyncHandles {
    override suspend fun invoke() {
        getCameraUploadsSyncHandles()?.let {
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