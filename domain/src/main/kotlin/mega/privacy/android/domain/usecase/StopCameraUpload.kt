package mega.privacy.android.domain.usecase

/**
 * Use case for stopping the camera upload service
 */
fun interface StopCameraUpload {

    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}
