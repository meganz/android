package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.folderlink.FetchNodeRequestResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * FolderLink repository
 */
interface FolderLinkRepository {
    /**
     * Fetch the filesystem in MEGA
     */
    suspend fun fetchNodes(): FetchNodeRequestResult

    /**
     * Update last public handle in local storage
     */
    suspend fun updateLastPublicHandle(nodeHandle: Long)

    /**
     * Log in to a public folder using a folder link
     *
     * @param folderLink Public link to a folder in MEGA
     */
    suspend fun loginToFolder(folderLink: String): FolderLoginStatus

    /**
     * Get the root node
     */
    suspend fun getRootNode(): UnTypedNode?

    /**
     * Get children nodes by handle
     */
    suspend fun getNodeChildren(handle: Long, order: Int? = null): List<UnTypedNode>
}