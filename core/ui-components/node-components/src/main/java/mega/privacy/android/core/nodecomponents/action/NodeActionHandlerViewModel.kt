package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
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
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import javax.inject.Inject

/**
 * Simplified view model for handling node actions
 * Contains only the use cases needed for HandleNodeAction
 */
@HiltViewModel
class NodeActionHandlerViewModel @Inject constructor(
    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
) : ViewModel() {

    /**
     * Handle file node clicked
     * Determines the type of content and returns appropriate FileNodeContent
     *
     * @param fileNode The file node to handle
     * @return FileNodeContent representing the type of content
     */
    suspend fun handleFileNodeClicked(fileNode: TypedFileNode) =
        getFileNodeContentForFileNodeUseCase(fileNode)

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
}