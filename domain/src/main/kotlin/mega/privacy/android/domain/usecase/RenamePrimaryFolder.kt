package mega.privacy.android.domain.usecase

/**
 * Use case for renaming the primary camera upload folder
 */
fun interface RenamePrimaryFolder {

    /**
     * Invoke
     *
     * @param primaryHandle handle of node to rename
     */
    suspend operator fun invoke(primaryHandle: Long)
}
