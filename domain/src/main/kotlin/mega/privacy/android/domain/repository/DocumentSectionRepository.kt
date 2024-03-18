package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * Repository related to audios
 */
interface DocumentSectionRepository {

    /**
     * Get all documents
     *
     * @param order the list order
     * @return List<UnTypedNode>
     */
    suspend fun getAllDocuments(order: SortOrder): List<UnTypedNode>
}