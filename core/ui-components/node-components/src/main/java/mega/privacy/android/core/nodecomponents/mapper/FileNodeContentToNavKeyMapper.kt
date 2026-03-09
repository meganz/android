package mega.privacy.android.core.nodecomponents.mapper

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey
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
     * @param nodeSourceData The node source data describing the navigation context
     * @param sortOrder The sort order for media player (default: ORDER_NONE)
     * @param textEditorMode The text editor mode (default: View)
     * @param searchedItems The searched items for media player (default: null)
     * @param isTextEditorComposeEnabled Whether to use Compose text editor screen (default: false)
     * @return The corresponding NavKey or null if no navigation is found for this content
     */
    operator fun invoke(
        content: FileNodeContent,
        fileNode: TypedFileNode,
        nodeSourceData: NodeSourceData,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        textEditorMode: TextEditorMode = TextEditorMode.View,
        searchedItems: List<Long>? = null,
        isTextEditorComposeEnabled: Boolean = false,
        isPDFViewerEnabled: Boolean = false,
    ): NavKey? {
        val viewType = nodeSourceTypeToViewTypeMapper(nodeSourceData.nodeSourceType)
        val nodeIds = (nodeSourceData as? NodeSourceData.RecentsBucket)?.nodeIds
        val isInShare = (nodeSourceData as? NodeSourceData.RecentsBucket)?.isInShare ?: false
        val publicUrl = (nodeSourceData as? NodeSourceData.FileLink)?.url

        return when (content) {
            is FileNodeContent.Pdf -> if (isPDFViewerEnabled.not()) {
                LegacyPdfViewerNavKey(
                    nodeHandle = fileNode.id.longValue,
                    nodeContentUri = content.uri,
                    nodeSourceType = viewType,
                    mimeType = fileNode.type.mimeType
                )
            } else {
                val (contentUri, isLocal, shouldStop) = when (val uri = content.uri) {
                    is NodeContentUri.LocalContentUri -> Triple(uri.file.path, true, false)
                    is NodeContentUri.RemoteContentUri -> Triple(
                        uri.url,
                        false,
                        uri.shouldStopHttpSever
                    )
                }
                PdfViewerNavKey(
                    nodeHandle = fileNode.id.longValue,
                    contentUri = contentUri,
                    isLocalContent = isLocal,
                    shouldStopHttpServer = shouldStop,
                    nodeSourceType = nodeSourceData.nodeSourceType,
                    mimeType = fileNode.type.mimeType,
                    isFolderLink = nodeSourceData is NodeSourceData.FolderLink,
                    title = null,
                )
            }

            is FileNodeContent.ImageForNode -> LegacyImageViewerNavKey(
                nodeHandle = fileNode.id.longValue,
                parentNodeHandle = if (nodeSourceData is NodeSourceData.FileLink) {
                    -1L
                } else {
                    fileNode.parentId.longValue
                },
                nodeSourceType = viewType,
                nodeIds = nodeIds,
                isInShare = isInShare,
                url = publicUrl
            )

            is FileNodeContent.TextContent -> LegacyTextEditorNavKey(
                nodeHandle = fileNode.id.longValue,
                mode = textEditorMode.value,
                nodeSourceType = viewType,
                isTextEditorComposeEnabled = isTextEditorComposeEnabled
            )

            is FileNodeContent.AudioOrVideo -> LegacyMediaPlayerNavKey(
                nodeHandle = fileNode.id.longValue,
                nodeContentUri = content.uri,
                nodeSourceType = viewType,
                sortOrder = sortOrder,
                isFolderLink = nodeSourceData is NodeSourceData.FolderLink,
                fileName = fileNode.name,
                parentHandle = if (nodeSourceData is NodeSourceData.FileLink) {
                    -1L
                } else {
                    fileNode.parentId.longValue
                },
                fileHandle = fileNode.id.longValue,
                fileTypeInfo = fileNode.type,
                searchedItems = searchedItems,
                nodeHandles = nodeIds,
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
