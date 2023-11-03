package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.views.StalledIssueCard

@Composable
internal fun StalledIssuesScreen(
    modifier: Modifier = Modifier,
    stalledIssues: List<StalledIssueUiItem>,
    issueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
) {
    StalledIssuesScreenContent(stalledIssues, modifier, issueDetailsClicked, moreClicked)
}

@Composable
private fun StalledIssuesScreenContent(
    stalledIssues: List<StalledIssueUiItem>,
    modifier: Modifier,
    issueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
) {
    LazyColumn(state = LazyListState()) {
        val itemsCount = stalledIssues.size
        if (itemsCount == 0) {
            item {
                NoStalledIssuesPlaceholder(
                    modifier
                        .fillParentMaxHeight(0.8f)
                        .fillParentMaxWidth()
                )
            }
        } else {
            items(count = stalledIssues.size, key = {
                stalledIssues[it].nodeIds.first().longValue
            }) { itemIndex ->
                val issue = stalledIssues[itemIndex]
                StalledIssueCard(
                    nodeName = issue.nodeNames.first(),
                    conflictName = issue.conflictName,
                    icon = issue.icon,
                    issueDetailsClicked = { issueDetailsClicked(stalledIssues[itemIndex]) },
                    moreClicked = {
                        moreClicked(stalledIssues[itemIndex])
                    },
                )
                Divider(Modifier.padding(start = 72.dp))
            }
        }
    }
}