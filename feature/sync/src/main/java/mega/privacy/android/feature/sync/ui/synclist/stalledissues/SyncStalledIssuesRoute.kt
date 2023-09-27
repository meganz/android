package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.feature.sync.ui.model.StallIssueType
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.NoItemsPlaceholder
import mega.privacy.android.feature.sync.ui.views.StalledIssueCard

@Composable
internal fun SyncStalledIssuesRoute(
    modifier: Modifier = Modifier,
) {
    val mockStalledIssues = getMockStalledIssueUiItems()

    LazyColumn(state = LazyListState(), modifier = modifier) {
        val itemsCount = mockStalledIssues.size
        if (itemsCount == 0) {
            item {
                NoItemsPlaceholder(
                    modifier
                        .fillParentMaxHeight(0.8f)
                        .fillParentMaxWidth()
                )
            }
        } else {
            items(count = mockStalledIssues.size, key = {
                mockStalledIssues[it].id
            }) { itemIndex ->
                val issue = mockStalledIssues[itemIndex]
                StalledIssueCard(
                    nodeName = issue.nodeName,
                    conflictName = issue.conflictName,
                    icon = R.drawable.ic_folder_list,
                    issueDetailsClicked = { },
                    moreClicked = { }
                )
                Divider(Modifier.padding(start = 72.dp))
            }
        }
    }
}

private fun getMockStalledIssueUiItems() = listOf(
    StalledIssueUiItem(
        id = 1,
        nodeId = 1,
        issueType = StallIssueType.UploadIssue,
        "Naming conflict",
        "some folder 1",
    ),
    StalledIssueUiItem(
        id = 2,
        nodeId = 2,
        issueType = StallIssueType.DeleteWaitingOnMoves,
        "Concurrent modification conflict",
        "some folder 2",
    ),
    StalledIssueUiItem(
        id = 3,
        nodeId = 3,
        issueType = StallIssueType.UploadIssue,
        "Clashing names conflict",
        "Another folder 3",
    )
)