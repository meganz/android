package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

const val MEDIA_DISCOVERY_TAG = "header_view_item:image_media_discovery"

/**
 * Header View item for [NodesView] or [NodeGridView]
 * @param isListView current view type
 * @param onChangeViewTypeClick changeViewType Click
 * @param onSortOrderClick change sort order click
 * @param sortOrder current sort name from resource
 */
@Composable
fun HeaderViewItem(
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
            .padding(start = 8.dp, end = 8.dp),
    ) {
        if (showSortOrder) {
            Row(
                modifier = Modifier.clickable {
                    onSortOrderClick()
                }
            ) {
                Text(
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.textColorPrimary,
                    text = sortOrder
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_down),
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorSecondary),
                    contentDescription = "DropDown arrow",
                    modifier = Modifier.align(CenterVertically),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showMediaDiscoveryButton) {
            Image(
                modifier = Modifier
                    .align(CenterVertically)
                    .padding(end = 16.dp)
                    .clickable { onEnterMediaDiscoveryClick() }
                    .testTag(MEDIA_DISCOVERY_TAG),
                painter = painterResource(id = R.drawable.ic_media_discovery),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorSecondary),
                contentDescription = "Enter media discovery"
            )
        }
        if (showChangeViewType) {
            Image(
                modifier = Modifier
                    .align(CenterVertically)
                    .clickable {
                        onChangeViewTypeClick()
                    },
                painter = if (isListView) {
                    painterResource(id = R.drawable.ic_grid_view_new)
                } else {
                    painterResource(id = R.drawable.ic_list_view_new)
                },
                colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorSecondary),
                contentDescription = "Toggle grid list"
            )
        }
    }
}

/**
 * PreviewHeaderView
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeaderView(
    @PreviewParameter(TwoBooleansProvider::class) params: Pair<Boolean, Boolean>,
) {
    val (showSortOrder, showChangeViewType) = params
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        HeaderViewItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            onEnterMediaDiscoveryClick = {},
            isListView = true,
            sortOrder = "Name",
            showSortOrder = showSortOrder,
            showChangeViewType = showChangeViewType,
        )
    }
}

private class TwoBooleansProvider : PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values = listOf(
        Pair(true, true),
        Pair(true, false),
        Pair(false, true),
    ).asSequence()
}