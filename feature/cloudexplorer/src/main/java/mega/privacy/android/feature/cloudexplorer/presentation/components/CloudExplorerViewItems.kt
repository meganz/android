package mega.privacy.android.feature.cloudexplorer.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.shared.nodes.components.NodeGridViewItem
import mega.privacy.android.shared.nodes.components.NodeListViewItem
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.nodes.selection.NodeSelectionState

/**
 * Cloud explorer list row: forwards to [NodeListViewItem] with cloud explorer defaults
 */
@Composable
internal fun CloudExplorerListViewItem(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    tags: List<String>? = null,
    thumbnailData: ThumbnailData? = null,
    titleColor: TextColor = TextColor.Primary,
    titleMaxLines: Int = 1,
    titleTextStyle: TextStyle = AppTheme.typography.bodyLarge,
    subtitleColor: TextColor = TextColor.Secondary,
    highlightText: String = "",
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    showIsVerified: Boolean = false,
    isTakenDown: Boolean = false,
    label: NodeLabel? = null,
    showLink: Boolean = false,
    isSensitive: Boolean = false,
    enabled: Boolean = true,
    showBlurEffect: Boolean = false,
    isHighlighted: Boolean = false,
    enableClick: Boolean = enabled,
    onItemClicked: () -> Unit,
) {
    NodeListViewItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier,
        description = description,
        tags = tags,
        thumbnailData = thumbnailData,
        titleColor = titleColor,
        titleMaxLines = titleMaxLines,
        titleTextStyle = titleTextStyle,
        subtitleColor = subtitleColor,
        highlightText = highlightText,
        showOffline = false,
        showVersion = false,
        isSelected = isSelected,
        isInSelectionMode = isInSelectionMode,
        showIsVerified = showIsVerified,
        isTakenDown = isTakenDown,
        label = label,
        showLink = showLink,
        isSensitive = isSensitive,
        enabled = enabled,
        showBlurEffect = showBlurEffect,
        isHighlighted = isHighlighted,
        enableClick = enableClick,
        onItemClicked = onItemClicked,
    )
}

/**
 * Cloud explorer grid cell: forwards to [mega.privacy.android.shared.nodes.components.NodeGridViewItem] with cloud explorer defaults
 */
@Composable
internal fun CloudExplorerGridViewItem(
    name: String,
    @DrawableRes iconRes: Int,
    thumbnailData: ThumbnailData?,
    isTakenDown: Boolean,
    modifier: Modifier = Modifier,
    duration: String? = null,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    isFolderNode: Boolean = false,
    isVideoNode: Boolean = false,
    highlightText: String = "",
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    isSensitive: Boolean = false,
    showBlurEffect: Boolean = false,
    isHighlighted: Boolean = false,
    showLink: Boolean = false,
    label: NodeLabel? = null,
) {
    NodeGridViewItem(
        name = name,
        iconRes = iconRes,
        thumbnailData = thumbnailData,
        isTakenDown = isTakenDown,
        modifier = modifier,
        duration = duration,
        isSelected = isSelected,
        isInSelectionMode = isInSelectionMode,
        isFolderNode = isFolderNode,
        isVideoNode = isVideoNode,
        highlightText = highlightText,
        onClick = onClick,
        enabled = enabled,
        isSensitive = isSensitive,
        showBlurEffect = showBlurEffect,
        isHighlighted = isHighlighted,
        showLink = showLink,
        label = label,
    )
}

/**
 * List row for a browsed node, shared by the cloud-drive and favourites sources. Pre-added nodes
 * ([disabledNodeIds]) render checked-but-disabled, and in video-only mode non-video files are
 * disabled. [showLink] is opted into per source.
 */
@Composable
internal fun ExplorerNodeListItem(
    item: NodeViewItem<TypedNode>,
    isSelected: Boolean,
    isSelectionModeEnabled: Boolean,
    isHiddenNodesEnabled: Boolean,
    videosOnly: Boolean,
    disabledNodeIds: Set<NodeId>,
    onItemClicked: () -> Unit,
    showLink: Boolean = false,
) {
    val isAlreadyAdded = item.id in disabledNodeIds
    val isUnsupportedFile = videosOnly && !item.isFolderNode && !item.isVideoNode
    val isDisabled = isAlreadyAdded || isUnsupportedFile
    CloudExplorerListViewItem(
        title = item.title.text,
        subtitle = item.subtitle.text(),
        icon = item.iconRes,
        description = item.formattedDescription?.text,
        tags = item.tags,
        thumbnailData = item.thumbnailData,
        isSelected = isSelected || isAlreadyAdded,
        isInSelectionMode = isSelectionModeEnabled && (item.node is FileNode),
        showIsVerified = item.showIsVerified,
        isTakenDown = item.isTakenDown,
        label = item.nodeLabel,
        showLink = showLink,
        isSensitive = item.isSensitive && isHiddenNodesEnabled,
        showBlurEffect = item.showBlurEffect && isHiddenNodesEnabled,
        isHighlighted = item.isHighlighted,
        onItemClicked = onItemClicked,
        enabled = (item.isFolderNode || isSelectionModeEnabled) && !isDisabled,
    )
}

/**
 * Grid cell counterpart of [ExplorerNodeListItem].
 */
@Composable
internal fun ExplorerNodeGridItem(
    item: NodeViewItem<TypedNode>,
    isSelected: Boolean,
    isSelectionModeEnabled: Boolean,
    isHiddenNodesEnabled: Boolean,
    videosOnly: Boolean,
    disabledNodeIds: Set<NodeId>,
    onItemClicked: () -> Unit,
    showLink: Boolean = false,
) {
    val isAlreadyAdded = item.id in disabledNodeIds
    val isUnsupportedFile = videosOnly && !item.isFolderNode && !item.isVideoNode
    val isDisabled = isAlreadyAdded || isUnsupportedFile
    CloudExplorerGridViewItem(
        name = item.title.text,
        iconRes = item.iconRes,
        thumbnailData = item.thumbnailData,
        isTakenDown = item.isTakenDown,
        duration = item.duration,
        isSelected = isSelected || isAlreadyAdded,
        isInSelectionMode = isSelectionModeEnabled && (item.node is FileNode),
        isFolderNode = item.isFolderNode,
        isVideoNode = item.isVideoNode,
        onClick = onItemClicked,
        isSensitive = item.isSensitive && isHiddenNodesEnabled,
        showBlurEffect = item.showBlurEffect && isHiddenNodesEnabled,
        isHighlighted = item.isHighlighted,
        showLink = showLink,
        label = item.nodeLabel,
        enabled = (item.isFolderNode || isSelectionModeEnabled) && !isDisabled,
    )
}

/**
 * Tap handler shared by the cloud-drive and favourites lists: folders open (clearing any selection),
 * pre-added nodes and unsupported (non-video, in video-only mode) files ignore taps, and files
 * toggle selection when selection is enabled.
 */
internal fun explorerNodeClick(
    selectionState: NodeSelectionState,
    disabledNodeIds: Set<NodeId>,
    videosOnly: Boolean,
    isSelectionModeEnabled: Boolean,
    onFolderClick: (NodeId) -> Unit,
): (NodeViewItem<TypedNode>) -> Unit = { item ->
    when {
        item.id in disabledNodeIds -> Unit

        item.isFolderNode -> {
            onFolderClick(item.id)
            selectionState.deselectAll()
        }

        videosOnly && !item.isVideoNode -> Unit

        isSelectionModeEnabled -> selectionState.toggleSelection(item.id)
    }
}