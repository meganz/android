package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.qualifier.DefaultDispatcher
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

/** Number of lines per chunk in both view and edit modes.
 * Unified so that item indices match 1:1 across mode switches, eliminating scroll jumps. */
internal const val CHUNK_SIZE = 200

/** Chunk size for gradual file read; balances responsiveness and I/O overhead. */
private const val CHUNK_SIZE_LINES = 500

/**
 * ViewModel for the Compose text editor screen.
 *
 * The full document is stored as [fullContentLines] during loading and view mode.
 * In edit mode, the document is split into [chunkTexts] (one entry per [CHUNK_SIZE] lines).
 * Each visible chunk gets its own [TextFieldState] held in [chunkStates];
 * only the focused one has the cursor. When a chunk scrolls off-screen its edits
 * are flushed back to [chunkTexts] via [disposeChunkState].
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

    /** Per-line list used during loading and in View mode. */
    private val fullContentLines = mutableListOf<String>()

    /** Per-chunk text list used in Edit/Create mode. Variable-size chunks. */
    private val chunkTexts = mutableListOf<String>()

    /** Active TextFieldStates for visible chunks (created lazily, flushed on dispose). */
    private val chunkStates = mutableMapOf<Int, TextFieldState>()

    /** Original text per chunk at the moment its TextFieldState was created. */
    private val chunkOriginals = mutableMapOf<Int, String>()

    /** Set to true when a disposed chunk had edits. */
    private var hasDisposedEdits: Boolean = false

    /** Cached cumulative start-line for each chunk; rebuilt on content/mode changes. */
    private var cachedStartLines: IntArray = IntArray(0)

    /** Content at last load or last successful save; used for discard. */
    private var lastSavedContent: String = ""

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
                        if (_uiState.value.isLoading) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorEvent = consumed,
                                    totalLineCount = fullContentLines.size,
                                )
                            }
                        } else {
                            _uiState.update { it.copy(totalLineCount = fullContentLines.size) }
                        }
                    }
                lastSavedContent = withContext(defaultDispatcher) {
                    fullContentLines.joinToString("\n")
                }
                if (args.mode == TextEditorMode.Edit) {
                    buildChunksFromLines()
                }
                rebuildStartLineCache()
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
        } else {
            lastSavedContent = ""
            chunkTexts.add("")
            rebuildStartLineCache()
            _uiState.update {
                it.copy(isLoading = false, totalLineCount = 0)
            }
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
        val isFromSharedFolder: Boolean = false,
        val chatId: Long? = null,
        val messageId: Long? = null,
        val localPath: String? = null,
    )

    /** Total number of chunks in the current mode. */
    fun getChunkCount(): Int {
        return if (isEditMode()) {
            chunkTexts.size.coerceAtLeast(1)
        } else {
            ceilDiv(fullContentLines.size, CHUNK_SIZE)
        }
    }

    /** Returns the text for a read-only chunk (View mode — fixed CHUNK_SIZE slices). */
    fun getChunkText(chunkIndex: Int): String {
        val start = chunkIndex * CHUNK_SIZE
        val end = (start + CHUNK_SIZE).coerceAtMost(fullContentLines.size)
        if (start >= fullContentLines.size) return ""
        return fullContentLines.subList(start, end).joinToString("\n")
    }

    /** Updates which chunk is the editing focus. Only the focused chunk ±1 are editable. */
    fun setFocusedEditChunk(chunkIndex: Int) {
        _uiState.update { it.copy(focusedEditChunk = chunkIndex) }
    }

    /** Returns the cached starting line number (1-based) for a chunk. O(1) per call. */
    fun getChunkStartLine(chunkIndex: Int): Int =
        cachedStartLines.getOrElse(chunkIndex) { 1 }

    /**
     * Returns or lazily creates a [TextFieldState] for [chunkIndex].
     * Called by the UI layer when a chunk composable enters composition.
     */
    fun getOrCreateChunkState(chunkIndex: Int): TextFieldState {
        return chunkStates.getOrPut(chunkIndex) {
            val text = chunkTexts.getOrElse(chunkIndex) { "" }
            chunkOriginals[chunkIndex] = text
            TextFieldState(text)
        }
    }

    /**
     * Flushes the content of a chunk's [TextFieldState] back to [chunkTexts]
     * and releases the state. Called when a chunk composable leaves composition.
     */
    fun disposeChunkState(chunkIndex: Int) {
        val state = chunkStates.remove(chunkIndex) ?: return
        val currentText = state.text.toString()
        val original = chunkOriginals.remove(chunkIndex)
        if (currentText != original && chunkIndex < chunkTexts.size) {
            chunkTexts[chunkIndex] = currentText
            hasDisposedEdits = true
        }
    }

    /**
     * Flushes ALL active chunk states back to [chunkTexts] and clears them.
     * After this call, [chunkTexts] is the single source of truth for all chunk content.
     */
    private fun flushAllActiveChunks() {
        chunkStates.forEach { (idx, state) ->
            if (idx < chunkTexts.size) {
                chunkTexts[idx] = state.text.toString()
            }
        }
        chunkStates.clear()
        chunkOriginals.clear()
    }

    /** Rebuilds [fullContentLines] from [chunkTexts]. */
    private fun rebuildLinesFromChunks() {
        fullContentLines.clear()
        chunkTexts.forEach { chunkText ->
            fullContentLines.addAll(chunkText.split("\n"))
        }
    }

    /**
     * Returns true when the editor has unsaved changes.
     */
    fun isContentDirty(): Boolean {
        if (hasDisposedEdits) return true
        return chunkStates.any { (idx, state) ->
            state.text.toString() != chunkOriginals[idx]
        }
    }

    /** Switches to Edit mode, building per-chunk text slices from the loaded content. */
    fun setEditMode(focusedChunkIndex: Int = 0) {
        buildChunksFromLines()
        chunkStates.clear()
        chunkOriginals.clear()
        hasDisposedEdits = false
        rebuildStartLineCache()
        val initialChunk = if (chunkTexts.isNotEmpty())
            focusedChunkIndex.coerceIn(0, chunkTexts.size - 1)
        else 0
        _uiState.update {
            it.copy(
                mode = TextEditorMode.Edit,
                totalLineCount = fullContentLines.size,
                contentVersion = it.contentVersion + 1,
                focusedEditChunk = initialChunk,
            )
        }
    }

    /**
     * Switches to View mode.
     * When [discardChanges] is true, restores content from [lastSavedContent] asynchronously.
     */
    fun setViewMode(discardChanges: Boolean = false) {
        if (discardChanges) {
            _uiState.update { it.copy(showDiscardDialog = false, isRestoringContent = true) }
            viewModelScope.launch {
                val lines = withContext(defaultDispatcher) { lastSavedContent.split("\n") }
                fullContentLines.clear()
                fullContentLines.addAll(lines)
                clearEditState()
                _uiState.update {
                    it.copy(
                        mode = TextEditorMode.View,
                        isRestoringContent = false,
                        totalLineCount = fullContentLines.size,
                        contentVersion = it.contentVersion + 1,
                    )
                }
                rebuildStartLineCache()
            }
            return
        }
        flushAllActiveChunks()
        rebuildLinesFromChunks()
        clearEditState()
        _uiState.update {
            it.copy(
                mode = TextEditorMode.View,
                showDiscardDialog = false,
                totalLineCount = fullContentLines.size,
                contentVersion = it.contentVersion + 1,
            )
        }
        rebuildStartLineCache()
    }

    /** Shows the discard-changes confirmation dialog. */
    fun requestShowDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = true) }
    }

    /** User confirmed discard: reverts content and switches to View mode. */
    fun confirmDiscard() {
        setViewMode(discardChanges = true)
    }

    /** User dismissed the discard dialog. */
    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    /** Flushes all chunk edits and persists the full document. No-op in View mode. */
    fun saveFile() {
        if (_uiState.value.mode == TextEditorMode.View) return
        flushAllActiveChunks()
        rebuildLinesFromChunks()
        val snapshot = fullContentLines.toList()
        viewModelScope.launch {
            val state = _uiState.value
            val fullTextToSave = withContext(defaultDispatcher) {
                snapshot.joinToString("\n")
            }
            runCatching {
                saveTextContentForTextEditorUseCase(
                    nodeHandle = args.nodeHandle,
                    text = fullTextToSave,
                    fileName = state.fileName.ifEmpty { "untitled.txt" },
                    mode = state.mode,
                    fromHome = false,
                    isFromSharedFolder = args.isFromSharedFolder,
                )
            }.fold(
                onSuccess = { saveResult ->
                    when (saveResult) {
                        is TextEditorSaveResult.UploadRequired -> {
                            lastSavedContent = fullTextToSave
                            _uiState.update {
                                it.copy(
                                    mode = TextEditorMode.View,
                                    totalLineCount = fullContentLines.size,
                                    contentVersion = it.contentVersion + 1,
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
                            clearEditState()
                            rebuildStartLineCache()
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

    fun onMenuAction(action: TextEditorTopBarAction) {
        when (action) {
            TextEditorTopBarAction.LineNumbers -> {
                _uiState.update { it.copy(showLineNumbers = !it.showLineNumbers) }
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
            is TextEditorBottomBarAction.SendToChat -> {}
        }
    }

    private fun isEditMode(): Boolean {
        val mode = _uiState.value.mode
        return mode == TextEditorMode.Edit || mode == TextEditorMode.Create
    }

    private fun buildChunksFromLines() {
        chunkTexts.clear()
        for (i in fullContentLines.indices step CHUNK_SIZE) {
            val end = (i + CHUNK_SIZE).coerceAtMost(fullContentLines.size)
            chunkTexts.add(fullContentLines.subList(i, end).joinToString("\n"))
        }
        if (chunkTexts.isEmpty()) chunkTexts.add("")
    }

    private fun clearEditState() {
        chunkStates.clear()
        chunkOriginals.clear()
        chunkTexts.clear()
        hasDisposedEdits = false
    }

    private fun rebuildStartLineCache() {
        val count = getChunkCount()
        cachedStartLines = IntArray(count)
        if (isEditMode()) {
            var line = 1
            for (i in 0 until count) {
                cachedStartLines[i] = line
                val text = chunkStates[i]?.text?.toString()
                    ?: chunkTexts.getOrElse(i) { "" }
                line += text.count { it == '\n' } + 1
            }
        } else {
            for (i in 0 until count) {
                cachedStartLines[i] = i * CHUNK_SIZE + 1
            }
        }
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
}
