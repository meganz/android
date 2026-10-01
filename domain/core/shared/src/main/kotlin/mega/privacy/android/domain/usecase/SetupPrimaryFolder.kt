package mega.privacy.android.domain.usecase

/**
 * Setup Primary Folder for Camera Upload
 *
 */
interface SetupPrimaryFolder {
    /**
     * Invoke
     *
     * @param primaryHandle
     * @return
     */
    suspend operator fun invoke(primaryHandle: Long)
}
