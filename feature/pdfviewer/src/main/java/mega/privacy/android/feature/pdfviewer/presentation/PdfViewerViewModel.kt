package mega.privacy.android.feature.pdfviewer.presentation

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.pdf.GetLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.pdf.SetOrUpdateLastPageViewedInPdfUseCase
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngine
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineFactory
import timber.log.Timber
import java.io.File
import java.net.URL

/**
 * ViewModel for the PDF Viewer screen.
 *
 * Owns a [PdfSearchEngine] (from [PdfSearchEngineFactory]) and manages the complete search pipeline.
 * Search results and highlight rects flow down to the UI via [PdfViewerState];
 * the UI never pushes computed data back up to the ViewModel.
 */
@HiltViewModel(assistedFactory = PdfViewerViewModel.Factory::class)
internal class PdfViewerViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    @ApplicationContext private val context: Context,
    private val pdfSearchEngineFactory: PdfSearchEngineFactory,
    private val getLastPageViewedInPdfUseCase: GetLastPageViewedInPdfUseCase,
    private val setOrUpdateLastPageViewedInPdfUseCase: SetOrUpdateLastPageViewedInPdfUseCase,
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    // search text (debounce + flatMapLatest) reacts to this.
    private val _rawQuery = MutableStateFlow("")

    // search engine is ready to accept queries.
    private val _searchEngineReady = MutableStateFlow(false)

    private var searchEngine: PdfSearchEngine? = null

    init {
        initializeFromArgs()
        observeSearchPipeline()
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it, "Connectivity monitoring failed") }
                .collect { connected ->
                    _state.update { it.copy(isOnline = connected) }
                }
        }
    }

    private fun initializeFromArgs() {
        _state.update { currentState ->
            currentState.copy(
                title = args.title,
                nodeHandle = args.nodeHandle,
                nodeSourceType = args.nodeSourceType,
                isFromChat = args.chatId != null,
                isFromFolderLink = args.nodeSourceType == NodeSourceType.FOLDER_LINK,
                isFromFileLink = args.nodeSourceType == NodeSourceType.FILE_LINK,
                isOffline = args.nodeSourceType == NodeSourceType.OFFLINE,
                source = createSourceFromArgs(),
            )
        }

        loadLastViewedPage()

        // For remote http/https URLs, fetch bytes before rendering (and searching).
        if (!args.isLocalContent &&
            (args.contentUri.startsWith("http://") || args.contentUri.startsWith("https://"))
        ) {
            loadPdfBytes(args.contentUri)
        } else {
            // Local content — initialise search engine immediately from source URI.
            initSearchEngineFromSource()
        }
    }

    private fun loadPdfBytes(url: String) {
        viewModelScope.launch {
            runCatching {
                getDataBytesFromUrlUseCase(URL(url))
            }.onSuccess { bytes ->
                if (bytes != null) {
                    _state.update { it.copy(pdfBytes = PdfBytes(bytes)) }
                    Timber.d("PDF bytes loaded: ${bytes.size} bytes")
                    // Initialise search engine with the downloaded bytes.
                    initSearchEngineFromBytes(bytes)
                } else {
                    Timber.e("Failed to load PDF bytes: null result")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = PdfViewerError.StreamingError(null),
                        )
                    }
                }
            }.onFailure { error ->
                Timber.e(error, "Exception loading PDF bytes from URL")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = PdfViewerError.StreamingError(error.message),
                    )
                }
            }
        }
    }

    private fun initSearchEngineFromBytes(bytes: ByteArray) {
        viewModelScope.launch(ioDispatcher) {
            val engine = pdfSearchEngineFactory.create(context)
            val password = _state.value.currentPassword
            val opened = engine.openFromBytes(bytes, password)
            if (opened) {
                searchEngine?.close()
                searchEngine = engine
                _searchEngineReady.value = true
                Timber.d("PdfSearchEngine initialised from bytes")
            } else {
                engine.close()
                Timber.w("PdfSearchEngine failed to open from bytes")
            }
        }
    }

    private fun initSearchEngineFromSource() {
        val source = _state.value.source ?: return
        viewModelScope.launch(ioDispatcher) {
            val engine = pdfSearchEngineFactory.create(context)
            val password = _state.value.currentPassword

            // For offline files, use openFromFilePath for better reliability
            val opened = when (source) {
                is PdfViewerSource.Offline -> {
                    Timber.d("PdfSearchEngine: opening offline file from path: ${source.localPath}")
                    engine.openFromFilePath(source.localPath, password)
                }

                else -> {
                    val uri = resolveLocalUri(source) ?: run {
                        engine.close()
                        Timber.w("PdfSearchEngine: could not resolve local URI for source $source")
                        return@launch
                    }
                    engine.openFromUri(uri, password)
                }
            }

            if (opened) {
                searchEngine?.close()
                searchEngine = engine
                _searchEngineReady.value = true
                Timber.d("PdfSearchEngine initialised from source: $source")
            } else {
                engine.close()
                Timber.w("PdfSearchEngine failed to open from source: $source")
            }
        }
    }

    /**
     * Re-initialise the search engine after a password change.
     * Called when the user submits a new password.
     */
    private fun reinitSearchEngine() {
        _searchEngineReady.value = false
        val bytes = _state.value.pdfBytes?.bytes
        if (bytes != null) {
            initSearchEngineFromBytes(bytes)
        } else {
            initSearchEngineFromSource()
        }
    }

    private fun resolveLocalUri(source: PdfViewerSource): Uri? = when (source) {
        is PdfViewerSource.Offline -> Uri.fromFile(File(source.localPath))
        is PdfViewerSource.ZipFile -> source.uri
        is PdfViewerSource.ExternalFile -> source.uri
        is PdfViewerSource.CloudNode ->
            if (source.isLocalContent) source.contentUri.toLocalUri() else null

        is PdfViewerSource.ChatAttachment ->
            if (source.isLocalContent) source.contentUri.toLocalUri() else null

        is PdfViewerSource.FileLink ->
            if (source.isLocalContent) source.contentUri.toLocalUri() else null

        is PdfViewerSource.FolderLink ->
            if (source.isLocalContent) source.contentUri.toLocalUri() else null
    }

    private fun String.toLocalUri(): Uri =
        if (startsWith("/")) Uri.fromFile(File(this)) else this.toUri()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearchPipeline() {
        viewModelScope.launch {
            combine(_rawQuery, _searchEngineReady) { query, engineReady ->
                // Emit a non-empty query only when the engine is available.
                if (engineReady && query.length >= 2) query else ""
            }
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        flow { emit(emptyList()) }
                    } else {
                        flow {
                            val results = searchEngine?.searchAllPages(query) ?: emptyList()
                            emit(results)
                        }.flowOn(ioDispatcher)
                    }
                }
                .collect { results ->
                    val firstMatch = results.firstOrNull()
                    val pdfRects = firstMatch?.let {
                        withContext(ioDispatcher) {
                            runCatching { searchEngine?.getPdfRects(it) }.getOrNull()
                        }
                    }
                    _state.update { current ->
                        current.copy(
                            // Auto-navigate to the first match's page.
                            currentPage = firstMatch?.let { m -> m.pageIndex + 1 }
                                ?: current.currentPage,
                            searchState = current.searchState.copy(
                                results = results,
                                currentMatchIndex = if (results.isEmpty()) -1 else 0,
                                currentMatchPdfRects = pdfRects,
                                currentMatchPageIndex = firstMatch?.pageIndex ?: -1,
                                isSearching = false,
                            )
                        )
                    }
                }
        }
    }

    fun toggleToolbarVisibility() {
        _state.update { it.copy(isToolbarVisible = !it.isToolbarVisible) }
    }

    fun hideToolbar() {
        _state.update { it.copy(isToolbarVisible = false) }
    }

    fun onPageChanged(page: Int, totalPages: Int) {
        _state.update { it.copy(currentPage = page, totalPages = totalPages) }
        viewModelScope.launch {
            setOrUpdateLastPageViewedInPdfUseCase(
                LastPageViewedInPdf(
                    nodeHandle = args.nodeHandle,
                    lastPageViewed = page.toLong()
                )
            )
        }
    }

    fun onLoadComplete(pageCount: Int) {
        _state.update {
            it.copy(
                isLoading = false,
                totalPages = pageCount,
                error = null,
            )
        }
    }

    fun onLoadError(error: PdfViewerError) {
        _state.update { current ->
            val resolvedError = when {
                error is PdfViewerError.PasswordProtected &&
                        current.currentPassword != null ->
                    PdfViewerError.InvalidPassword

                else -> error
            }
            current.copy(
                isLoading = false,
                error = resolvedError,
            )
        }
    }

    /**
     * Call when the user edits the password field so the inline "incorrect password" hint clears
     * while the dialog stays open.
     */
    fun onPasswordDialogInputChanged() {
        _state.update { current ->
            when (current.error) {
                is PdfViewerError.InvalidPassword ->
                    current.copy(error = PdfViewerError.PasswordProtected)

                else -> current
            }
        }
    }

    fun submitPassword(password: String) {
        _state.update {
            it.copy(
                currentPassword = password,
                showPasswordDialog = false,
                isLoading = true,
                error = null,
            )
        }
        reinitSearchEngine()
    }

    fun dismissPasswordDialog() {
        _state.update {
            it.copy(showPasswordDialog = false, error = null)
        }
    }

    fun retryLoad() {
        _state.update { it.copy(isLoading = true, error = null) }
        if (!args.isLocalContent &&
            (args.contentUri.startsWith("http://") || args.contentUri.startsWith("https://"))
        ) {
            loadPdfBytes(args.contentUri)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun activateSearch() {
        _state.update { it.copy(searchState = it.searchState.copy(isSearchActive = true)) }
    }

    fun deactivateSearch() {
        _rawQuery.value = ""
        _state.update { it.copy(searchState = PdfViewerSearchState(isSearchActive = false)) }
    }

    /**
     * Called as the user types in the search field.
     * The reactive pipeline ([observeSearchPipeline]) handles debounce and cancellation.
     */
    fun onSearchQueryChanged(text: String) {
        _rawQuery.value = text
        _state.update {
            it.copy(
                searchState = it.searchState.copy(
                    query = text,
                    isSearching = text.length >= 2,
                )
            )
        }
    }

    /**
     * Navigate to the next search result.
     * Updates both [PdfViewerState.currentPage] and [PdfViewerSearchState.currentMatchIndex]
     * so the PDF scrolls to the correct page.
     */
    fun navigateToNextMatch() {
        val search = _state.value.searchState
        if (!search.hasResults) return
        val next = (search.currentMatchIndex + 1) % search.totalMatches
        selectMatch(next)
    }

    /**
     * Navigate to the previous search result.
     * Updates both [PdfViewerState.currentPage] and [PdfViewerSearchState.currentMatchIndex].
     */
    fun navigateToPreviousMatch() {
        val search = _state.value.searchState
        if (!search.hasResults) return
        val prev = if (search.currentMatchIndex <= 0) search.totalMatches - 1
        else search.currentMatchIndex - 1
        selectMatch(prev)
    }

    private fun selectMatch(index: Int) {
        val match = _state.value.searchState.results.getOrNull(index) ?: return
        viewModelScope.launch {
            val pdfRects = withContext(ioDispatcher) {
                runCatching { searchEngine?.getPdfRects(match) }.getOrNull()
            }
            _state.update { current ->
                current.copy(
                    currentPage = match.pageIndex + 1,
                    searchState = current.searchState.copy(
                        currentMatchIndex = index,
                        currentMatchPdfRects = pdfRects,
                        currentMatchPageIndex = match.pageIndex,
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchEngine?.close()
        searchEngine = null
    }

    private fun createSourceFromArgs(): PdfViewerSource {
        val contentUri = args.contentUri
        val isLocal = args.isLocalContent

        if (args.nodeSourceType == NodeSourceType.OFFLINE) {
            return PdfViewerSource.Offline(
                handle = args.nodeHandle.toString(),
                localPath = contentUri,
            )
        }

        if (args.chatId != null) {
            return PdfViewerSource.ChatAttachment(
                chatId = args.chatId,
                messageId = args.messageId ?: -1L,
                nodeHandle = args.nodeHandle,
                contentUri = contentUri,
                isLocalContent = isLocal,
            )
        }

        return when (args.nodeSourceType) {
            NodeSourceType.FOLDER_LINK -> PdfViewerSource.FolderLink(
                nodeHandle = args.nodeHandle,
                contentUri = contentUri,
                isLocalContent = isLocal,
            )

            NodeSourceType.FILE_LINK -> PdfViewerSource.FileLink(
                serializedNode = "",
                url = null,
                contentUri = contentUri,
                isLocalContent = isLocal,
            )

            NodeSourceType.RUBBISH_BIN,
            NodeSourceType.BACKUPS,
            NodeSourceType.CLOUD_DRIVE,
                -> PdfViewerSource.CloudNode(
                nodeHandle = args.nodeHandle,
                contentUri = contentUri,
                isLocalContent = isLocal,
                nodeSourceType = args.nodeSourceType,
            )

            else -> PdfViewerSource.CloudNode(
                nodeHandle = args.nodeHandle,
                contentUri = contentUri,
                isLocalContent = isLocal,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )
        }
    }

    private fun loadLastViewedPage() {
        viewModelScope.launch {
            val lastPage = getLastPageViewedInPdfUseCase(args.nodeHandle)
            _state.update { it.copy(currentPage = lastPage?.toInt() ?: 1) }
        }
    }

    /**
     * Arguments for the PdfViewerViewModel.
     *
     * @param nodeHandle The handle of the node to display
     * @param contentUri The content URI string for the PDF (local file path or remote URL)
     * @param isLocalContent True if content is local, false if remote streaming
     * @param nodeSourceType The source type of the node (use FOLDER_LINK/FILE_LINK for links)
     * @param mimeType The MIME type of the file
     * @param title Optional title to display in the toolbar
     * @param chatId The chat ID if opening from chat (optional)
     * @param messageId The message ID if opening from chat (optional)
     * @param shouldStopHttpServer True if HTTP server should be stopped when done
     */
    data class Args(
        val nodeHandle: Long,
        val contentUri: String,
        val isLocalContent: Boolean,
        val nodeSourceType: NodeSourceType,
        val mimeType: String,
        val title: String?,
        val chatId: Long?,
        val messageId: Long?,
        val shouldStopHttpServer: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(args: Args): PdfViewerViewModel
    }
}
