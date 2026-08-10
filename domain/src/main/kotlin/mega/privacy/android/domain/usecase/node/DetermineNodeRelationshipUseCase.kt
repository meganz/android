package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.HasAncestor
import javax.inject.Inject


/**
 * Use case to determine the hierarchical relationship between two nodes.
 *
 * This use case determines if two nodes have an ancestor/descendant relationship
 * or are the same node, using path comparison with fallback to parent chain traversal.
 *
 * Priority: Path comparison (faster for deep hierarchies) → Parent chain traversal (fallback)
 */
class DetermineNodeRelationshipUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val hasAncestor: HasAncestor,
) {
    /**
     * Determines the relationship between two nodes.
     *
     * @param sourceNodeId The source node ID to check
     * @param targetNodeId The target node ID to compare against
     * @return [NodeRelationship] indicating the relationship between the nodes
     */
    suspend operator fun invoke(sourceNodeId: NodeId, targetNodeId: NodeId): NodeRelationship {
        // Check for exact match first (fast path)
        if (sourceNodeId == targetNodeId) {
            return NodeRelationship.ExactMatch
        }

        // Try path-based comparison first (more efficient for deep hierarchies)
        val sourcePath = nodeRepository.getFullNodePathById(sourceNodeId)
        val targetPath = nodeRepository.getFullNodePathById(targetNodeId)

        return if (sourcePath != null && targetPath != null) {
            determineByPathComparison(sourcePath, targetPath)
        } else {
            // Fallback to parent chain traversal if path lookup fails
            determineByParentChain(sourceNodeId, targetNodeId)
        }
    }

    /**
     * Determines relationship by comparing node paths.
     * Uses string-based path comparison which is efficient for deep hierarchies.
     */
    private fun determineByPathComparison(
        sourcePath: String,
        targetPath: String,
    ): NodeRelationship {
        val normalizedSource = sourcePath.trimEnd('/')
        val normalizedTarget = targetPath.trimEnd('/')

        return when {
            normalizedSource == normalizedTarget -> NodeRelationship.ExactMatch
            UriPath(normalizedSource).isSubPathOf(
                UriPath(normalizedTarget)
            ) -> NodeRelationship.TargetIsAncestor

            UriPath(normalizedTarget).isSubPathOf(
                UriPath(normalizedSource)
            ) -> NodeRelationship.TargetIsDescendant

            else -> NodeRelationship.NoMatch
        }
    }

    /**
     * Determines relationship by traversing parent chain.
     * Used as fallback when path-based comparison fails (e.g., nodes in rubbish bin).
     */
    private suspend fun determineByParentChain(
        sourceNodeId: NodeId,
        targetNodeId: NodeId,
    ): NodeRelationship {
        return runCatching {
            when {
                hasAncestor(sourceNodeId, targetNodeId) -> NodeRelationship.TargetIsAncestor
                hasAncestor(targetNodeId, sourceNodeId) -> NodeRelationship.TargetIsDescendant
                else -> NodeRelationship.NoMatch
            }
        }.getOrElse {
            NodeRelationship.NoMatch
        }
    }
}
