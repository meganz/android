package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType

/**
 * Check if local folder attribute changed and reset timelines
 *
 */
interface ResetCameraUploadTimelines {

    /**
     * Invoke
     *
     * @param handleInAttribute updated folder handle
     * @param cameraUploadFolderType  [CameraUploadFolderType]
     * @return is local folder attribute changed
     */
    suspend operator fun invoke(
        handleInAttribute: Long,
        cameraUploadFolderType: CameraUploadFolderType,
    ): Boolean
}
