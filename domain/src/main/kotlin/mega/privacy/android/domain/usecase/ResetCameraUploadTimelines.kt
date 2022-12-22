package mega.privacy.android.domain.usecase

/**
 * Check if local folder attribute changed and reset timelines
 *
 */
interface ResetCameraUploadTimelines {

    /**
     * Invoke
     *
     * @param handleInAttribute updated folder handle
     * @param isSecondary      whether primary or secondary folder
     * @return is local folder attribute changed
     */
    suspend operator fun invoke(handleInAttribute: Long, isSecondary: Boolean): Boolean
}
