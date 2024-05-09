package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case to search nodes from searched Query
 *
 * @property getCloudSortOrder Sort order of cloud
 * @property megaNodeRepository [MegaNodeRepository]
 * @property searchRepository [SearchRepository]
 */
@Deprecated("Use SearchUseCase instead")
class LegacySearchUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val megaNodeRepository: MegaNodeRepository,
    private val searchRepository: SearchRepository,
) {

    /**
     * Invocation
     *
     * @param query search query
     * @param parentHandle search parent
     * @param nodeSourceType search type [NodeSourceType]
     * @param searchCategory search category [SearchCategory]
     * @param modificationDate modified date filter if set [DateFilterOption]
     * @param creationDate added date filter if set [DateFilterOption]
     *
     * @return list of search results or empty TypedNode
     */
    suspend operator fun invoke(
        query: String,
        parentHandle: NodeId,
        nodeSourceType: NodeSourceType,
        searchCategory: SearchCategory = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<MegaNode> {
        val invalidNodeHandle = searchRepository.getInvalidHandle()
        val searchTarget = getSearchTarget(nodeSourceType)

        return when {
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.INCOMING_SHARE -> megaNodeRepository.getInShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.OUTGOING_SHARE -> megaNodeRepository.getOutShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.LINKS_SHARE -> megaNodeRepository.getPublicLinks()
            query.isEmpty() && searchCategory == SearchCategory.ALL && modificationDate == null && creationDate == null -> megaNodeRepository.getChildren(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                searchCategory = searchCategory,
                query = query,
                searchTarget = searchTarget,
                order = getCloudSortOrder(),
            )

            else -> megaNodeRepository.search(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                searchCategory = searchCategory,
                query = query,
                searchTarget = searchTarget,
                order = getCloudSortOrder(),
                modificationDate = modificationDate,
                creationDate = creationDate,
            )
        }
    }

    private fun getSearchTarget(nodeSourceType: NodeSourceType) = when (nodeSourceType) {
        NodeSourceType.INCOMING_SHARES -> SearchTarget.INCOMING_SHARE
        NodeSourceType.OUTGOING_SHARES -> SearchTarget.OUTGOING_SHARE
        NodeSourceType.LINKS -> SearchTarget.LINKS_SHARE
        else -> SearchTarget.ROOT_NODES
    }

    /**
     * This method Returns [Node] for respective selected [NodeSourceType]
     *
     * @param nodeSourceType
     * @param parentHandle
     * @return [Node]
     */
    private suspend fun getSearchParentNode(
        nodeSourceType: NodeSourceType,
        parentHandle: NodeId,
        invalidNodeHandle: NodeId,
    ): NodeId? = if (parentHandle.longValue == invalidNodeHandle.longValue) {
        when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> searchRepository.getRootNodeId()
            NodeSourceType.RUBBISH_BIN -> searchRepository.getRubbishNodeId()
            NodeSourceType.BACKUPS -> searchRepository.getBackUpNodeId()
            else -> null
        }
    } else {
        parentHandle
    }
}