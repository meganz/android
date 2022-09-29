package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus

/**
 * Use Case that returns [EnableCameraUploadsStatus] when the user enables Camera Uploads.
 */
fun interface CheckEnableCameraUploadsStatus {

    /**
     * Calls the Use Case and returns [EnableCameraUploadsStatus] when the user enables Camera Uploads
     *
     * @return [EnableCameraUploadsStatus] that denotes the behavior for Use Case callers
     */
    suspend operator fun invoke(): EnableCameraUploadsStatus
}