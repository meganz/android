package mega.privacy.android.domain.entity.node

/**
 * Node name collision result
 *
 * @property copySuccess Number of nodes that were successfully copied
 * @property copyError Number of nodes that failed to copy
 * @property conflictNodes List of [NodeNameCollision]
 */
data class ImportNodesResult(
    val copySuccess: Int,
    val copyError: Int,
    val conflictNodes: List<NodeNameCollision>,
)