package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUiEntity
import mega.privacy.android.app.presentation.recentactions.view.previewdataprovider.SampleRecentActionDataProvider
import mega.privacy.android.shared.original.core.ui.controls.lists.RecentActionListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme


/**
 * Composable for the recent actions list
 *
 * @param groupedRecentActions     Grouped recent actions
 * @param onMenuClick              Callback when the menu button is clicked
 * @param onItemClick              Callback when an item is clicked
 * @param onScrollStateChanged     Callback when the scroll state changes
 * @param backgroundColor          Background color
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActionsListView(
    groupedRecentActions: Map<String, List<RecentActionBucketUiEntity>>,
    accountType: AccountType? = null,
    showHiddenItems: Boolean = false,
    onMenuClick: (TypedFileNode) -> Unit = {},
    onItemClick: (RecentActionBucket) -> Unit = {},
    onScrollStateChanged: (isScrolling: Boolean) -> Unit = {},
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                onScrollStateChanged(scrolling)
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 86.dp)
    ) {
        groupedRecentActions.forEach { (date, list) ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            stickyHeader {
                RecentActionHeaderView(
                    text = date,
                    backgroundColor = backgroundColor
                )
            }

            items(list) { item ->
                val isSensitive = item.bucket.nodes.firstOrNull()?.let { node ->
                    (node.isMarkedSensitive || node.isSensitiveInherited) && item.bucket.parentFolderSharesType != RecentActionsSharesType.INCOMING_SHARES
                }?.takeIf { item.showMenuButton } ?: false

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
                    labelColor = item.labelColorId?.let { colorResource(id = it) },
                    isSensitive = accountType?.isPaid == true && isSensitive && showHiddenItems,
                    onItemClick = { onItemClick(item.bucket) },
                    onMenuClick = { onMenuClick(item.bucket.nodes.first()) }
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RecentActionsListView(groupedRecentActions = items.groupBy { it.date })
    }
}