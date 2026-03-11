package mega.privacy.android.feature.texteditor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorComposeUiState
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeActionRequest
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.navigation.contract.TransferHandler
import timber.log.Timber

/** Initial cap on displayed lines so first paint is fast; user can load more by scrolling to bottom. */
private const val INITIAL_DISPLAY_LINES = 1000
/** Lines to add each time the user triggers load more near the end of scroll. */
private const val LINES_TO_ADD_ON_LOAD_MORE = 1000
/** Chunk size for gradual file read; balances responsiveness and I/O overhead. */
private const val CHUNK_SIZE_LINES = 500

/**
 * ViewModel for the Compose text editor screen.
 * Uses MVI-style intent handling: UI emits actions via [onMenuAction], ViewModel processes them.
 * Loads large files gradually so the first chunk is shown quickly without blocking the main thread.
 */
@HiltViewModel(assistedFactory = TextEditorComposeViewModel.Factory::class)
class TextEditorComposeViewModel @AssistedInject constructor(
    @Assisted val args: Args,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getTextContentForTextEditorUseCase: GetTextContentForTextEditorUseCase,
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
    private val nodeActionHandler: TextEditorNodeActionHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TextEditorComposeUiState(
            fileName = args.fileName.orEmpty(),
            mode = args.mode,
            isLoading = args.mode != TextEditorMode.Create,
            bottomBarActions = emptyList(),
        )
    )
    val uiState = _uiState.asStateFlow()

    /** Accumulated lines from gradual load; not restored after process death (user sees first chunk again). */
    private val fullContentLines = mutableListOf<String>()
    private var displayLineCap = INITIAL_DISPLAY_LINES

    private fun updateDisplayedContent() {
        _uiState.update {
            it.copy(
                content = fullContentLines.take(displayLineCap).joinToString("\n"),
                hasMoreLines = fullContentLines.size > displayLineCap,
                totalLinesLoaded = fullContentLines.size,
            )
        }
    }

    init {
        if (args.mode != TextEditorMode.Create) {
            viewModelScope.launch {
                _uiState.update { it.copy(isFullyLoaded = false) }
                getTextContentForTextEditorUseCase(
                    nodeHandle = args.nodeHandle,
                    localPath = args.localPath,
                    chunkSizeLines = CHUNK_SIZE_LINES,
                )
                    .catch { e ->
                        Timber.e(e, "Text editor: failed to load content gradually")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorEvent = triggered,
                                loadErrorMessage = e.message.orEmpty().ifBlank { null },
                            )
                        }
                    }
                    .collect { chunk ->
                        fullContentLines.addAll(chunk)
                        _uiState.update {
                            it.copy(
                                content = fullContentLines.take(displayLineCap).joinToString("\n"),
                                isLoading = false,
                                errorEvent = consumed,
                                hasMoreLines = fullContentLines.size > displayLineCap,
                                totalLinesLoaded = fullContentLines.size,
                            )
                        }
                    }
                _uiState.update { it.copy(isFullyLoaded = true) }
            }
            viewModelScope.launch {
                val actions = runCatching {
                    withContext(ioDispatcher) {
                        val node = getNodeByIdUseCase(NodeId(args.nodeHandle))
                        val accessPermission = getNodeAccessUseCase(NodeId(args.nodeHandle))
                        val isNodeExported = node?.exportedData != null
                        computeTextEditorBottomBarActions(
                            args.mode,
                            accessPermission,
                            isNodeExported,
                            args.inExcludedAdapterForGetLinkAndEdit,
                            args.showDownload,
                            args.showShare,
                        )
                    }
                }.getOrElse { emptyList() }
                _uiState.update { it.copy(bottomBarActions = actions) }
            }
        } else if (args.mode == TextEditorMode.Create) {
            _uiState.update { it.copy(content = "", isLoading = false, errorEvent = consumed) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): TextEditorComposeViewModel
    }

    data class Args(
        val nodeHandle: Long,
        val mode: TextEditorMode,
        val nodeSourceType: Int?,
        val fileName: String?,
        val inExcludedAdapterForGetLinkAndEdit: Boolean = false,
        val showDownload: Boolean = true,
        val showShare: Boolean = true,
        val transferHandler: TransferHandler,
        /** Reserved for send-to-chat flow (e.g. when opening editor from chat). */
        val chatId: Long? = null,
        /** Reserved for send-to-chat flow (e.g. when opening editor from chat). */
        val messageId: Long? = null,
        val localPath: String? = null,
    )

    fun setViewMode() {
        _uiState.update {
            it.copy(mode = TextEditorMode.View)
        }
    }

    fun setEditMode() {
        _uiState.update {
            it.copy(mode = TextEditorMode.Edit)
        }
    }

    fun updateContent(newContent: String) {
        _uiState.update {
            it.copy(content = newContent, isFileEdited = true)
        }
    }

    /**
     * Expands the displayed content by [LINES_TO_ADD_ON_LOAD_MORE] lines when content was capped (gradual load).
     */
    fun onLoadMoreLines() {
        if (fullContentLines.size <= displayLineCap) return
        displayLineCap = (displayLineCap + LINES_TO_ADD_ON_LOAD_MORE).coerceAtMost(fullContentLines.size)
        updateDisplayedContent()
    }

    /**
     * Saves text content. When content was capped for display (gradual load), merges
     * the user-edited visible portion with the original non-displayed tail.
     * Contract: [displayLineCap] reflects how many original lines were shown to the user,
     * so `fullContentLines.drop(displayLineCap)` always yields the untouched tail.
     * In Create mode [fullContentLines] is empty, so `state.content` is saved as-is.
     */
    fun saveFile(fromHome: Boolean) {
        if (args.mode == TextEditorMode.View) return
        viewModelScope.launch {
            val state = _uiState.value
            val fullTextToSave = if (fullContentLines.isNotEmpty()) {
                val editedLines = state.content.split("\n")
                (editedLines + fullContentLines.drop(displayLineCap)).joinToString("\n")
            } else {
                state.content
            }
            runCatching {
                saveTextContentForTextEditorUseCase(
                    nodeHandle = args.nodeHandle,
                    nodeSourceType = args.nodeSourceType ?: 0,
                    text = fullTextToSave,
                    fileName = state.fileName.ifEmpty { "untitled.txt" },
                    mode = state.mode,
                    fromHome = fromHome,
                )
            }.fold(
                onSuccess = { saveResult ->
                    when (saveResult) {
                        is TextEditorSaveResult.UploadRequired ->
                            _uiState.update {
                                it.copy(
                                    transferEvent = triggered(
                                        TransferTriggerEvent.StartUpload.TextFile(
                                            path = saveResult.tempPath,
                                            destinationId = NodeId(saveResult.parentHandle),
                                            isEditMode = saveResult.isEditMode,
                                            fromHomePage = saveResult.fromHome,
                                        )
                                    ),
                                    isFileEdited = false,
                                )
                            }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(errorEvent = triggered) }
                },
            )
        }
    }

    fun consumeErrorEvent() {
        _uiState.update { it.copy(errorEvent = consumed, loadErrorMessage = null) }
    }

    fun consumeTransferEvent() {
        _uiState.update { it.copy(transferEvent = consumed()) }
    }

    /**
     * Handles top bar action intents from the UI (MVI pattern).
     */
    fun onMenuAction(action: TextEditorTopBarAction) {
        when (action) {
            TextEditorTopBarAction.Download -> {}
            TextEditorTopBarAction.GetLink -> {}
            TextEditorTopBarAction.SendToChat -> {}
            TextEditorTopBarAction.Share -> {}
            TextEditorTopBarAction.LineNumbers -> {
                _uiState.update {
                    it.copy(showLineNumbers = !it.showLineNumbers)
                }
            }
        }
    }

    /**
     * Handles bottom bar action intents. Download, GetLink, Share are handled via [nodeActionHandler];
     * Edit switches to edit mode; SendToChat is no-op for now.
     */
    fun onBottomBarAction(action: TextEditorBottomBarAction) {
        when (action) {
            is TextEditorBottomBarAction.Download -> {
                nodeActionHandler.handle(
                    viewModelScope,
                    TextEditorNodeActionRequest.Download(args.nodeHandle),
                    args.transferHandler,
                )
            }

            is TextEditorBottomBarAction.GetLink -> {
                nodeActionHandler.handle(
                    viewModelScope,
                    TextEditorNodeActionRequest.GetLink(args.nodeHandle),
                    args.transferHandler,
                )
            }

            is TextEditorBottomBarAction.Share -> {
                nodeActionHandler.handle(
                    viewModelScope,
                    TextEditorNodeActionRequest.Share(args.nodeHandle),
                    args.transferHandler,
                )
            }

            is TextEditorBottomBarAction.Edit -> setEditMode()
            is TextEditorBottomBarAction.SendToChat -> { /* No-op; reserved for later phase */
            }
        }
    }
}
