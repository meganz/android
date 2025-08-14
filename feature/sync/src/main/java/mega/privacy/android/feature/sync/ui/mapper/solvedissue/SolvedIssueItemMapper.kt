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
        val resolutionType = getResolutionType(solvedIssue.resolutionExplanation)
        val nameAndPathPairs = extractNameAndPathPairs(nodes, solvedIssue.localPaths)
        val icon = determineIcon(nodes.firstOrNull(), solvedIssue.localPaths)

        return SolvedIssueUiItem(
            nodeIds = solvedIssue.nodeIds,
            nodeNames = nameAndPathPairs.map { it.first },
            localPaths = nameAndPathPairs.map { it.second },
            resolutionExplanation = resolutionActionTypeToResolutionNameMapper(resolutionType),
            icon = icon,
        )
    }

    private fun getResolutionType(resolutionExplanation: String): StalledIssueResolutionActionType {
        return StalledIssueResolutionActionType.entries
            .find { it.name == resolutionExplanation }
            ?: StalledIssueResolutionActionType.UNKNOWN
    }

    private fun extractNameAndPathPairs(
        nodes: List<UnTypedNode>,
        localPaths: List<String>,
    ): List<Pair<String, String>> {
        return when {
            nodes.isNotEmpty() -> extractFromNodes(nodes, localPaths)
            localPaths.isNotEmpty() -> extractFromPaths(localPaths)
            else -> emptyList()
        }
    }

    private fun extractFromNodes(
        nodes: List<UnTypedNode>,
        localPaths: List<String>,
    ): List<Pair<String, String>> {
        return nodes.mapIndexed { index, node ->
            val path = localPaths.getOrNull(index) ?: ""
            node.name to path.let {
                if (it.startsWith("content://")) {
                    runCatching { parseContentUri(path).second }.getOrElse { throwable ->
                        Timber.e("Error parsing content URI: $path and reason $throwable")
                        ""
                    }
                } else {
                    it
                }
            }
        }
    }

    private fun extractFromPaths(localPaths: List<String>): List<Pair<String, String>> {
        return localPaths.map { path ->
            parsePath(path)
        }
    }

    private fun parsePath(path: String): Pair<String, String> {
        return runCatching {
            if (path.startsWith("content://")) {
                parseContentUri(path)
            } else {
                parseFilePath(path)
            }
        }.getOrElse { throwable ->
            Timber.e(throwable, "Error parsing local path: $path")
            parseFilePath(path) // Fallback to simple path parsing
        }
    }

    private fun parseContentUri(path: String): Pair<String, String> {
        //path segments [tree, primary:Ringtones, document, primary:Ringtones, Folder 1, Folder 1, Folder 1, F01.pdf]
        val pathSegments = UriPath(path).toUri().pathSegments.drop(3)
        val fileName = pathSegments.lastOrNull().orEmpty()
        val directoryPath = pathSegments.dropLast(1).joinToString("/")
        return fileName to directoryPath
    }

    private fun parseFilePath(path: String): Pair<String, String> {
        val fileName = path.substringAfterLast('/', missingDelimiterValue = path)
        val directoryPath = path.substringBeforeLast('/', missingDelimiterValue = "")
        return fileName to directoryPath
    }


    private fun determineIcon(node: UnTypedNode?, localPaths: List<String>): Int {
        return when (node) {
            is FolderNode -> node.getIcon()
            is FileNode -> fileTypeIconMapper(node.type.extension)
            else -> getIconFromPath(localPaths.firstOrNull())
        }
    }

    private fun getIconFromPath(path: String?): Int {
        return path?.let { filePath ->
            val extension = filePath.substringAfterLast('.', "")
            if (extension.isNotEmpty()) {
                fileTypeIconMapper(extension)
            } else {
                iconPackR.drawable.ic_generic_medium_solid
            }
        } ?: iconPackR.drawable.ic_generic_medium_solid
    }
}
