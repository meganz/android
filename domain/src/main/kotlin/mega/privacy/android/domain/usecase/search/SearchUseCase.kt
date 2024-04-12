package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.AddNodesTypeUseCase
import javax.inject.Inject

/**
 * Search Node Use Case
 *
 * Handles every use-case related to search
 */
class SearchUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val searchRepository: SearchRepository,
    private val addNodesTypeUseCase: AddNodesTypeUseCase,
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
    ): List<TypedNode> {
        val invalidNodeHandle = searchRepository.getInvalidHandle()
        val searchTarget = getSearchTarget(nodeSourceType)

        val searchList = when {
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.INCOMING_SHARE -> searchRepository.getInShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.OUTGOING_SHARE -> searchRepository.getOutShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.LINKS_SHARE -> searchRepository.getPublicLinks()
            query.isEmpty() && searchCategory == SearchCategory.ALL && modificationDate == null && creationDate == null -> searchRepository.getChildren(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                searchCategory = searchCategory,
                query = query,
                searchTarget = searchTarget,
                order = getCloudSortOrder(),
            )

            else -> searchRepository.search(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                searchCategory = searchCategory,
                query = query,
                searchTarget = searchTarget,
                order = getCloudSortOrder(),
                modificationDate = modificationDate,
                creationDate = creationDate,
            )
        }
        return addNodesTypeUseCase(searchList)
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