package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUiEntity
import mega.privacy.android.app.presentation.recentactions.view.previewdataprovider.SampleRecentActionDataProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.shared.theme.MegaAppTheme


/**
 * Composable for the recent actions list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActionsListView(
    groupedRecentActions: Map<String, List<RecentActionBucketUiEntity>>,
    onMenuClick: (RecentActionBucket) -> Unit = {},
    onItemClick: (RecentActionBucket) -> Unit = {},
) {
    LazyColumn {
        groupedRecentActions.forEach { (date, list) ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            stickyHeader {
                RecentActionHeaderView(
                    text = date
                )
            }

            items(list) { item ->
                RecentActionListViewItem(
                    firstLineText = item.firstLineText,
                    icon = item.icon,
                    shareIcon = item.shareIcon,
                    actionIcon = item.actionIcon,
                    parentFolderName = item.parentFolderName,
                    showMenuButton = item.showMenuButton,
                    time = item.time,
                    updatedByText = item.updatedByText,
                    isFavourite = item.isFavourite,
                    labelColor = item.labelColor,
                    onItemClick = { onItemClick(item.bucket) },
                    onMenuClick = { onMenuClick(item.bucket) }
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RecentActionListViewPreview(
    @PreviewParameter(SampleRecentActionDataProvider::class) items: List<RecentActionBucketUiEntity>,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RecentActionsListView(groupedRecentActions = items.groupBy { it.date })
    }
}