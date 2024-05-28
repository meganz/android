package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.views.SyncListNoItemsPlaceHolder
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionNodeHeaderWithBody
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun SyncSolvedIssuesScreen(solvedIssues: List<SolvedIssueUiItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        content = {
            if (solvedIssues.isEmpty()) {
                item {
                    SyncListNoItemsPlaceHolder(
                        placeholderText = stringResource(id = sharedResR.string.device_center_sync_solved_issues_empty_state_text),
                        placeholderIcon = IconPackR.drawable.ic_check_circle_color,
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillParentMaxWidth()
                    )
                }
            } else {
                items(solvedIssues) { solvedIssue ->
                    MenuActionNodeHeaderWithBody(
                        title = solvedIssue.nodeNames.firstOrNull()
                            ?: solvedIssue.localPaths.first(),
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
internal fun SyncSolvedIssuesScreenEmptyStatePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncSolvedIssuesScreen(solvedIssues = emptyList())
    }
}

@CombinedThemePreviews
@Composable
internal fun SyncSolvedIssuesScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncSolvedIssuesScreen(
            listOf(
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(1L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = IconPackR.drawable.ic_folder_medium_solid,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(2L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "Folders were merged",
                    icon = IconPackR.drawable.ic_folder_medium_solid,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(3L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All duplicates were removed",
                    icon = IconPackR.drawable.ic_generic_medium_solid
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(4L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("File name"),
                    resolutionExplanation = "All items were renamed",
                    icon = IconPackR.drawable.ic_generic_medium_solid,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(6L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = IconPackR.drawable.ic_generic_medium_solid,
                ),
                SolvedIssueUiItem(
                    nodeIds = listOf(NodeId(7L)),
                    nodeNames = listOf("Folder name"),
                    localPaths = listOf("Folder name"),
                    resolutionExplanation = "All items were renamed",
                    icon = IconPackR.drawable.ic_generic_medium_solid,
                )
            )
        )
    }
}

internal const val SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY =
    "solve_issues_menu_action_node_header_with_body"