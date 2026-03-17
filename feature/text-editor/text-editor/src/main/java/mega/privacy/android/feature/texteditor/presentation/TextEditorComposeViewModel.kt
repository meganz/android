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
import mega.privacy.android.domain.qualifier.DefaultDispatcher
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

/** Number of lines per chunk in view mode (virtualised LazyColumn). */
internal const val CHUNK_SIZE = 200

/**
 * ViewModel for the Compose text editor screen.
 * Uses MVI-style intent handling: UI emits actions via [onMenuAction], ViewModel processes them.
 * Loads large files gradually so the first chunk is shown quickly without blocking the main thread.
 */
@HiltViewModel(assistedFactory = TextEditorComposeViewModel.Factory::class)
class TextEditorComposeViewModel @AssistedInject constructor(
    @Assisted val args: Args,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getTextContentForTextEditorUseCase: GetTextContentForTextEditorUseCase,
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
    private val textEditorBottomBarActionsMapper: TextEditorBottomBarActionsMapper,
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

    /**
     * Accumulated lines from gradual load; not restored after process death (user sees first chunk again).
     * Thread safety: all mutations happen on Main (after withContext returns), so no synchronisation is needed.
     * Do NOT mutate this list inside withContext(defaultDispatcher) blocks.
     */
    private val fullContentLines = mutableListOf<String>()
    private var displayLineCap = INITIAL_DISPLAY_LINES

    /** Content at last load or last successful save; restored when user discards changes (Pre-Phase 5.3). */
    private var lastSavedContent: String = ""

    /** Cached displayed content for O(1) dirty-check on the main thread; updated whenever [fullContentLines] or [displayLineCap] change. */
    private var cachedCleanContent: String = ""

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
                                errorMessage = e.message.orEmpty().ifBlank { null },
                            )
                        }
                    }
                    .collect { chunk ->
                        fullContentLines.addAll(chunk)
                        val (displayed, fullContent) = withContext(defaultDispatcher) {
                            fullContentLines.take(displayLineCap).joinToString("\n") to
                                    fullContentLines.joinToString("\n")
                        }
                        lastSavedContent = fullContent
                        cachedCleanContent = displayed
                        _uiState.update {
                            it.copy(
                                content = displayed,
                                isLoading = false,
                                errorEvent = consumed,
                                hasMoreLines = fullContentLines.size > displayLineCap,
                                totalLinesLoaded = fullContentLines.size,
                                totalLineCount = fullContentLines.size,
                            )
                        }
                    }
                _uiState.update { it.copy(isFullyLoaded = true) }
            }
            viewModelScope.launch {
                val (nodeName, actions) = runCatching {
                    val node = getNodeByIdUseCase(NodeId(args.nodeHandle))
                    val accessPermission = getNodeAccessUseCase(NodeId(args.nodeHandle))
                    val isNodeExported = node?.exportedData != null
                    val name = node?.name
                    name to textEditorBottomBarActionsMapper(
                        args.mode,
                        accessPermission,
                        isNodeExported,
                        args.inExcludedAdapterForGetLinkAndEdit,
                        args.showDownload,
                        args.showShare,
                    )
                }.getOrElse { null to emptyList() }
                _uiState.update {
                    it.copy(
                        fileName = nodeName ?: it.fileName,
                        bottomBarActions = actions,
                    )
                }
            }
        } else if (args.mode == TextEditorMode.Create) {
            lastSavedContent = ""
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
        val fileName: String?,
        val inExcludedAdapterForGetLinkAndEdit: Boolean = false,
        val showDownload: Boolean = true,
        val showShare: Boolean = true,
        val transferHandler: TransferHandler,
        /** True when opened from Shared folder (incoming/outgoing shares or links). When true, save creates a new file with (1)(2) naming; when false, Edit mode overwrites the same file. Derived from nodeSourceType by the caller. */
        val isFromSharedFolder: Boolean = false,
        /** Reserved for send-to-chat flow (e.g. when opening editor from chat). */
        val chatId: Long? = null,
        /** Reserved for send-to-chat flow (e.g. when opening editor from chat). */
        val messageId: Long? = null,
        val localPath: String? = null,
    )

    /**
     * Switches to View mode. When [discardChanges] is true (discard flow), restores content from
     * [lastSavedContent] and syncs internal line buffer. Heavy work runs on [defaultDispatcher].
     */
    fun setViewMode(discardChanges: Boolean = false) {
        if (!discardChanges) {
            _uiState.update {
                it.copy(mode = TextEditorMode.View, showDiscardDialog = false)
            }
            return
        }
        val savedContent = lastSavedContent
        _uiState.update {
            it.copy(showDiscardDialog = false, isRestoringContent = true)
        }
        viewModelScope.launch {
            val (lines, displayedContent, cap, hasMoreLines, totalLinesLoaded) = withContext(
                defaultDispatcher
            ) {
                val l = savedContent.split("\n")
                val c = INITIAL_DISPLAY_LINES.coerceAtMost(l.size)
                RestoreResult(
                    lines = l,
                    displayedContent = l.take(c).joinToString("\n"),
                    displayLineCap = c,
                    hasMoreLines = l.size > c,
                    totalLinesLoaded = l.size,
                )
            }
            fullContentLines.clear()
            fullContentLines.addAll(lines)
            displayLineCap = cap
            cachedCleanContent = displayedContent
            _uiState.update {
                it.copy(
                    mode = TextEditorMode.View,
                    content = displayedContent,
                    hasMoreLines = hasMoreLines,
                    totalLinesLoaded = totalLinesLoaded,
                    totalLineCount = fullContentLines.size,
                    isRestoringContent = false,
                )
            }
        }
    }

    /** Number of chunks for view-mode virtualised list (fixed [CHUNK_SIZE] lines per chunk). */
    fun getChunkCount(): Int = Math.ceilDiv(fullContentLines.size, CHUNK_SIZE)

    /** Text for chunk [chunkIndex] in view mode (read-only slice of [fullContentLines]). */
    fun getChunkText(chunkIndex: Int): String {
        val start = chunkIndex * CHUNK_SIZE
        val end = (start + CHUNK_SIZE).coerceAtMost(fullContentLines.size)
        if (start >= fullContentLines.size) return ""
        return fullContentLines.subList(start, end).joinToString("\n")
    }

    /** 1-based starting line number for chunk [chunkIndex] in view mode. */
    fun getChunkStartLine(chunkIndex: Int): Int = chunkIndex * CHUNK_SIZE + 1

    /** Shows the discard-changes confirmation dialog (Edit mode, unsaved). */
    fun requestShowDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = true) }
    }

    /** User confirmed Discard: revert to last saved content and switch to View mode. */
    fun confirmDiscard() {
        setViewMode(discardChanges = true)
    }

    /** User dismissed the discard dialog (Cancel). */
    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun setEditMode() {
        _uiState.update {
            it.copy(mode = TextEditorMode.Edit)
        }
    }

    /**
     * Returns true when [currentText] (from the text field) differs from [cachedCleanContent].
     * O(1) lookup — no string concatenation on the main thread.
     */
    fun isContentDirty(currentText: String): Boolean =
        currentText != cachedCleanContent

    /** Clears the one-shot suffix appended during edit-mode load-more. */
    fun consumeAppendSuffix() {
        _uiState.update { it.copy(appendSuffix = null) }
    }

    /**
     * Expands the displayed content by [LINES_TO_ADD_ON_LOAD_MORE] lines when content was capped.
     * In Edit mode emits a suffix via [TextEditorComposeUiState.appendSuffix] so the composable
     * appends it directly to the text field without needing the user's current text.
     * In View mode rebuilds the full displayed content from [fullContentLines].
     */
    fun onLoadMoreLines() {
        if (fullContentLines.size <= displayLineCap) return
        val oldCap = displayLineCap
        displayLineCap =
            (displayLineCap + LINES_TO_ADD_ON_LOAD_MORE).coerceAtMost(fullContentLines.size)
        viewModelScope.launch {
            val hasMoreLines = fullContentLines.size > displayLineCap
            val totalLinesLoaded = fullContentLines.size
            if (_uiState.value.mode == TextEditorMode.Edit) {
                val (suffix, cleanContent) = withContext(defaultDispatcher) {
                    val s =
                        "\n" + fullContentLines.subList(oldCap, displayLineCap).joinToString("\n")
                    s to fullContentLines.take(displayLineCap).joinToString("\n")
                }
                cachedCleanContent = cleanContent
                _uiState.update {
                    it.copy(
                        appendSuffix = suffix,
                        hasMoreLines = hasMoreLines,
                        totalLinesLoaded = totalLinesLoaded,
                    )
                }
            } else {
                val content = withContext(defaultDispatcher) {
                    fullContentLines.take(displayLineCap).joinToString("\n")
                }
                cachedCleanContent = content
                _uiState.update {
                    it.copy(
                        content = content,
                        hasMoreLines = hasMoreLines,
                        totalLinesLoaded = totalLinesLoaded,
                    )
                }
            }
        }
    }

    /**
     * Saves text content. [currentText] is the text currently in the editor (read from the text
     * field at save time). When content was capped for display (gradual load), merges the
     * user-edited visible portion with the original non-displayed tail.
     * In Create mode [fullContentLines] is empty, so [currentText] is saved as-is.
     * Save/upload behaviour uses [Args.isFromSharedFolder] only; fromHome is not used in this flow.
     */
    fun saveFile(currentText: String) {
        if (_uiState.value.mode == TextEditorMode.View) return
        viewModelScope.launch {
            val state = _uiState.value
            val fullTextToSave = withContext(defaultDispatcher) {
                if (fullContentLines.isNotEmpty()) {
                    val editedLines = currentText.split("\n")
                    (editedLines + fullContentLines.drop(displayLineCap)).joinToString("\n")
                } else {
                    currentText
                }
            }
            runCatching {
                saveTextContentForTextEditorUseCase(
                    nodeHandle = args.nodeHandle,
                    text = fullTextToSave,
                    fileName = state.fileName.ifEmpty { "untitled.txt" },
                    mode = state.mode,
                    fromHome = false, // Compose flow uses isFromSharedFolder only
                    isFromSharedFolder = args.isFromSharedFolder,
                )
            }.fold(
                onSuccess = { saveResult ->
                    when (saveResult) {
                        is TextEditorSaveResult.UploadRequired -> {
                            lastSavedContent = fullTextToSave
                            val lines = fullTextToSave.split("\n")
                            fullContentLines.clear()
                            fullContentLines.addAll(lines)
                            displayLineCap = lines.size
                            cachedCleanContent = fullTextToSave
                            _uiState.update {
                                it.copy(
                                    content = fullTextToSave,
                                    mode = TextEditorMode.View,
                                    hasMoreLines = false,
                                    totalLinesLoaded = lines.size,
                                    totalLineCount = lines.size,
                                    transferEvent = triggered(
                                        TransferTriggerEvent.StartUpload.TextFile(
                                            path = saveResult.tempPath,
                                            destinationId = NodeId(saveResult.parentHandle),
                                            isEditMode = saveResult.isEditMode,
                                            fromHomePage = saveResult.fromHome,
                                        )
                                    ),
                                    saveSuccessEvent = triggered,
                                )
                            }
                        }
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Text editor: failed to save content")
                    _uiState.update {
                        it.copy(
                            errorEvent = triggered,
                            errorMessage = e.message?.ifBlank { null },
                        )
                    }
                },
            )
        }
    }

    fun consumeErrorEvent() {
        _uiState.update { it.copy(errorEvent = consumed, errorMessage = null) }
    }

    fun consumeTransferEvent() {
        _uiState.update { it.copy(transferEvent = consumed()) }
    }

    fun consumeSaveSuccessEvent() {
        _uiState.update { it.copy(saveSuccessEvent = consumed) }
    }

    /**
     * Handles top bar action intents from the UI (MVI pattern).
     */
    fun onMenuAction(action: TextEditorTopBarAction) {
        when (action) {
            TextEditorTopBarAction.LineNumbers -> {
                _uiState.update {
                    it.copy(showLineNumbers = !it.showLineNumbers)
                }
            }
            // TODO: Wire Download, GetLink, SendToChat, Share when top-bar variants are needed (currently bottom-bar only)
            TextEditorTopBarAction.Download,
            TextEditorTopBarAction.GetLink,
            TextEditorTopBarAction.SendToChat,
            TextEditorTopBarAction.Share,
            TextEditorTopBarAction.Save,
            TextEditorTopBarAction.More,
                -> Unit
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

    private data class RestoreResult(
        val lines: List<String>,
        val displayedContent: String,
        val displayLineCap: Int,
        val hasMoreLines: Boolean,
        val totalLinesLoaded: Int,
    )

}
