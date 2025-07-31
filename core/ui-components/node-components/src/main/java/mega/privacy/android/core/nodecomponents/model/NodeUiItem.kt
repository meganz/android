package mega.privacy.android.core.nodecomponents.model

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode

/**
 * This class is used to display list items on screen
 * @property node [Node]
 * @param isSelected Node is selected
 * @param isInvisible Node is invisible
 * @param isHighlighted Node is highlighted because it comes from "Locate" action in notification
 * @param title Localized title for the node
 * @param subtitle Localized subtitle for the node
 * @param description Optional localized description for the node
 * @param tags Optional list of tags for the node
 * @param iconRes Drawable resource ID for the node icon
 * @param thumbnailData Data for the thumbnail, can be a URL or any other data type supported by ThumbnailView
 * @param accessPermissionIcon Optional icon resource ID for access permission
 * @param showIsVerified if true, shows a verified icon
 * @param isTakenDown if true, shows a taken down icon
 * @param labelColor Optional color for a label circle next to the title
 * @param showLink if true, shows a link icon
 * @param showFavourite if true, shows a favourite icon
 * @param isSensitive if true, the item is considered sensitive and will be displayed with reduced opacity
 * @param showBlurEffect if true, applies a blur effect to the thumbnail when the item is sensitive
 * @param isFolderNode Whether this node represents a folder (affects layout and styling)
 * @param isVideoNode Whether this node represents a video file (shows play icon overlay)
 * @param duration Optional duration string for video files (displayed as a badge)
 * @constructor Create empty Node UI Item
 */
data class NodeUiItem<T : TypedNode>(
    val node: T,
    var isSelected: Boolean,
    val isInvisible: Boolean = false,
    val isHighlighted: Boolean = false,
    val title: LocalizedText = LocalizedText.Literal(""),
    val subtitle: LocalizedText = LocalizedText.Literal(""),
    val formattedDescription: LocalizedText? = null,
    val iconRes: Int = 0,
    val thumbnailData: Any? = null,
    val accessPermissionIcon: Int? = null,
    val showIsVerified: Boolean = false,
    val labelColor: Int? = null,
    val showLink: Boolean = false,
    val showFavourite: Boolean = false,
    val isSensitive: Boolean = false,
    val showBlurEffect: Boolean = false,
    val isFolderNode: Boolean = false,
    val isVideoNode: Boolean = false,
    val duration: String? = null,
    override val tags: List<String>? = node.tags,
) : Node by node