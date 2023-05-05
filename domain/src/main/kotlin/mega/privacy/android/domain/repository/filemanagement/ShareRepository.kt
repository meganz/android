package mega.privacy.android.domain.repository.filemanagement

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * Share repository
 *
 * @constructor Create empty Share repository
 */
interface ShareRepository {
    /**
     * Get public links
     *
     * @param sortOrder
     * @return public links
     */
    suspend fun getPublicLinks(sortOrder: SortOrder): List<UnTypedNode>
}