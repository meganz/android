package mega.privacy.android.feature.sync.ui.synclist.solvedissues

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
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionNodeHeaderWithBody

@Composable
internal fun SyncSolvedIssuesScreen(solvedIssues: List<SolvedIssueUiItem>) {
    LazyColumn(content = {
        items(solvedIssues) { solvedIssue ->
            MenuActionNodeHeaderWithBody(
                title = solvedIssue.localPaths.first(),
                body = solvedIssue.resolutionExplanation,
                nodeIcon = solvedIssue.icon,
                bodyIcon = R.drawable.ic_check_circle,
                bodyIconColor = MaterialTheme.colors.secondary
            )
            Divider(
                Modifier.padding(start = 72.dp)
                    .testTag(SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY)
            )
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
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = R.drawable.ic_folder_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(2L)),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = R.drawable.ic_folder_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(3L)),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All duplicates were removed",
                    icon = R.drawable.ic_generic_list
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(4L)),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All items were renamed",
                    icon = R.drawable.ic_generic_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(6L)),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = R.drawable.ic_generic_list,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(7L)),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = R.drawable.ic_generic_list,
                )
            )
        )
    }
}

internal const val SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY = "solve_issues_menu_action_node_header_with_body"