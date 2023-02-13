package mega.privacy.android.domain.entity.node


/**
 * Node update
 *
 * @property changes
 */
data class NodeUpdate(val changes: Map<Node, List<NodeChanges>>)
