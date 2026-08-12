package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import mega.android.core.ui.components.divider.SubtleDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.tabs.LocalTabContentModifier
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.BOTTOM_PADDING
import mega.privacy.android.feature.sync.ui.views.StalledIssueCard
import mega.privacy.android.feature.sync.ui.views.SyncListNoItemsPlaceHolder
import mega.privacy.android.icon.pack.R as iconPackR
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

@Composable
internal fun StalledIssuesScreen(
    stalledIssues: List<StalledIssueUiItem>,
    moreClicked: (StalledIssueUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    StalledIssuesScreenContent(stalledIssues, modifier, moreClicked)
}

@Composable
private fun StalledIssuesScreenContent(
    stalledIssues: List<StalledIssueUiItem>,
    modifier: Modifier,
    moreClicked: (StalledIssueUiItem) -> Unit,
) {
    val modifierList = LocalTabContentModifier.current ?: Modifier
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(modifierList),
        contentPadding = PaddingValues(bottom = if (stalledIssues.isEmpty()) 0.dp else BOTTOM_PADDING.dp),
    ) {
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
            items(count = stalledIssues.size, key = {
                stalledIssues[it].id
            }) { itemIndex ->
                val issue = stalledIssues[itemIndex]
                StalledIssueCard(
                    nodeName = issue.displayedName,
                    conflictName = issue.conflictName,
                    modifier = modifier,
                    icon = issue.icon,
                    moreClicked = { moreClicked(issue) },
                    shouldShowMoreIcon = issue.actions.isNotEmpty(),
                    nodePath = issue.displayedPath,
                )
                SubtleDivider(Modifier.padding(start = 72.dp))
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun StalledIssuesScreenEmptyStatePreview() {
    AndroidThemeForPreviews {
        StalledIssuesScreen(
            stalledIssues = emptyList(),
            moreClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun StalledIssuesScreenPreview() {
    AndroidThemeForPreviews {
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
                    actions = listOf(stalledIssueResolutionAction),
                    displayedName = "Folder name",
                    displayedPath = "Folder path",
                    id = "1_1_0",
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
                    actions = listOf(stalledIssueResolutionAction),
                    displayedName = "Folder name",
                    displayedPath = "Folder path",
                    id = "1_2_0",
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
                    actions = listOf(stalledIssueResolutionAction),
                    displayedName = "Folder name",
                    displayedPath = "Folder path",
                    id = "1_3_0",
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
                    actions = listOf(stalledIssueResolutionAction),
                    displayedName = "Folder name",
                    displayedPath = "Folder path",
                    id = "1_4_0",
                ),
            ),
            moreClicked = {},
        )
    }
}
