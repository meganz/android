package mega.privacy.android.domain.usecase


/**
 * * Use Case for saving Time Stamps and Folder Handle
 */
fun interface BackupTimeStampsAndFolderHandle {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}