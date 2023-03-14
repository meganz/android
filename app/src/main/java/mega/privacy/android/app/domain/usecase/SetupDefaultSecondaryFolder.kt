package mega.privacy.android.app.domain.usecase

/**
 * Setup Default Secondary Folder for Camera Upload
 *
 */
interface SetupDefaultSecondaryFolder {
    /**
     * Invoke
     *
     * @param secondaryFolderName
     * @return
     */
    suspend operator fun invoke(secondaryFolderName: String)
}
