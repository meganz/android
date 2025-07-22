package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack


/**
 * Header View item for [NodesView] or [NodeGridView]
 * @param onSortOrderClick callback triggered when sort order is clicked
 * @param onChangeViewTypeClick callback triggered when view type toggle is clicked
 * @param onEnterMediaDiscoveryClick callback triggered when media discovery button is clicked
 * @param sortOrder current sort order name from resource
 * @param isListView current view type - true for list view, false for grid view
 * @param showSortOrder whether to show the sort order section
 * @param showChangeViewType whether to show the view type toggle button
 * @param modifier optional [Modifier] for this composable
 * @param showMediaDiscoveryButton whether to show the media discovery button
 */
@Composable
fun NodeHeaderItem(
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    isListView: Boolean,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
    modifier: Modifier = Modifier,
    showMediaDiscoveryButton: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DSTokens.spacings.s3),
    ) {
        if (showSortOrder) {
            Row(
                modifier = Modifier
                    .padding(DSTokens.spacings.s3)
                    .clickable {
                        onSortOrderClick()
                    }
            ) {
                MegaText(
                    style = AppTheme.typography.titleSmall,
                    textColor = TextColor.Secondary,
                    text = sortOrder,
                    modifier = Modifier.testTag(SORT_ORDER_TAG)
                )
                Spacer(modifier = Modifier.size(DSTokens.spacings.s2))
                MegaIcon(
                    imageVector = IconPack.Small.Thin.Outline.ArrowDown,
                    tint = IconColor.Secondary,
                    contentDescription = "DropDown arrow",
                    modifier = Modifier
                        .align(CenterVertically)
                        .size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showMediaDiscoveryButton) {
            MegaIcon(
                imageVector = IconPack.Small.Thin.Outline.Image04,
                tint = IconColor.Secondary,
                contentDescription = "Enter media discovery",
                modifier = Modifier
                    .padding(DSTokens.spacings.s3)
                    .align(CenterVertically)
                    .size(16.dp)
                    .clickable { onEnterMediaDiscoveryClick() }
                    .testTag(MEDIA_DISCOVERY_TAG)
            )
        }
        if (showChangeViewType) {
            if (isListView) {
                MegaIcon(
                    imageVector = IconPack.Small.Thin.Outline.Squares4,
                    tint = IconColor.Secondary,
                    contentDescription = "Switch to grid view",
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(DSTokens.spacings.s3)
                        .size(16.dp)
                        .clickable {
                            onChangeViewTypeClick()
                        }
                        .testTag(GRID_VIEW_TOGGLE_TAG)
                )
            } else {
                MegaIcon(
                    imageVector = IconPack.Small.Thin.Outline.ListSmall,
                    tint = IconColor.Secondary,
                    contentDescription = "Switch to list view",
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(DSTokens.spacings.s3)
                        .size(16.dp)
                        .clickable {
                            onChangeViewTypeClick()
                        }
                        .testTag(LIST_VIEW_TOGGLE_TAG)
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun NodeHeaderItemListPreview() {
    AndroidThemeForPreviews {
        NodeHeaderItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            onEnterMediaDiscoveryClick = {},
            isListView = true,
            sortOrder = "Name",
            showSortOrder = true,
            showChangeViewType = true,
            showMediaDiscoveryButton = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeHeaderItemGridPreview() {
    AndroidThemeForPreviews {
        NodeHeaderItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            onEnterMediaDiscoveryClick = {},
            isListView = false,
            sortOrder = "Name",
            showSortOrder = true,
            showChangeViewType = true,
            showMediaDiscoveryButton = true
        )
    }
}


const val MEDIA_DISCOVERY_TAG = "header_view_item:image_media_discovery"
const val GRID_VIEW_TOGGLE_TAG = "header_view_item:grid_view_toggle"
const val LIST_VIEW_TOGGLE_TAG = "header_view_item:list_view_toggle"
const val SORT_ORDER_TAG = "header_view_item:sort_order"
