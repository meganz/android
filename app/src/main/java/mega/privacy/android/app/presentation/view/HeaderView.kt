package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Header View item for [NodesView] or [NodeGridView]
 * @param isListView current view type
 * @param onChangeViewTypeClick changeViewType Click
 * @param onSortOrderClick change sort order click
 * @param sortOrder current sort name from resource
 */
@Composable
fun HeaderViewItem(
    modifier: Modifier,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    sortOrder: String,
    isListView: Boolean,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
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
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showChangeViewType) {
            Column(
                modifier = Modifier.clickable {
                    onChangeViewTypeClick()
                }
            ) {
                Image(
                    painter = if (isListView) painterResource(id = R.drawable.ic_grid_view_new) else painterResource(
                        id = R.drawable.ic_list_view_new
                    ),
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorSecondary),
                    contentDescription = "DropDown arrow"
                )
            }
        }
    }
}

/**
 * PreviewHeaderView
 */
@CombinedThemePreviews
@Composable
fun PreviewHeaderView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        HeaderViewItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            isListView = true,
            sortOrder = "Name",
            showSortOrder = true,
            showChangeViewType = true,
        )
    }
}

@CombinedThemePreviews
@Composable
fun PreviewHeaderViewNoSortOrder() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        HeaderViewItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            isListView = true,
            sortOrder = "Name",
            showSortOrder = false,
            showChangeViewType = true,
        )
    }
}

@CombinedThemePreviews
@Composable
fun PreviewHeaderViewNoViewType() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        HeaderViewItem(
            modifier = Modifier,
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            isListView = true,
            sortOrder = "Name",
            showSortOrder = true,
            showChangeViewType = false,
        )
    }
}