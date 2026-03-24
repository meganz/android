package mega.privacy.android.shared.nodes.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData

/**
 * This class is used to display node items on screen
 * @property node [mega.privacy.android.domain.entity.node.Node]
 * @param isHighlighted Node is highlighted because it comes from "Locate" action in notification
 * @param title Localized title for the node
 * @param subtitle NodeSubtitleText for the node that can be resolved to localized string in Composable
 * @param mega.privacy.android.domain.entity.node.Node.description Optional localized description for the node
 * @param tags Optional list of tags for the node
 * @param iconRes Drawable resource ID for the node icon
 * @param thumbnailData Data for the thumbnail, can be a URL or any other data type supported by ThumbnailView
 * @param accessPermissionIcon Optional icon resource ID for access permission
 * @param showIsVerified if true, shows a verified icon
 * @param mega.privacy.android.domain.entity.node.Node.isTakenDown if true, shows a taken down icon
 * @param showLink if true, shows a link icon
 * @param showFavourite if true, shows a favourite icon
 * @param isSensitive if true, the item is considered sensitive and will be displayed with reduced opacity
 * @param showBlurEffect if true, applies a blur effect to the thumbnail when the item is sensitive
 * @param isFolderNode Whether this node represents a folder (affects layout and styling)
 * @param isVideoNode Whether this node represents a video file (shows play icon overlay)
 * @param duration Optional duration string for video files (displayed as a badge)
 */
@Immutable
data class NodeViewItem<T : TypedNode>(
    override val node: T,
    override val isHighlighted: Boolean = false,
    override val title: LocalizedText = LocalizedText.Literal(""),
    override val subtitle: NodeSubtitleText = NodeSubtitleText.Empty,
    override val formattedDescription: LocalizedText? = null,
    override val iconRes: Int = 0,
    override val thumbnailData: ThumbnailData? = null,
    override val accessPermissionIcon: Int? = null,
    override val showIsVerified: Boolean = false,
    override val showLink: Boolean = false,
    override val showFavourite: Boolean = false,
    override val isSensitive: Boolean = false,
    override val showBlurEffect: Boolean = false,
    override val isFolderNode: Boolean = false,
    override val isVideoNode: Boolean = false,
    override val duration: String? = null,
    override val tags: List<String>? = node.tags,
) : Node by node, TypedNodeItem<T>