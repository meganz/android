package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import javax.inject.Inject


/**
 * Get [FileNodeContent] for a specific [TypedFileNode]
 */
class GetFileNodeContentForFileNodeUseCase @Inject constructor(
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
) {
    /**
     * Invoke
     *
     * @param fileNode The file node to get content for
     * @param isLinkNode Whether the node belongs to a public link (folder/file link).
     *        When true, content URI is resolved via [GetFolderLinkNodeContentUriUseCase].
     */
    suspend operator fun invoke(
        fileNode: TypedFileNode,
        isLinkNode: Boolean = false,
    ): FileNodeContent {
        val getContentUri = if (isLinkNode) {
            getFolderLinkNodeContentUriUseCase::invoke
        } else {
            getNodeContentUriUseCase::invoke
        }
        return when (fileNode.type) {
            is PdfFileTypeInfo -> FileNodeContent.Pdf(
                uri = getContentUri(fileNode)
            )

            is ImageFileTypeInfo -> FileNodeContent.ImageForNode
            is TextFileTypeInfo if fileNode.size <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> FileNodeContent.TextContent
            is VideoFileTypeInfo, is AudioFileTypeInfo -> {
                FileNodeContent.AudioOrVideo(
                    uri = getContentUri(fileNode)
                )
            }

            is UrlFileTypeInfo -> {
                val content = getNodeContentUriUseCase(fileNode)
                val path = getPathFromNodeContentUseCase(content)
                FileNodeContent.UrlContent(
                    uri = content,
                    path = path
                )
            }

            else -> FileNodeContent.Other(
                localFile = getNodePreviewFileUseCase(fileNode)
            )
        }
    }
}
