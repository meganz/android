package mega.privacy.android.domain.usecase

/**
 * Use case for starting the camera upload service
 */
fun interface StartCameraUpload {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}
