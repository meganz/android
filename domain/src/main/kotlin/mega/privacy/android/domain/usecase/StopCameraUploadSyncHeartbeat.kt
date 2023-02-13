package mega.privacy.android.domain.usecase

/**
 * Use case for stopping workers scheduled to run the camera upload periodically.
 */
fun interface StopCameraUploadSyncHeartbeat {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}