package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.SearchParameters

/**
 * Search Repository
 *
 * All the methods related to search will be included here
 */
interface SearchRepository {
    /**
     * Search node and return list of [UnTypedNode]
     * @param nodeId [NodeId] place to be searched
     * @param order [SortOrder] locally saved user selected sort order
     * @param parameters [SearchParameters] additional search parameters
     */
    suspend fun search(
        nodeId: NodeId?,
        order: SortOrder,
        parameters: SearchParameters,
    ): List<UnTypedNode>

    /**
     * Get children of a node and return list of [UnTypedNode]
     * @param nodeId [NodeId] place to be searched
     * @param order [SortOrder] locally saved user selected sort order
     * @param parameters [SearchParameters] additional search parameters
     */
    suspend fun getChildren(
        nodeId: NodeId?,
        order: SortOrder,
        parameters: SearchParameters,
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