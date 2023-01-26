package mega.privacy.android.domain.usecase

/**
 * Use case to restart Camera Uploads
 */
fun interface RestartCameraUpload {

    /**
     * Invocation method
     *
     * @param shouldIgnoreAttributes Whether to start Camera Uploads without checking User Attributes
     */
    suspend operator fun invoke(shouldIgnoreAttributes: Boolean)
}