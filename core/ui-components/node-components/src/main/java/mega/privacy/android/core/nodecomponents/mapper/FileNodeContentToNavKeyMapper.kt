package mega.privacy.android.core.nodecomponents.mapper

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey
import javax.inject.Inject

/**
 * Mapper to convert [FileNodeContent] to [NavKey]
 */
class FileNodeContentToNavKeyMapper @Inject constructor(
    private val nodeSourceTypeToViewTypeMapper: NodeSourceTypeToViewTypeMapper,
) {

    /**
     * Convert [FileNodeContent] to [NavKey]
     *
     * @param content The file node content to convert
     * @param fileNode The file node containing metadata
     * @param nodeSourceType The NodeSourceType
     * @param sortOrder The sort order for media player (default: ORDER_NONE)
     * @param textEditorMode The text editor mode (default: View)
     * @return The corresponding NavKey or null if no navigation is found for this content
     */
    operator fun invoke(
        content: FileNodeContent,
        fileNode: TypedFileNode,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        textEditorMode: TextEditorMode = TextEditorMode.View,
    ): NavKey? {
        val viewType = nodeSourceTypeToViewTypeMapper(nodeSourceType)
        return when (content) {
            is FileNodeContent.Pdf -> LegacyPdfViewerNavKey(
                nodeHandle = fileNode.id.longValue,
                nodeContentUri = content.uri,
                nodeSourceType = viewType,
                mimeType = fileNode.type.mimeType
            )

            is FileNodeContent.ImageForNode -> LegacyImageViewerNavKey(
                nodeHandle = fileNode.id.longValue,
                parentNodeHandle = fileNode.parentId.longValue,
                nodeSourceType = viewType
            )

            is FileNodeContent.TextContent -> LegacyTextEditorNavKey(
                nodeHandle = fileNode.id.longValue,
                mode = textEditorMode.value,
                nodeSourceType = viewType
            )

            is FileNodeContent.AudioOrVideo -> LegacyMediaPlayerNavKey(
                nodeHandle = fileNode.id.longValue,
                nodeContentUri = content.uri,
                nodeSourceType = viewType,
                sortOrder = sortOrder,
                isFolderLink = false,
                fileName = fileNode.name,
                parentHandle = fileNode.parentId.longValue,
                fileHandle = fileNode.id.longValue,
                fileTypeInfo = fileNode.type,
            )

            is FileNodeContent.LocalZipFile -> LegacyZipBrowserNavKey(
                zipFilePath = content.localFile.absolutePath
            )

            is FileNodeContent.ImageForChat, is FileNodeContent.Other, is FileNodeContent.UrlContent -> {
                null
            }
        }
    }
}

