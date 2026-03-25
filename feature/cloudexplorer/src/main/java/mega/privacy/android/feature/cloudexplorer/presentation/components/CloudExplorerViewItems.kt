package mega.privacy.android.feature.cloudexplorer.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.shared.nodes.components.NodeGridViewItem
import mega.privacy.android.shared.nodes.components.NodeListViewItem

/**
 * Cloud explorer list row: forwards to [NodeListViewItem] with cloud explorer defaults
 */
@Composable
fun CloudExplorerListViewItem(
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
fun CloudExplorerGridViewItem(
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
        label = label,
    )
}