package mega.privacy.android.core.nodecomponents.list.model

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 * @param fileDuration Duration of file
 * @param isHighlighted Node is highlighted because it comes from "Locate" action in notification
 * @property uniqueKey Unique key of the node, to be used in Compose list
 * @constructor Create empty Node UI Item
 */
data class NodeUiItem<T : TypedNode>(
    val node: T,
    var isSelected: Boolean,
    val isInvisible: Boolean = false,
    val fileDuration: String? = null,
    val isHighlighted: Boolean = false,
) : Node by node {
    val uniqueKey =
        "${node.id.longValue}".plus(
            (node as? ShareFolderNode)
                ?.shareData
                ?.let {
                    "_${it.count}_${it.user}_${it.isPending}_${it.isUnverifiedDistinctNode}_${it.isVerified}"
                }
        )
}