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
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(fileNode: TypedFileNode): FileNodeContent = when {
        fileNode.type is PdfFileTypeInfo -> FileNodeContent.Pdf(
            uri = getNodeContentUriUseCase(fileNode)
        )

        fileNode.type is ImageFileTypeInfo -> FileNodeContent.ImageForNode

        fileNode.type is TextFileTypeInfo && fileNode.size <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> FileNodeContent.TextContent

        fileNode.type is VideoFileTypeInfo || fileNode.type is AudioFileTypeInfo -> {
            FileNodeContent.AudioOrVideo(
                uri = getNodeContentUriUseCase(fileNode)
            )
        }

        fileNode.type is UrlFileTypeInfo -> {
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