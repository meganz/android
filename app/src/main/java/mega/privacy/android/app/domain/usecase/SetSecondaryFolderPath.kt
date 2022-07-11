package mega.privacy.android.app.domain.usecase

/**
 * Set secondary folder path
 *
 */
interface SetSecondaryFolderPath {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(folderPath: String)
}
