package mega.privacy.android.feature.sync.ui.mapper.solvedissue

import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.ResolutionActionTypeToResolutionNameMapper
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.icon.pack.R as iconPackR
import timber.log.Timber
import javax.inject.Inject

internal class SolvedIssueItemMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val resolutionActionTypeToResolutionNameMapper: ResolutionActionTypeToResolutionNameMapper,
) {

    operator fun invoke(
        solvedIssue: SolvedIssue,
        nodes: List<UnTypedNode>,
    ): SolvedIssueUiItem {
        val node = nodes.firstOrNull()
        val solvedIssueType =
            StalledIssueResolutionActionType.entries.find { it.name == solvedIssue.resolutionExplanation }
                ?: StalledIssueResolutionActionType.UNKNOWN
        return SolvedIssueUiItem(
            nodeIds = solvedIssue.nodeIds,
            nodeNames = nodes.map { it.name },
            localPaths = solvedIssue.localPaths.map { path ->
                runCatching {
                    if (path.startsWith("content://")) {
                        // [tree, primary:Ringtones, document, primary:Ringtones, Folder 1, Folder 1, Folder 1, F01.pdf]
                        UriPath(path).toUri().pathSegments.drop(3).dropLast(1).joinToString("/")
                    } else path
                }.getOrElse {
                    Timber.e("Error parsing local path: $it")
                    path // Fallback to original path if parsing fails
                }
            },
            resolutionExplanation = resolutionActionTypeToResolutionNameMapper(solvedIssueType),
            icon = when (node) {
                is FolderNode -> node.getIcon()
                is FileNode -> fileTypeIconMapper(node.type.extension)
                else -> solvedIssue.localPaths.firstOrNull()?.let {
                    fileTypeIconMapper(it.substringAfterLast('.'))
                } ?: iconPackR.drawable.ic_generic_medium_solid
            },
        )
    }
}
