package mega.privacy.android.domain.usecase

/**
 * Use case for checking if the Backups Node has children
 */
fun interface HasBackupsChildren {

    /**
     * Invoke.
     *
     * @return true if the Backups Node has children, and false if otherwise
     */
    suspend operator fun invoke(): Boolean
}