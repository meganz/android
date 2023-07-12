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
}