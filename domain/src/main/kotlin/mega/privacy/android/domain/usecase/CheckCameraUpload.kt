package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.CameraUploadState

/**
 * Check Camera Upload
 */
fun interface CheckCameraUpload {
    /**
     * Invoke
     */
    suspend operator fun invoke(
        shouldDisable: Boolean,
        primaryHandle: Long,
        secondaryHandle: Long,
    ): CameraUploadState
}