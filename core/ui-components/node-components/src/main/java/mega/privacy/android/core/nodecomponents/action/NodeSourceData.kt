package mega.privacy.android.core.nodecomponents.action

import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * Represents the source type of a node with some data based on context
 */
sealed class NodeSourceData(
    open val nodeSourceType: NodeSourceType,
) {
    /**
     * Recents bucket node source type with required data
     */
    data class RecentsBucket(
        val nodeIds: List<Long>,
        val isInShare: Boolean,
    ) : NodeSourceData(
        nodeSourceType = NodeSourceType.RECENTS_BUCKET
    )

    /**
     * Folder link node source type
     */
    data object FolderLink : NodeSourceData(
        nodeSourceType = NodeSourceType.FOLDER_LINK
    )

    /**
     * File link node source type with required data
     */
    data class FileLink(
        val url: String,
    ) : NodeSourceData(
        nodeSourceType = NodeSourceType.FILE_LINK
    )

    /**
     * Default node source type that doesn't require any additional data
     */
    data class Default(
        override val nodeSourceType: NodeSourceType,
    ) : NodeSourceData(
        nodeSourceType = nodeSourceType
    )

    /**
     * Determines if the node source is a public link
     */
    fun isPublicLinkSource() = this is FolderLink || this is FileLink
}
