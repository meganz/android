package mega.privacy.android.app.presentation.data

import mega.privacy.android.domain.entity.node.Node

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 */
data class NodeUIItem(
    val node: Node,
    var isSelected: Boolean,
    val isInvisible: Boolean
) : Node by node