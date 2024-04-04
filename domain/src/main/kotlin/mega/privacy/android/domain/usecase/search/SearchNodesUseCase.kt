package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
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
     * @param nodeSourceType search type [NodeSourceType]
     * @param searchCategory search category [SearchCategory]
     * @param isFirstLevel checks if user is on first level navigation
     * @param modificationDate modified date filter if set [DateFilterOption]
     * @param creationDate added date filter if set [DateFilterOption]
     *
     * @return list of search results or empty TypedNode
     */
    suspend operator fun invoke(
        query: String,
        parentHandle: Long,
        nodeSourceType: NodeSourceType,
        isFirstLevel: Boolean,
        searchCategory: SearchCategory = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<TypedNode> {
        val invalidNodeHandle = nodeRepository.getInvalidHandle()
        if (query.isEmpty() && parentHandle != invalidNodeHandle && searchCategory == SearchCategory.ALL && modificationDate == null && creationDate == null) return getTypedChildrenNodeUseCase(
            parentNodeId = NodeId(longValue = parentHandle),
            order = getCloudSortOrder()
        )

        return if (parentHandle == invalidNodeHandle) return when (nodeSourceType) {
            NodeSourceType.INCOMING_SHARES -> incomingSharesTabSearchUseCase(query = query)
            NodeSourceType.OUTGOING_SHARES -> outgoingSharesTabSearchUseCase(query = query)
            NodeSourceType.LINKS -> linkSharesTabSearchUseCase(
                query = query,
                isFirstLevel = isFirstLevel
            )

            else -> searchInNodes(
                nodeSourceType = nodeSourceType,
                parentHandle = parentHandle,
                invalidNodeHandle = invalidNodeHandle,
                query = query,
                searchCategory = searchCategory,
                modificationDate = modificationDate,
                creationDate = creationDate
            )
        } else searchInNodes(
            nodeSourceType = nodeSourceType,
            parentHandle = parentHandle,
            invalidNodeHandle = invalidNodeHandle,
            query = query,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate
        )
    }

    private suspend fun searchInNodes(
        nodeSourceType: NodeSourceType,
        parentHandle: Long,
        invalidNodeHandle: Long,
        query: String,
        searchCategory: SearchCategory,
        modificationDate: DateFilterOption?,
        creationDate: DateFilterOption?,
    ): List<TypedNode> {
        val node = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle)
        return searchInNodesUseCase(
            nodeId = node?.id,
            query = query,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate,
        )
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
        parentHandle: Long,
        invalidNodeHandle: Long,
    ): Node? = if (parentHandle == invalidNodeHandle) {
        when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> getRootNodeUseCase()
            NodeSourceType.RUBBISH_BIN -> getRubbishNodeUseCase()
            NodeSourceType.BACKUPS -> getBackupsNodeUseCase()
            else -> null
        }
    } else {
        getNodeByHandleUseCase(parentHandle)
    }
}