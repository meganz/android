package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for [NodeActionHandlerViewModel].
 * Eventually this class will be removed when feature flags are removed, no need to create a separate file for it.
 * @param isTextEditorComposeEnabled Whether the Compose text editor feature is enabled (null until loaded).
 * @param isPDFViewerEnabled Whether the Compose PDF viewer feature is enabled (null until loaded).
 */
data class NodeActionHandlerUiState(
    val isTextEditorComposeEnabled: Boolean? = null,
    val isPDFViewerEnabled: Boolean? = null,
)

/**
 * Simplified view model for handling node actions
 * Contains only the use cases needed for HandleNodeAction
 */
@HiltViewModel
class NodeActionHandlerViewModel @Inject constructor(
    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NodeActionHandlerUiState())
    val state: StateFlow<NodeActionHandlerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.TextEditorCompose)
            }.onSuccess { value ->
                _state.update { it.copy(isTextEditorComposeEnabled = value) }
            }
        }
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)
            }.onSuccess { value ->
                Timber.d("PDF Viewer Compose UI feature flag value: $value")
                _state.update { it.copy(isPDFViewerEnabled = value) }
            }
        }
    }

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
