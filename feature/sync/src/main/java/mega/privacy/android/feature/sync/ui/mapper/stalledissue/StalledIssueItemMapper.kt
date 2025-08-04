package mega.privacy.android.feature.sync.ui.mapper.stalledissue

import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.icon.pack.R as iconPackR
import timber.log.Timber
import javax.inject.Inject

/**
 * Maps [StalledIssue] domain entities to [StalledIssueUiItem] UI models and vice-versa.
 */
internal class StalledIssueItemMapper @Inject constructor(
    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val stalledIssueDetailInfoMapper: StalledIssueDetailInfoMapper,
) {

    /**
     * Maps a [StalledIssue] entity to a [StalledIssueUiItem] UI model.
     *
     * @param stalledIssueEntity The [StalledIssue] entity to map.
     * @param nodes The list of [UnTypedNode] associated with the stalled issue.
     * @return The mapped [StalledIssueUiItem].
     */
    operator fun invoke(
        stalledIssueEntity: StalledIssue,
        nodes: List<UnTypedNode>,
    ): StalledIssueUiItem {
        val firstNode = nodes.firstOrNull()
        val areAllNodesFolders = nodes.all { it is FolderNode }
        val detailedInfo = stalledIssueDetailInfoMapper(stalledIssueEntity)
        val nameAndPath =
            stalledIssueEntity.nodeNames.takeIf { it.isNotEmpty() }?.firstOrNull()?.split("/")
                ?: stalledIssueEntity.localPaths.takeIf { it.isNotEmpty() }?.firstOrNull()
                    ?.let {
                        // [tree, primary:Ringtones, document, primary:Ringtones, Folder 1, Folder 1, Folder 1, F01.pdf]
                        UriPath(it).toUri().pathSegments.drop(3)
                    }
        return StalledIssueUiItem(
            id = stalledIssueEntity.id,
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
        ).also {
            Timber.d("StalledIssueUiItem: id ${it.id} Names ${it.nodeNames} LocalPath ${it.localPaths} issueType: ${it.issueType} actions: ${it.actions}")
        }
    }

    /**
     * Maps a [StalledIssueUiItem] UI model to a [StalledIssue] domain entity.
     *
     * @param stalledIssueUiItem The [StalledIssueUiItem] to map.
     * @return The mapped [StalledIssue] entity.
     */
    operator fun invoke(stalledIssueUiItem: StalledIssueUiItem): StalledIssue =
        StalledIssue(
            syncId = stalledIssueUiItem.syncId,
            nodeIds = stalledIssueUiItem.nodeIds,
            localPaths = stalledIssueUiItem.localPaths,
            issueType = stalledIssueUiItem.issueType,
            conflictName = stalledIssueUiItem.conflictName,
            nodeNames = stalledIssueUiItem.nodeNames,
            id = stalledIssueUiItem.id
        )
}
