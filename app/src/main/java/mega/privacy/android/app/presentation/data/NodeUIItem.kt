package mega.privacy.android.app.presentation.data

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 * @param isAvailableOffline Node is available offline or not
 */
data class NodeUIItem<T : TypedNode>(
    val node: T,
    var isSelected: Boolean,
    val isInvisible: Boolean,
    val isAvailableOffline: Boolean = false
) : Node by node