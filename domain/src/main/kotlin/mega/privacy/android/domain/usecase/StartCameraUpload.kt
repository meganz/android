package mega.privacy.android.domain.usecase

/**
 * Use case for starting the camera upload service
 */
fun interface StartCameraUpload {

    /**
     * Invoke
     *
     * @param shouldIgnoreAttributes start camera upload w/o checking attributes
     */
    suspend operator fun invoke(shouldIgnoreAttributes: Boolean)
}
