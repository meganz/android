package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.chat.SendToChatResult
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent.StartDownloadNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.usecase.continuewhereleftoff.GetTextEditorScrollUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveTextEditorScrollUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.node.publiclink.MapTypedNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.texteditor.GetShowLineNumbersPreferenceUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForFileLinkUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForFolderLinkUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SetShowLineNumbersPreferenceUseCase
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorComposeUiState
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeEffect
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/** Number of lines per chunk in both view and edit modes.
 *  Sized for a good balance between selection range and memory/layout cost. */
internal const val CHUNK_SIZE = 1000

/** Maximum characters per chunk. Prevents ANRs caused by native text measurement
 *  (`MeasuredText.nBuildMeasuredText`) blocking the main thread when a chunk contains
 *  very long lines (e.g. minified JSON, base64 blobs). */
internal const val CHUNK_MAX_CHARS = 50_000

/**
 * A chunk boundary pointing to a position in [TextEditorComposeViewModel.fullContentLines].
 * @param lineIndex Index of the line in [TextEditorComposeViewModel.fullContentLines].
 * @param charOffset Character offset within that line (0 = start of line).
 */
internal data class ChunkBoundary(val lineIndex: Int, val charOffset: Int = 0)

/** Same value as [nz.mega.sdk.MegaApiJava.INVALID_HANDLE]; avoids SDK dependency in the feature module. */
private const val INVALID_NODE_HANDLE = -1L

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
    private val getTextContentForFileLinkUseCase: GetTextContentForFileLinkUseCase,
    private val getTextContentForFolderLinkUseCase: GetTextContentForFolderLinkUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase,
    private val getShowLineNumbersPreferenceUseCase: GetShowLineNumbersPreferenceUseCase,
    private val setShowLineNumbersPreferenceUseCase: SetShowLineNumbersPreferenceUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
    private val textEditorBottomBarActionsMapper: TextEditorBottomBarActionsMapper,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val mapTypedNodeToPublicLinkUseCase: MapTypedNodeToPublicLinkUseCase,
    private val saveTextEditorScrollUseCase: SaveTextEditorScrollUseCase,
    private val getTextEditorScrollUseCase: GetTextEditorScrollUseCase,
    private val saveRecentlyUsedItemUseCase: SaveRecentlyUsedItemUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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

    /** Saved cursor/selection per chunk, captured on dispose and restored on recreation (e.g. rotation). */
    private val chunkSelections = mutableMapOf<Int, TextRange>()

    /** Set to true when a disposed chunk had edits. */
    private var hasDisposedEdits: Boolean = false

    /** Cached cumulative start-line for each chunk; rebuilt on content/mode changes. */
    private var cachedStartLines: IntArray = IntArray(0)

    /** Precomputed chunk boundaries: each entry points to a position in [fullContentLines].
     *  Used by both view mode (for on-demand text slicing) and edit mode (to build
     *  [chunkTexts]). Rebuilt on content/mode changes, before triggering recomposition. */
    @Volatile
    private var chunkBoundaries: List<ChunkBoundary> = emptyList()

    /** Content at last load or last successful save; used for discard. */
    private var lastSavedContent: String = ""

    /** Resolved node handle; updated from chat file resolution when opening from chat. */
    private var resolvedNodeHandle: Long = args.nodeHandle

    /** Resolved public node for file link; used for download. */
    private var resolvedPublicNode: TypedFileNode? = null

    /** Last known scroll position reported by the UI, used for persistence. */
    private var lastScrollFraction: Float = 0f
    private var lastScrollOffset: Int = 0

    /** Whether long-line chunking (AND-23707) is enabled. Resolved once during init. */
    @Volatile
    private var longLineChunkingEnabled: Boolean = true

    /** Active content-load job; cancelled if connectivity drops while loading. */
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = runCatching { getShowLineNumbersPreferenceUseCase() }.getOrDefault(false)
            _uiState.update { it.copy(showLineNumbers = saved) }
        }
        monitorNodeRename()
        if (args.mode != TextEditorMode.Create) {
            loadJob = viewModelScope.launch {
                // No localPath means the editor must reach the network (Cloud Drive open,
                // chat-attached file, or public file link). Short-circuit with a no-internet
                // prompt to avoid kicking off a download that would hang forever.
                if (args.localPath.isNullOrBlank() && !isConnectedToInternetUseCase()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorEvent = triggered,
                            isNoInternetError = true,
                        )
                    }
                    return@launch
                }
                longLineChunkingEnabled = runCatching {
                    getFeatureFlagValueUseCase(ApiFeatures.TextEditorLongLineChunking)
                }.getOrDefault(true)
                _uiState.update { it.copy(isFullyLoaded = false) }
                val chatId = args.chatId
                val messageId = args.messageId
                val publicUrl = args.publicUrl
                var resolvedNode: TypedFileNode? = null
                if (chatId != null && messageId != null) {
                    val chatFileResult = runCatching {
                        getChatFileUseCase(chatId, messageId)
                    }
                    val chatFile = chatFileResult.getOrNull()
                    if (chatFile != null) {
                        resolvedNodeHandle = chatFile.id.longValue
                        resolvedNode = chatFile
                        _uiState.update {
                            it.copy(fileName = chatFile.name)
                        }
                    } else {
                        val exception = chatFileResult.exceptionOrNull()
                        Timber.e(exception, "Text editor: chat file not found for chatId=$chatId, messageId=$messageId")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorEvent = triggered,
                                errorMessage = exception?.message?.ifBlank { null },
                            )
                        }
                        return@launch
                    }
                } else if (!publicUrl.isNullOrBlank()) {
                    val publicNodeResult = runCatching {
                        getPublicNodeUseCase(publicUrl)
                    }
                    val publicNode = publicNodeResult.getOrNull()
                    if (publicNode != null) {
                        resolvedNodeHandle = publicNode.id.longValue
                        resolvedNode = publicNode
                        resolvedPublicNode = publicNode
                        _uiState.update {
                            it.copy(fileName = publicNode.name)
                        }
                    } else {
                        val exception = publicNodeResult.exceptionOrNull()
                        Timber.e(exception, "Text editor: failed to resolve public node from file link")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorEvent = triggered,
                                errorMessage = exception?.message?.ifBlank { null },
                            )
                        }
                        return@launch
                    }
                } else if (args.isFolderLink) {
                    // Folder link nodes live in the folder API session, not the main account, so
                    // they must be resolved as public child nodes; resolving by handle through the
                    // main API would fail and the file would never load.
                    val folderNodeResult = runCatching {
                        getPublicChildNodeFromIdUseCase(NodeId(resolvedNodeHandle)) as? TypedFileNode
                    }
                    val folderNode = folderNodeResult.getOrNull()
                    if (folderNode != null) {
                        resolvedNodeHandle = folderNode.id.longValue
                        resolvedNode = folderNode
                        resolvedPublicNode = folderNode
                        _uiState.update {
                            it.copy(fileName = folderNode.name)
                        }
                    } else {
                        val exception = folderNodeResult.exceptionOrNull()
                        Timber.e(exception, "Text editor: failed to resolve folder link node")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorEvent = triggered,
                                errorMessage = exception?.message?.ifBlank { null },
                            )
                        }
                        return@launch
                    }
                }
                val contentFlow = when {
                    !publicUrl.isNullOrBlank() -> getTextContentForFileLinkUseCase(
                        urlFileLink = publicUrl,
                        chunkSizeLines = CHUNK_SIZE_LINES,
                    )

                    args.isFolderLink && resolvedNode != null -> getTextContentForFolderLinkUseCase(
                        node = resolvedNode,
                        chunkSizeLines = CHUNK_SIZE_LINES,
                    )

                    resolvedNode != null -> getTextContentForTextEditorUseCase(
                        resolvedNode = resolvedNode,
                        localPath = args.localPath,
                        chunkSizeLines = CHUNK_SIZE_LINES,
                    )

                    else -> getTextContentForTextEditorUseCase(
                        nodeHandle = resolvedNodeHandle,
                        localPath = args.localPath,
                        chunkSizeLines = CHUNK_SIZE_LINES,
                    )
                }
                contentFlow
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
                        if (args.mode != TextEditorMode.Edit && longLineChunkingEnabled) {
                            buildChunkBoundaries()
                        }
                        // In Edit mode keep isLoading=true until buildChunksFromLines() is
                        // called below; showing the edit UI with empty chunks would cause a
                        // blank screen while the rest of the file is still streaming in.
                        if (_uiState.value.isLoading && args.mode != TextEditorMode.Edit) {
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
                val linesSnapshot = fullContentLines.toList()
                lastSavedContent = withContext(defaultDispatcher) {
                    linesSnapshot.joinToString("\n")
                }
                if (args.mode == TextEditorMode.Edit) {
                    buildChunksFromLines()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorEvent = consumed,
                            totalLineCount = fullContentLines.size,
                            contentVersion = it.contentVersion + 1,
                        )
                    }
                }
                rebuildStartLineCache()
                _uiState.update { it.copy(isFullyLoaded = true) }
                restoreScrollPosition()
                saveRecentlyUsed()
                fetchBottomBarActions(resolvedNode)
            }
            monitorConnectivityDuringLoad()
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
        val showSendToChat: Boolean = false,
        val isFromSharedFolder: Boolean = false,
        /** When true (e.g. new file from Home), forwarded to upload as [TransferTriggerEvent.StartUpload.TextFile.fromHomePage]. */
        val fromHome: Boolean = false,
        val chatId: Long? = null,
        val messageId: Long? = null,
        val localPath: String? = null,
        /** Public file link URL; when set the editor resolves the node from this URL. */
        val publicUrl: String? = null,
        /** True when opened from a folder link; the node is resolved and streamed via the folder API. */
        val isFolderLink: Boolean = false,
    )

    /** Total number of chunks in the current mode. */
    fun getChunkCount(): Int {
        return if (isEditMode()) {
            chunkTexts.size.coerceAtLeast(1)
        } else if (longLineChunkingEnabled) {
            chunkBoundaries.size.coerceAtLeast(if (fullContentLines.isEmpty()) 0 else 1)
        } else {
            ceilDiv(fullContentLines.size, CHUNK_SIZE)
        }
    }

    /** Returns the text for a read-only chunk (View mode). */
    fun getChunkText(chunkIndex: Int): String {
        if (!longLineChunkingEnabled) {
            val start = chunkIndex * CHUNK_SIZE
            val end = (start + CHUNK_SIZE).coerceAtMost(fullContentLines.size)
            if (start >= fullContentLines.size) return ""
            return fullContentLines.subList(start, end).joinToString("\n")
        }
        if (chunkIndex >= chunkBoundaries.size) return ""
        val start = chunkBoundaries[chunkIndex]
        val endBoundary = if (chunkIndex + 1 < chunkBoundaries.size) {
            chunkBoundaries[chunkIndex + 1]
        } else {
            ChunkBoundary(fullContentLines.size, 0)
        }
        if (start.lineIndex >= fullContentLines.size) return ""
        return extractChunkText(start, endBoundary)
    }

    /**
     * Extracts the text between two [ChunkBoundary] positions.
     * Handles mid-line offsets for boundaries that split a long line.
     */
    private fun extractChunkText(start: ChunkBoundary, end: ChunkBoundary): String = buildString {
        // Last line to include: end.charOffset > 0 means we partially include end.lineIndex,
        // otherwise we stop before it.
        val lastLine = if (end.charOffset > 0) end.lineIndex else end.lineIndex - 1

        for (i in start.lineIndex..lastLine.coerceAtMost(fullContentLines.lastIndex)) {
            val line = fullContentLines[i]
            val from = if (i == start.lineIndex) start.charOffset else 0
            val to = if (i == end.lineIndex && end.charOffset > 0) {
                end.charOffset.coerceAtMost(line.length)
            } else {
                line.length
            }
            if (i > start.lineIndex) append('\n')
            append(line, from, to)
        }
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
            val savedSelection = chunkSelections.remove(chunkIndex)
            val initialSelection = if (savedSelection != null) {
                TextRange(
                    savedSelection.start.coerceIn(0, text.length),
                    savedSelection.end.coerceIn(0, text.length),
                )
            } else {
                TextRange(text.length)
            }
            TextFieldState(text, initialSelection = initialSelection)
        }
    }

    /**
     * Flushes the content of a chunk's [TextFieldState] back to [chunkTexts]
     * and releases the state. Called when a chunk composable leaves composition.
     */
    fun disposeChunkState(chunkIndex: Int) {
        val state = chunkStates.remove(chunkIndex) ?: return
        chunkSelections[chunkIndex] = state.selection
        if (chunkIndex == _uiState.value.focusedEditChunk) {
            _uiState.update { it.copy(restoreFocusChunkIndex = chunkIndex) }
        }
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

    /** Rebuilds [fullContentLines] from [chunkTexts].
     *  When chunking is enabled, chunks that start mid-line ([ChunkBoundary.charOffset] > 0)
     *  are concatenated with the previous chunk's last line before splitting by newlines. */
    private fun rebuildLinesFromChunks() {
        fullContentLines.clear()

        if (longLineChunkingEnabled) {
            for (i in chunkTexts.indices) {
                val isMidLine = i < chunkBoundaries.size && chunkBoundaries[i].charOffset > 0
                if (isMidLine && fullContentLines.isNotEmpty()) {
                    fullContentLines[fullContentLines.lastIndex] += chunkTexts[i]
                } else {
                    fullContentLines.addAll(chunkTexts[i].split("\n"))
                }
            }
        } else {
            chunkTexts.forEach { fullContentLines.addAll(it.split("\n")) }
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

    /**
     * When the editor was opened in [TextEditorMode.Edit], leaving edit with no unsaved changes
     * should pop the destination. When opened in [TextEditorMode.View] (then user chose Edit),
     * the same action should return to view mode on this screen instead.
     */
    fun shouldPopDestinationOnCleanEditExit(): Boolean = args.mode == TextEditorMode.Edit

    /** Switches to Edit mode, building per-chunk text slices from the loaded content. */
    fun setEditMode(focusedChunkIndex: Int = 0) {
        buildChunksFromLines()
        chunkStates.clear()
        chunkOriginals.clear()
        chunkSelections.clear()
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

    /**
     * Handles close action from the UI (back press or close button in edit/create mode).
     * If content is dirty, shows the discard dialog. Otherwise, either pops back
     * (Create mode, or Edit mode opened as Edit) or switches to View mode.
     */
    fun handleClose() {
        if (isContentDirty()) {
            requestShowDiscardDialog()
        } else {
            val mode = _uiState.value.mode
            when {
                mode == TextEditorMode.Create -> emitCloseEvent()
                mode == TextEditorMode.Edit && shouldPopDestinationOnCleanEditExit() -> emitCloseEvent()
                mode == TextEditorMode.Edit -> setViewMode()
                else -> emitCloseEvent()
            }
        }
    }

    private fun emitCloseEvent() {
        viewModelScope.launch {
            // Order matters: saveRecentlyUsed() must run before saveScrollState() because
            // the scroll table has a FK to recently_used, and the REPLACE strategy on
            // recently_used cascade-deletes the scroll row before re-inserting.
            saveRecentlyUsed()
            saveScrollState()
            _uiState.update { it.copy(closeEvent = triggered) }
        }
    }

    fun consumeCloseEvent() {
        _uiState.update { it.copy(closeEvent = consumed) }
    }

    /** Shows the discard-changes confirmation dialog. */
    fun requestShowDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = true) }
    }

    /**
     * User confirmed discard: in Create mode, emits [TextEditorComposeUiState.exitAfterCreateDiscardEvent]
     * so the UI pops without saving; in Edit mode, reverts content and switches to View mode.
     */
    fun confirmDiscard() {
        if (_uiState.value.mode == TextEditorMode.Create) {
            _uiState.update {
                it.copy(
                    showDiscardDialog = false,
                    exitAfterCreateDiscardEvent = triggered,
                )
            }
        } else {
            setViewMode(discardChanges = true)
        }
    }

    fun consumeExitAfterCreateDiscardEvent() {
        _uiState.update { it.copy(exitAfterCreateDiscardEvent = consumed) }
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
            val wasCreateMode = state.mode == TextEditorMode.Create
            runCatching {
                saveTextContentForTextEditorUseCase(
                    nodeHandle = resolvedNodeHandle,
                    text = fullTextToSave,
                    fileName = state.fileName.ifEmpty { "untitled.txt" },
                    mode = state.mode,
                    fromHome = args.fromHome,
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
                                )
                            }
                            clearEditState()
                            rebuildStartLineCache()
                            if (!wasCreateMode) {
                                snackbarEventQueue.queueMessage(sharedR.string.general_changes_saved)
                            }
                            emitCloseEvent()
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
        _uiState.update {
            it.copy(
                errorEvent = consumed,
                errorMessage = null,
                isNoInternetError = false,
            )
        }
    }

    fun consumeTransferEvent() {
        _uiState.update { it.copy(transferEvent = consumed()) }
    }

    fun consumeNodeEffectEvent() {
        _uiState.update { it.copy(nodeEffectEvent = consumed()) }
    }

    fun consumeShareErrorEvent() {
        _uiState.update { it.copy(shareErrorEvent = consumed) }
    }

    fun consumeSendToChatErrorEvent() {
        _uiState.update { it.copy(sendToChatErrorEvent = consumed) }
    }

    fun onMenuAction(action: TextEditorTopBarAction) {
        when (action) {
            TextEditorTopBarAction.LineNumbers -> {
                val newValue = !_uiState.value.showLineNumbers
                _uiState.update { it.copy(showLineNumbers = newValue) }
                viewModelScope.launch {
                    runCatching { setShowLineNumbersPreferenceUseCase(newValue) }
                        .onFailure { Timber.e(it, "Failed to persist show line numbers preference") }
                }
            }

            TextEditorTopBarAction.Download -> emitDownloadTransferEvent()

            TextEditorTopBarAction.GetLink -> emitManageLinkEffect()
            TextEditorTopBarAction.Share -> emitShareEffect()
            TextEditorTopBarAction.SendToChat -> emitSendToChatEffect()

            TextEditorTopBarAction.Save,
            TextEditorTopBarAction.More,
                -> Unit
        }
    }

    fun onBottomBarAction(action: TextEditorBottomBarAction) {
        when (action) {
            is TextEditorBottomBarAction.Download -> emitDownloadTransferEvent()

            is TextEditorBottomBarAction.GetLink -> emitManageLinkEffect()
            is TextEditorBottomBarAction.Share -> emitShareEffect()
            is TextEditorBottomBarAction.SendToChat -> emitSendToChatEffect()
            is TextEditorBottomBarAction.Edit -> setEditMode()
        }
    }

    private fun emitDownloadTransferEvent() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        viewModelScope.launch {
            val publicNode = resolvedPublicNode
            if (publicNode != null) {
                // Folder-link nodes are already PublicLinkNodes; only file-link nodes (plain
                // TypedFileNode) need mapping. Avoids re-wrapping an already-public node.
                val downloadNode = if (publicNode is PublicLinkNode) {
                    publicNode
                } else {
                    runCatching {
                        mapTypedNodeToPublicLinkUseCase(publicNode)
                    }.onFailure {
                        Timber.e(it, "Text editor: failed to map public node for download")
                    }.getOrDefault(publicNode)
                }
                _uiState.update {
                    it.copy(
                        transferEvent = triggered(
                            StartDownloadNode(
                                nodes = listOf(downloadNode),
                                withStartMessage = true,
                            )
                        )
                    )
                }
                return@launch
            }
            val node = getNodeByIdUseCase(NodeId(resolvedNodeHandle))
            if (node != null) {
                _uiState.update {
                    it.copy(
                        transferEvent = triggered(
                            StartDownloadNode(nodes = listOf(node), withStartMessage = true)
                        )
                    )
                }
            } else {
                Timber.w("Text editor: node %d not found for download", resolvedNodeHandle)
            }
        }
    }

    private fun emitManageLinkEffect() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        _uiState.update {
            it.copy(nodeEffectEvent = triggered(TextEditorNodeEffect.ManageLink(resolvedNodeHandle)))
        }
    }

    private fun emitShareEffect() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE && args.localPath.isNullOrBlank()) return
        val name = _uiState.value.fileName.ifBlank { args.fileName.orEmpty() }.ifBlank { null }
        if (!args.localPath.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    nodeEffectEvent = triggered(
                        TextEditorNodeEffect.Share(
                            nodeHandle = resolvedNodeHandle,
                            localPath = args.localPath,
                            fileName = name,
                        ),
                    ),
                )
            }
            return
        }
        if (!args.publicUrl.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    nodeEffectEvent = triggered(
                        TextEditorNodeEffect.Share(
                            nodeHandle = resolvedNodeHandle,
                            localPath = null,
                            fileName = name,
                            resolvedPublicLink = args.publicUrl,
                        ),
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                val node = getNodeByIdUseCase(NodeId(resolvedNodeHandle))
                node?.exportedData?.publicLink
                    ?: exportNodeUseCase(
                        nodeToExport = NodeId(resolvedNodeHandle),
                        callerName = "TextEditor:share",
                    )
            }.onSuccess { publicLink ->
                _uiState.update {
                    it.copy(
                        nodeEffectEvent = triggered(
                            TextEditorNodeEffect.Share(
                                nodeHandle = resolvedNodeHandle,
                                localPath = null,
                                fileName = name,
                                resolvedPublicLink = publicLink,
                            ),
                        ),
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Text editor: failed to resolve public link for share")
                _uiState.update { it.copy(shareErrorEvent = triggered) }
            }
        }
    }

    private fun emitSendToChatEffect() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        _uiState.update {
            it.copy(nodeEffectEvent = triggered(TextEditorNodeEffect.SendToChat(resolvedNodeHandle)))
        }
    }

    /**
     * Attaches nodes to chat conversations after the user picks recipients in the send-to-chat
     * picker. Converts user handles to 1-on-1 chat IDs, then attaches via [AttachMultipleNodesUseCase].
     */
    fun attachNodesToChat(result: SendToChatResult) {
        viewModelScope.launch {
            runCatching {
                val chatIdsFromUserHandles = result.userHandles.toList().mapNotNull { userHandle ->
                    runCatching { get1On1ChatIdUseCase(userHandle) }.getOrNull()
                }
                val allChatIds = chatIdsFromUserHandles + result.chatIds.toList()
                attachMultipleNodesUseCase(
                    nodeIds = result.nodeIds.map { NodeId(it) },
                    chatIds = allChatIds,
                )
            }.onFailure { e ->
                Timber.e(e, "Text editor: failed to attach nodes to chat")
                _uiState.update { it.copy(sendToChatErrorEvent = triggered) }
            }
        }
    }

    private fun isEditMode(): Boolean {
        val mode = _uiState.value.mode
        return mode == TextEditorMode.Edit || mode == TextEditorMode.Create
    }

    private fun buildChunksFromLines() {
        chunkTexts.clear()
        if (longLineChunkingEnabled) {
            buildChunkBoundaries()
            for (i in chunkBoundaries.indices) {
                val start = chunkBoundaries[i]
                val end = if (i + 1 < chunkBoundaries.size) {
                    chunkBoundaries[i + 1]
                } else {
                    ChunkBoundary(fullContentLines.size)
                }
                chunkTexts.add(extractChunkText(start, end))
            }
        } else {
            for (i in fullContentLines.indices step CHUNK_SIZE) {
                val end = (i + CHUNK_SIZE).coerceAtMost(fullContentLines.size)
                chunkTexts.add(fullContentLines.subList(i, end).joinToString("\n"))
            }
        }
        if (chunkTexts.isEmpty()) chunkTexts.add("")
    }

    /**
     * Builds [chunkBoundaries] from [fullContentLines].
     * When [longLineChunkingEnabled], caps each chunk at [CHUNK_MAX_CHARS] characters,
     * splitting long lines mid-character when needed.
     * When disabled, uses the original fixed [CHUNK_SIZE] line-count grouping.
     */
    private fun buildChunkBoundaries() {
        if (!longLineChunkingEnabled) {
            chunkBoundaries = (0 until ceilDiv(fullContentLines.size, CHUNK_SIZE))
                .map { ChunkBoundary(it * CHUNK_SIZE) }

            return
        }
        val boundaries = mutableListOf<ChunkBoundary>()
        var lineIdx = 0
        var charOff = 0
        while (lineIdx < fullContentLines.size) {
            boundaries.add(ChunkBoundary(lineIdx, charOff))
            var chunkChars = 0
            while (lineIdx < fullContentLines.size && chunkChars < CHUNK_MAX_CHARS) {
                val line = fullContentLines[lineIdx]
                val remaining = line.length - charOff
                val available = CHUNK_MAX_CHARS - chunkChars
                if (remaining <= available) {
                    // Whole remaining part of this line fits in the chunk
                    chunkChars += remaining + 1 // +1 for \n separator
                    lineIdx++
                    charOff = 0
                } else if (chunkChars == 0) {
                    // First content in chunk but line is too long — take up to limit
                    charOff += available
                    chunkChars += available
                } else {
                    // Line doesn't fit — start a new chunk
                    break
                }
            }
        }
        chunkBoundaries = boundaries
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

    private fun clearEditState() {
        chunkStates.clear()
        chunkOriginals.clear()
        chunkSelections.clear()
        chunkTexts.clear()
        hasDisposedEdits = false
    }

    private fun rebuildStartLineCache() {
        if (!isEditMode() && longLineChunkingEnabled) {
            buildChunkBoundaries()
        }
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
        } else if (longLineChunkingEnabled) {
            for (i in 0 until count) {
                cachedStartLines[i] = chunkBoundaries[i].lineIndex + 1
            }
        } else {
            for (i in 0 until count) {
                cachedStartLines[i] = i * CHUNK_SIZE + 1
            }
        }
    }

    /**
     * Called by the UI to report the current scroll position.
     * [fraction] is the normalised position (0.0–1.0) used to calculate the chunk index.
     * [scrollOffset] is the pixel offset within the first visible chunk, for precise restoration.
     */
    fun updateScrollPosition(fraction: Float, scrollOffset: Int) {
        lastScrollFraction = fraction
        lastScrollOffset = scrollOffset
    }

    private suspend fun saveScrollState() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        runCatching {
            saveTextEditorScrollUseCase(
                TextEditorScroll(
                    nodeHandle = resolvedNodeHandle,
                    cursorPosition = lastScrollOffset,
                    scrollFraction = lastScrollFraction,
                )
            )
        }.onFailure { Timber.e(it, "Failed to save text editor scroll state") }
    }

    private suspend fun restoreScrollPosition() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        runCatching {
            getTextEditorScrollUseCase(resolvedNodeHandle)
        }.onSuccess { scroll ->
            if (scroll != null) {
                val chunkCount = getChunkCount()
                val targetIndex = (scroll.scrollFraction * chunkCount).toInt()
                    .coerceIn(0, (chunkCount - 1).coerceAtLeast(0))
                _uiState.update {
                    it.copy(
                        restoreScrollIndex = targetIndex,
                        restoreScrollOffset = scroll.cursorPosition,
                    )
                }
            }
        }.onFailure { Timber.e(it, "Failed to restore text editor scroll state") }
    }

    /**
     * Called by the UI after the scroll restoration has been applied.
     */
    fun consumeRestoreScrollIndex() {
        _uiState.update { it.copy(restoreScrollIndex = null, restoreScrollOffset = 0) }
    }

    /**
     * Called by the UI after focus has been restored to the chunk.
     */
    fun consumeRestoreFocusChunkIndex() {
        _uiState.update { it.copy(restoreFocusChunkIndex = null) }
    }

    private suspend fun fetchBottomBarActions(resolvedNode: TypedFileNode?) {
        val (nodeName, actions) = runCatching {
            val node = resolvedNode
                ?: getNodeByIdUseCase(NodeId(resolvedNodeHandle))
            val accessPermission = if (resolvedNode != null) null
                else node?.let { getNodeAccessUseCase(NodeId(resolvedNodeHandle)) }
            val isNodeExported = node?.exportedData != null
            val name = node?.name
            name to textEditorBottomBarActionsMapper(
                args.mode,
                accessPermission,
                isNodeExported,
                args.inExcludedAdapterForGetLinkAndEdit,
                args.showDownload,
                args.showShare,
                args.showSendToChat,
            )
        }.getOrElse { null to emptyList() }
        _uiState.update {
            it.copy(
                fileName = nodeName ?: it.fileName,
                bottomBarActions = actions,
            )
        }
    }

    /**
     * Keeps the displayed file name in sync when the open node is renamed elsewhere
     * (Node options bottom sheet, another device). The Compose editor reads the file name
     * once at load, so without this the toolbar title would go stale after a rename.
     */
    private fun monitorNodeRename() {
        monitorNodeUpdatesUseCase()
            .mapNotNull { update ->
                val handle = resolvedNodeHandle
                if (handle == INVALID_NODE_HANDLE) return@mapNotNull null
                update.changes.entries
                    .firstOrNull { (node, changes) ->
                        node.id.longValue == handle && NodeChanges.Name in changes
                    }
                    ?.key?.name
            }
            .onEach { newName -> _uiState.update { it.copy(fileName = newName) } }
            .catch { Timber.e(it, "Text editor: node updates flow failed") }
            .launchIn(viewModelScope)
    }

    /**
     * Cancels the content load and surfaces a no-internet error if connectivity drops
     * while the editor is still loading. The initial offline case is handled by the
     * synchronous [isConnectedToInternetUseCase] check at the top of the load; the
     * `.drop(1)` here keeps this monitor focused on *transitions* (mid-load drops)
     * so it cannot pre-empt a load that is still trying to read a local file.
     */
    private fun monitorConnectivityDuringLoad() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .drop(1)
                .collect { isConnected ->
                    if (isConnected) return@collect
                    if (!_uiState.value.isLoading) return@collect
                    loadJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorEvent = triggered,
                            errorMessage = null,
                            isNoInternetError = true,
                        )
                    }
                }
        }
    }

    private suspend fun saveRecentlyUsed() {
        if (resolvedNodeHandle == INVALID_NODE_HANDLE) return
        val fileName = _uiState.value.fileName
        runCatching {
            saveRecentlyUsedItemUseCase(
                nodeHandle = resolvedNodeHandle,
                type = RecentlyUsedType.TextEditor,
                fileName = fileName,
            )
        }.onFailure { Timber.e(it, "Failed to save recently used item") }
    }
}
