package mega.privacy.android.feature.sync.ui.mapper.stalledissue

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import javax.inject.Inject

internal class StalledIssueItemMapper @Inject constructor(
    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val stalledIssueDetailInfoMapper: StalledIssueDetailInfoMapper,
) {

    operator fun invoke(
        stalledIssueEntity: StalledIssue,
        nodes: List<UnTypedNode>,
    ): StalledIssueUiItem {
        val firstNode = nodes.firstOrNull()
        val areAllNodesFolders = nodes.all { it is FolderNode }
        val detailedInfo = stalledIssueDetailInfoMapper(stalledIssueEntity)
        return StalledIssueUiItem(
            nodeIds = stalledIssueEntity.nodeIds,
            localPaths = stalledIssueEntity.localPaths,
            issueType = stalledIssueEntity.issueType,
            conflictName = detailedInfo.title,
            nodeNames = stalledIssueEntity.nodeNames,
            icon = when (firstNode) {
                is FolderNode -> firstNode.getIcon()
                is FileNode -> fileTypeIconMapper(firstNode.type.extension)
                else -> stalledIssueEntity.nodeNames.firstOrNull()?.let {
                    fileTypeIconMapper(it.substringAfterLast('.'))
                } ?: iconPackR.drawable.ic_generic_list
            },
            detailedInfo = detailedInfo,
            actions = stalledIssueResolutionActionMapper(
                stalledIssueEntity.issueType,
                areAllNodesFolders
            ),
        )
    }

    operator fun invoke(stalledIssueUiItem: StalledIssueUiItem): StalledIssue =
        StalledIssue(
            nodeIds = stalledIssueUiItem.nodeIds,
            localPaths = stalledIssueUiItem.localPaths,
            issueType = stalledIssueUiItem.issueType,
            conflictName = stalledIssueUiItem.conflictName,
            nodeNames = stalledIssueUiItem.nodeNames,
        )
}