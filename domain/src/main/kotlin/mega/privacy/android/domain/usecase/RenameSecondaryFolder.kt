package mega.privacy.android.domain.usecase

/**
 * Use case for renaming the secondary camera upload folder
 */
fun interface RenameSecondaryFolder {

    /**
     * Invoke
     *
     * @param secondaryHandle handle of node to rename
     */
    suspend operator fun invoke(secondaryHandle: Long)
}
