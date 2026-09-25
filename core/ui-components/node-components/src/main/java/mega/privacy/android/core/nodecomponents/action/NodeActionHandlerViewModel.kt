package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
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
     * @param isLinkNode Whether the node belongs to a public link (folder/file link).
     *                   When true, content URI is resolved using the folder link API.
     * @return [FileNodeContent] representing the type of content
     */
    suspend fun handleFileNodeClicked(
        fileNode: TypedFileNode,
        isLinkNode: Boolean = false,
    ): FileNodeContent = getFileNodeContentForFileNodeUseCase(fileNode, isLinkNode)

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
