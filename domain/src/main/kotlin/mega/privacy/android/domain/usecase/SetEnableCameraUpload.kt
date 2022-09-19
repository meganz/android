package mega.privacy.android.domain.usecase

/**
 * Is camera sync enabled
 *
 */
fun interface SetEnableCameraUpload {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean)
}