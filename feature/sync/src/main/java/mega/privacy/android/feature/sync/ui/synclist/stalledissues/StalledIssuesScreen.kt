package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.views.StalledIssueCard
import mega.privacy.android.feature.sync.ui.views.SyncListNoItemsPlaceHolder
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun StalledIssuesScreen(
    stalledIssues: List<StalledIssueUiItem>,
    issueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    modifier: Modifier = Modifier,
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
    LazyColumn(state = LazyListState(), modifier = modifier.fillMaxSize()) {
        if (stalledIssues.isEmpty()) {
            item {
                SyncListNoItemsPlaceHolder(
                    placeholderText = stringResource(id = R.string.sync_stalled_issues_empty_message),
                    placeholderIcon = iconPackR.drawable.ic_alert_triangle_color,
                    modifier = Modifier
                        .fillParentMaxHeight(0.8f)
                        .fillParentMaxWidth()
                )
            }
        } else {
            items(count = stalledIssues.size) { itemIndex ->
                val issue = stalledIssues[itemIndex]
                StalledIssueCard(
                    nodeName = issue.nodeNames.firstOrNull() ?: issue.localPaths.first(),
                    conflictName = issue.conflictName,
                    modifier = modifier,
                    icon = issue.icon,
                    issueDetailsClicked = { issueDetailsClicked(issue) },
                    moreClicked = { moreClicked(issue) },
                    shouldShowMoreIcon = issue.actions.isNotEmpty()
                )
                Divider(Modifier.padding(start = 72.dp))
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun StalledIssuesScreenEmptyStatePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        StalledIssuesScreen(
            stalledIssues = emptyList(),
            issueDetailsClicked = {},
            moreClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun StalledIssuesScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val detailedInfo = StalledIssueDetailedInfo(
            title = "Conflict title",
            explanation = "Conflict explanation",
        )
        val stalledIssueResolutionAction = StalledIssueResolutionAction(
            actionName = "Action name",
            resolutionActionType = StalledIssueResolutionActionType.UNKNOWN,
        )
        StalledIssuesScreen(
            stalledIssues = listOf(
                StalledIssueUiItem(
                    syncId = 1L,
                    nodeIds = listOf(NodeId(1L)),
                    localPaths = listOf("Folder name"),
                    issueType = StallIssueType.FileIssue,
                    conflictName = "Conflict name",
                    nodeNames = listOf("Node names"),
                    icon = iconPackR.drawable.ic_folder_medium_solid,
                    detailedInfo = detailedInfo,
                    actions = listOf(stalledIssueResolutionAction)
                ),
                StalledIssueUiItem(
                    syncId = 1L,
                    nodeIds = listOf(NodeId(2L)),
                    localPaths = listOf("Folder name"),
                    issueType = StallIssueType.UploadIssue,
                    conflictName = "Conflict name",
                    nodeNames = listOf("Node names"),
                    icon = iconPackR.drawable.ic_text_medium_solid,
                    detailedInfo = detailedInfo,
                    actions = listOf(stalledIssueResolutionAction)
                ),
                StalledIssueUiItem(
                    syncId = 1L,
                    nodeIds = listOf(NodeId(3L)),
                    localPaths = listOf("Folder name"),
                    issueType = StallIssueType.UploadIssue,
                    conflictName = "Conflict name",
                    nodeNames = listOf("Node names"),
                    icon = iconPackR.drawable.ic_word_medium_solid,
                    detailedInfo = detailedInfo,
                    actions = listOf(stalledIssueResolutionAction)
                ),
                StalledIssueUiItem(
                    syncId = 1L,
                    nodeIds = listOf(NodeId(4L)),
                    localPaths = listOf("Folder name"),
                    issueType = StallIssueType.UploadIssue,
                    conflictName = "Conflict name",
                    nodeNames = listOf("Node names"),
                    icon = iconPackR.drawable.ic_generic_medium_solid,
                    detailedInfo = detailedInfo,
                    actions = listOf(stalledIssueResolutionAction)
                ),
            ),
            issueDetailsClicked = {},
            moreClicked = {},
        )
    }
}
