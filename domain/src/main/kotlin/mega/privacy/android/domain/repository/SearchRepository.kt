package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory

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
        searchCategory: SearchCategory = SearchCategory.ALL,
        query: String,
        order: SortOrder,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<UnTypedNode>

    /**
     * Search Nodes in incoming shares
     * @param query string to be search
     * @param order oder in which result should be there
     */
    suspend fun searchInShares(
        query: String,
        order: SortOrder
    ): List<UnTypedNode>

    /**
     * Search Nodes in incoming shares
     * @param query string to be search
     * @param order oder in which result should be there
     */
    suspend fun searchOutShares(
        query: String,
        order: SortOrder
    ): List<UnTypedNode>

    /**
     * Search nodes in links
     * @param query string to be search
     * @param order oder in which result should be there
     * @param isFirstLevelNavigation first level navigation
     */
    suspend fun searchLinkShares(
        query: String,
        order: SortOrder,
        isFirstLevelNavigation: Boolean,
    ): List<UnTypedNode>
}