package mega.privacy.android.domain.usecase

/**
 * Set secondary folder path
 *
 */
fun interface SetSecondaryFolderPath {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(folderPath: String)
}
