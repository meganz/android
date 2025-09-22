package mega.privacy.android.app.presentation.data

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 * @param fileDuration Duration of file
 * @param isHighlighted Node is highlighted because it comes from "Locate" action in notification
 * @property uniqueKey Unique key of the node, to be used in Compose list
 * @property accessPermission Access permission of the node.
 * @property canBeMovedToRubbishBin Whether the node can be moved to the rubbish bin.
 * @constructor Create empty Node UI Item
 */
data class NodeUIItem<T : TypedNode>(
    val node: T,
    var isSelected: Boolean,
    val isInvisible: Boolean = false,
    val fileDuration: String? = null,
    val isHighlighted: Boolean = false,
    val accessPermission: AccessPermission = AccessPermission.UNKNOWN,
    val canBeMovedToRubbishBin: Boolean = false,
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