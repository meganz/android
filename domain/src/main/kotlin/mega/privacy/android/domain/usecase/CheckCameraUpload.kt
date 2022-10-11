package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.CameraUploadState

/**
 * Check Camera Upload
 */
fun interface CheckCameraUpload {
    /**
     * Invoke
     * @param shouldDisable whether to disable camera upload job or not
     * @param primaryHandle Primary Folder Handle
     * @param secondaryHandle Secondary Folder Handle
     * @return [CameraUploadState]
     */
    suspend operator fun invoke(
        shouldDisable: Boolean,
        primaryHandle: Long,
        secondaryHandle: Long,
    ): CameraUploadState
}