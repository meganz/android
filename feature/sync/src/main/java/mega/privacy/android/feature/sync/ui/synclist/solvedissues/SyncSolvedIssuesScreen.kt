package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.feature.sync.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.views.SyncListNoItemsPlaceHolder
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionNodeHeaderWithBody

@Composable
internal fun SyncSolvedIssuesScreen(solvedIssues: List<SolvedIssueUiItem>) {
    LazyColumn(content = {
        if (solvedIssues.isEmpty()) {
            item {
                SyncListNoItemsPlaceHolder(
                    placeholderText = "No Solved Issues",
                    placeholderIcon = R.drawable.ic_no_solved_issues,
                    modifier = Modifier
                        .fillParentMaxHeight(0.8f)
                        .fillParentMaxWidth()
                )
            }
        } else {
            items(solvedIssues) { solvedIssue ->
                MenuActionNodeHeaderWithBody(
                    title = solvedIssue.localPaths.firstOrNull() ?: solvedIssue.nodeNames.first(),
                    body = solvedIssue.resolutionExplanation,
                    nodeIcon = solvedIssue.icon,
                    bodyIcon = CoreUiR.drawable.ic_check_circle,
                    bodyIconColor = MaterialTheme.colors.secondary
                )
                Divider(
                    Modifier
                        .padding(start = 72.dp)
                        .testTag(SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY)
                )
            }
        }
    })
}

@CombinedThemePreviews
@Composable
internal fun SyncSolvedIssuesScreenPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncSolvedIssuesScreen(
            listOf(
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(1L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = CoreUiR.drawable.ic_folder_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(2L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = CoreUiR.drawable.ic_folder_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(3L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All duplicates were removed",
                    icon = iconPackR.drawable.ic_generic_list
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(4L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All items were renamed",
                    icon = iconPackR.drawable.ic_generic_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(6L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = iconPackR.drawable.ic_generic_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(7L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = iconPackR.drawable.ic_generic_list,
                )
            )
        )
    }
}

internal const val SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY =
    "solve_issues_menu_action_node_header_with_body"