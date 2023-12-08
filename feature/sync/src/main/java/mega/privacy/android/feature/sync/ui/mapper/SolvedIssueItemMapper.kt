package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.ui.extensions.getIcon
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import javax.inject.Inject

internal class SolvedIssueItemMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
) {

    operator fun invoke(
        solvedIssue: SolvedIssue,
        nodes: List<UnTypedNode>,
    ): SolvedIssueUiItem {
        val node = nodes.firstOrNull()
        return SolvedIssueUiItem(
            nodeIds = solvedIssue.nodeIds,
            nodeNames = nodes.map { it.name },
            localPaths = solvedIssue.localPaths,
            resolutionExplanation = solvedIssue.resolutionExplanation,
            icon = when (node) {
                is FolderNode -> node.getIcon()
                is FileNode -> fileTypeIconMapper(node.type.extension)
                else -> solvedIssue.localPaths.firstOrNull()?.let {
                    fileTypeIconMapper(it.substringAfterLast('.'))
                } ?: iconPackR.drawable.ic_generic_list
            },
        )
    }
}