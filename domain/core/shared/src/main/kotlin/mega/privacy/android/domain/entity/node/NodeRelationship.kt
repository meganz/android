package mega.privacy.android.domain.entity.node

/**
 * Result type for node relationship comparison
 */
sealed interface NodeRelationship {
    /** Exact match - both nodes are the same */
    object ExactMatch : NodeRelationship

    /** Target node is an ancestor of source node (parent or higher) */
    object TargetIsAncestor : NodeRelationship

    /** Target node is a descendant of source node (child or deeper) */
    object TargetIsDescendant : NodeRelationship

    /** No relationship found between nodes */
    object NoMatch : NodeRelationship
}
