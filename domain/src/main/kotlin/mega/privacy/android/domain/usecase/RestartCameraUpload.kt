package mega.privacy.android.domain.usecase

/**
 * Use case to restart Camera Uploads
 */
fun interface RestartCameraUpload {

    /**
     * Invocation method
     */
    suspend operator fun invoke()
}
