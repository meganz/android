package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget

/**
 * Search Repository
 *
 * All the methods related to search will be included here
 */
interface SearchRepository {

    /**
     * Get all search filters
     * returns list of search categories available for filtering search
     * Currently the values are hardcoded from repository in future we will be taking those from API
     */
    fun getSearchCategories(): List<SearchCategory>

    /**
     * Search node and return list of [UnTypedNode]
     * @param nodeId [NodeId] place where needed to be searched
     * @param searchCategory Search Category for search
     * @param query string to be search
     * @param order oder in which result should be there
     * @param modificationDate modified date filter if set [DateFilterOption]
     * @param creationDate added date filter if set [DateFilterOption]
     */
    suspend fun search(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
        searchCategory: SearchCategory = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<UnTypedNode>

    /**
     * Get children of a node and return list of [UnTypedNode]
     * @param nodeId [NodeId] place where needed to be searched
     * @param query string to be search
     * @param searchCategory Search Category for search
     * @param order oder in which result should be there
     * @param modificationDate modified date filter if set [DateFilterOption]
     * @param creationDate added date filter if set [DateFilterOption]
     */
    suspend fun getChildren(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
        searchCategory: SearchCategory = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<UnTypedNode>

    /**
     * get incoming shares node list
     */
    suspend fun getInShares(): List<UnTypedNode>

    /**
     * get outgoing shares node list
     */
    suspend fun getOutShares(): List<UnTypedNode>

    /**
     * get links node list
     */
    suspend fun getPublicLinks(): List<UnTypedNode>

    /**
     * get root node id
     */
    suspend fun getRubbishNodeId(): NodeId?

    /**
     * get backup node id
     */
    suspend fun getBackUpNodeId(): NodeId?

    /**
     * get root node id
     */
    suspend fun getRootNodeId(): NodeId?

    /**
     * get invalid handle
     */
    suspend fun getInvalidHandle(): NodeId
}