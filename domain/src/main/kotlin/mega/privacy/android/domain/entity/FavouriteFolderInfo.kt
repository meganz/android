package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.Node

/**
 * The entity for favourite folder info
 * @param children favourite list
 * @param name current folder name
 * @param currentHandle current folder node handle
 * @param parentHandle parent node handle of current folder node
 */
data class FavouriteFolderInfo(
    val children: List<Node>,
    val name: String,
    val currentHandle: Long,
    val parentHandle: Long,
)