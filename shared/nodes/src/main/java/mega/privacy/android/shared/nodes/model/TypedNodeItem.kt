package mega.privacy.android.shared.nodes.model

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData

/**
 * Node item
 *
 * @param T type of node
 * @property node the typed node
 * @property isHighlighted whether the node is highlighted
 * @property title the title of the node
 * @property subtitle the subtitle text of the node
 * @property formattedDescription the formatted description of the node, or null if none
 * @property iconRes the resource ID of the node icon
 * @property thumbnailData the thumbnail data of the node, or null if none
 * @property accessPermissionIcon the resource ID of the access permission icon, or null if none
 * @property showIsVerified whether to show the verified indicator
 * @property showLink whether to show the link indicator
 * @property showFavourite whether to show the favourite indicator
 * @property isSensitive whether the node is sensitive
 * @property showBlurEffect whether to show the blur effect
 * @property isFolderNode whether the node is a folder
 * @property isVideoNode whether the node is a video
 * @property duration the duration of the node, or null if not applicable
 */
interface TypedNodeItem<T : TypedNode> : Node {
    val node: T
    val isHighlighted: Boolean
    val title: LocalizedText
    val subtitle: NodeSubtitleText
    val formattedDescription: LocalizedText?
    val iconRes: Int
    val thumbnailData: ThumbnailData?
    val accessPermissionIcon: Int?
    val showIsVerified: Boolean
    val showLink: Boolean
    val showFavourite: Boolean
    val isSensitive: Boolean
    val showBlurEffect: Boolean
    val isFolderNode: Boolean
    val isVideoNode: Boolean
    val duration: String?
}

/**
 * Selectable
 */
interface Selectable {
    /**
     * Is selected
     */
    val isSelected: Boolean
}