package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import javax.inject.Inject

/**
 * Search Node Use Case
 *
 * Handles every use-case related to search
 */
class SearchNodesUseCase @Inject constructor(
    private val incomingSharesTabSearchUseCase: IncomingSharesTabSearchUseCase,
    private val outgoingSharesTabSearchUseCase: OutgoingSharesTabSearchUseCase,
    private val linkSharesTabSearchUseCase: LinkSharesTabSearchUseCase,
    private val searchInNodesUseCase: SearchInNodesUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getTypedChildrenNodeUseCase: GetTypedChildrenNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation
     *
     * @param query search query
     * @param parentHandle search parent
     * @param searchType search type [SearchType]
     * @param searchCategory search category [SearchCategory]
     * @param isFirstLevel checks if user is on first level navigation
     *
     * @return list of search results or empty TypedNode
     */
    suspend operator fun invoke(
        query: String,
        parentHandle: Long,
        searchType: SearchType,
        isFirstLevel: Boolean,
        searchCategory: SearchCategory = SearchCategory.ALL,
    ): List<TypedNode> {
        val invalidNodeHandle = nodeRepository.getInvalidHandle()
        if (query.isEmpty() && parentHandle != invalidNodeHandle && searchCategory == SearchCategory.ALL) return getTypedChildrenNodeUseCase(
            parentNodeId = NodeId(longValue = parentHandle),
            order = getCloudSortOrder()
        )
        return when (searchType) {
            SearchType.INCOMING_SHARES -> incomingSharesTabSearchUseCase(query = query)
            SearchType.OUTGOING_SHARES -> outgoingSharesTabSearchUseCase(query = query)
            SearchType.LINKS -> linkSharesTabSearchUseCase(
                query = query,
                isFirstLevel = isFirstLevel
            )

            else -> {
                val node = getSearchParentNode(searchType, parentHandle, invalidNodeHandle)
                searchInNodesUseCase(
                    nodeId = node?.id,
                    query = query,
                    searchCategory = searchCategory
                )
            }
        }
    }

    /**
     * This method Returns [Node] for respective selected [SearchType]
     *
     * @param searchType
     * @param parentHandle
     * @return [Node]
     */
    private suspend fun getSearchParentNode(
        searchType: SearchType,
        parentHandle: Long,
        invalidNodeHandle: Long,
    ): Node? = if (parentHandle == invalidNodeHandle) {
        when (searchType) {
            SearchType.CLOUD_DRIVE -> getRootNodeUseCase()
            SearchType.RUBBISH_BIN -> getRubbishNodeUseCase()
            SearchType.BACKUPS -> getBackupsNodeUseCase()
            else -> null
        }
    } else {
        getNodeByHandleUseCase(parentHandle)
    }
}