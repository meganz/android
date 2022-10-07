package mega.privacy.android.app.domain.usecase

/**
 * Get the handle by the default path string
 */
interface GetDefaultNodeHandle {
    /**
     * Invoke
     *
     * @return long handle
     */
    suspend operator fun invoke(
        defaultFolderName: String,
    ): Long
}
