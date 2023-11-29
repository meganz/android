package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import javax.inject.Inject

internal class SolvedIssueItemMapper @Inject constructor() {

    operator fun invoke(
        solvedIssue: SolvedIssue,
        nodes: List<UnTypedNode>,
    ): SolvedIssueUiItem = SolvedIssueUiItem(
        nodeIds = solvedIssue.nodeIds,
        nodeNames = nodes.map { it.name },
        localPaths = solvedIssue.localPaths,
        resolutionExplanation = solvedIssue.resolutionExplanation,
        icon = if (nodes.firstOrNull() is FolderNode) {
            R.drawable.ic_folder_list
        } else {
            iconPackR.drawable.ic_generic_list
        },
    )
}