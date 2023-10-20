package mega.privacy.android.app.presentation.data

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 * @param fileDuration Duration of file
 */
data class NodeUIItem<T : TypedNode>(
    val node: T,
    var isSelected: Boolean,
    val isInvisible: Boolean = false,
    val fileDuration: String? = null
) : Node by node