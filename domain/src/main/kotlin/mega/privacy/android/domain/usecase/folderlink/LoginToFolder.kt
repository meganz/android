package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus

/**
 * Use case for logging into folder
 */
fun interface LoginToFolder {
    /**
     * Invoke
     *
     * @param folderLink    Link of the folder to login
     */
    suspend operator fun invoke(folderLink: String): FolderLoginStatus
}