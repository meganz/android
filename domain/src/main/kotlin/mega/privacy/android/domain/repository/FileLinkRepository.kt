package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * FileLink repository
 */
interface FileLinkRepository {

    /**
     * Get the public node from give url
     *
     * @param url Url of the public node
     */
    suspend fun getPublicNode(url: String): UnTypedNode

    /**
     * Encrypt link with password
     *
     * @param link
     * @param password
     */
    suspend fun encryptLinkWithPassword(link: String, password: String): String

    /**
     * Get file url by public link
     *
     * @param link public file link
     * @return local link
     */
    suspend fun getFileUrlByPublicLink(link: String): String?
}