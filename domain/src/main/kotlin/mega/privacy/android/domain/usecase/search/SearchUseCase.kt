package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
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
     * @param parentHandle search parent
     * @param nodeSourceType search type [NodeSourceType]
     * @param searchParameters search parameters [SearchParameters]
     *
     * @return list of search results or empty TypedNode
     */
    suspend operator fun invoke(
        parentHandle: NodeId,
        nodeSourceType: NodeSourceType,
        searchParameters: SearchParameters,
    ): List<TypedNode> {
        val (query, searchTarget, searchCategory, modificationDate, creationDate) = searchParameters
        val invalidNodeHandle = searchRepository.getInvalidHandle()
        val searchList = when {
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.INCOMING_SHARE -> searchRepository.getInShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.OUTGOING_SHARE -> searchRepository.getOutShares()
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.LINKS_SHARE -> searchRepository.getPublicLinks()
            query.isEmpty() && searchCategory == SearchCategory.ALL && modificationDate == null && creationDate == null -> searchRepository.getChildren(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                order = getCloudSortOrder(),
                parameters = searchParameters,
            )

            else -> searchRepository.search(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                order = getCloudSortOrder(),
                parameters = searchParameters,
            )
        }
        return addNodesTypeUseCase(searchList)
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