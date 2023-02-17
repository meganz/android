package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus

/**
 * FolderLink repository
 */
interface FolderLinkRepository {
    /**
     * Fetch the filesystem in MEGA
     */
    suspend fun fetchNodes()

    /**
     * Log in to a public folder using a folder link
     *
     * @param folderLink Public link to a folder in MEGA
     */
    suspend fun loginToFolder(folderLink: String): FolderLoginStatus
}