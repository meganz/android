package mega.privacy.android.feature.sync.ui.mapper.stalledissue

import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject

internal class StalledIssueItemMapper @Inject constructor(
    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val stalledIssueDetailInfoMapper: StalledIssueDetailInfoMapper,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
) {

    suspend operator fun invoke(
        stalledIssueEntity: StalledIssue,
        nodes: List<UnTypedNode>,
    ): StalledIssueUiItem {
        val firstNode = nodes.firstOrNull()
        val areAllNodesFolders = nodes.all { it is FolderNode }
        val detailedInfo = stalledIssueDetailInfoMapper(stalledIssueEntity)
        val nameAndPath = stalledIssueEntity.nodeNames.firstOrNull()?.split("/")
            ?: stalledIssueEntity.localPaths.firstOrNull()
                ?.let { getPathByDocumentContentUriUseCase(it)?.split("/") }
        return StalledIssueUiItem(
            syncId = stalledIssueEntity.syncId,
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
                } ?: iconPackR.drawable.ic_generic_medium_solid
            },
            detailedInfo = detailedInfo,
            actions = stalledIssueResolutionActionMapper(
                stalledIssueEntity.issueType,
                areAllNodesFolders
            ),
            displayedName = nameAndPath?.last() ?: "",
            displayedPath = nameAndPath?.dropLast(1)?.let {
                if (it.size > 1) {
                    it.joinToString(separator = "/")
                } else {
                    it.firstOrNull() ?: ""
                }
            } ?: ""
        )
    }

    operator fun invoke(stalledIssueUiItem: StalledIssueUiItem): StalledIssue =
        StalledIssue(
            syncId = stalledIssueUiItem.syncId,
            nodeIds = stalledIssueUiItem.nodeIds,
            localPaths = stalledIssueUiItem.localPaths,
            issueType = stalledIssueUiItem.issueType,
            conflictName = stalledIssueUiItem.conflictName,
            nodeNames = stalledIssueUiItem.nodeNames,
        )
}
