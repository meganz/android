package mega.privacy.android.domain.usecase

/**
 * Setup Secondary Folder for Camera Upload
 *
 */
interface SetupSecondaryFolder {
    /**
     * Invoke
     *
     * @param secondaryHandle
     * @return
     */
    suspend operator fun invoke(secondaryHandle: Long)
}
