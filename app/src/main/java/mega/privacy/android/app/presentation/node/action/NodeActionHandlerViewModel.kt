package mega.privacy.android.app.presentation.node.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Simplified view model for handling node actions
 * Contains only the use cases needed for HandleNodeAction
 */
@HiltViewModel
class NodeActionHandlerViewModel @Inject constructor(
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : ViewModel() {

    /**
     * Handle file node clicked
     * Determines the type of content and returns appropriate FileNodeContent
     *
     * @param fileNode The file node to handle
     * @return FileNodeContent representing the type of content
     */
    suspend fun handleFileNodeClicked(fileNode: TypedFileNode): FileNodeContent = when {
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

    /**
     * Apply node content uri to intent
     * Maps the NodeContentUri to the intent for opening files
     *
     * @param intent The intent to modify
     * @param content The node content URI
     * @param mimeType The MIME type of the file
     * @param isSupported Whether the file type is supported
     */
    fun applyNodeContentUri(
        intent: Intent,
        content: NodeContentUri,
        mimeType: String,
        isSupported: Boolean = true,
    ) {
        nodeContentUriIntentMapper(intent, content, mimeType, isSupported)
    }

    /**
     * Get file type info for a local file
     *
     * @param file The file to get type info for
     * @return FileTypeInfo for the file
     */
    fun getTypeInfo(file: File) = fileTypeInfoMapper(file.name)
} 