package mega.privacy.android.feature.pdfviewer.presentation

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shockwave.pdfium.PdfTextMatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.core.nodecomponents.mapper.OfflineTypedNodeMapper
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveRecentlyUsedItemIfQualifiesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetPublicNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.pdf.GetLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.pdf.SetOrUpdateLastPageViewedInPdfUseCase
import mega.privacy.android.feature.pdfviewer.presentation.PdfViewerViewModel.Companion.PREFETCH_RADIUS
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngine
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineFactory
import mega.privacy.android.feature_flags.AppFeatures
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
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val saveRecentlyUsedItemIfQualifiesUseCase: SaveRecentlyUsedItemIfQualifiesUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val getPublicNodeByIdUseCase: GetPublicNodeByIdUseCase,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val offlineTypedNodeMapper: OfflineTypedNodeMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    // search text (debounce + flatMapLatest) reacts to this.
    private val _rawQuery = MutableStateFlow("")

    // search engine is ready to accept queries.
    private val _searchEngineReady = MutableStateFlow(false)

    // Guards searchEngine assignment against race between init coroutine and onCleared.
    private val engineLock = Any()

    private var searchEngine: PdfSearchEngine? = null

    // All three fields below are accessed only on Dispatchers.Main (viewModelScope default),
    // so no synchronization is needed.

    // Incremented on every query change; captured once per fetchRectsForVisiblePages call so
    // all coroutines launched from that call skip state updates if the query has since changed.
    private var rectFetchGeneration = 0

    // Pages that currently have an in-flight rect fetch.
    private val fetchingPages = mutableSetOf<Int>()

    // results grouped by pageIndex for O(1) lookup in fetchRectsForVisiblePages.
    private var searchMatchesByPage: Map<Int, List<PdfTextMatch>> = emptyMap()

    init {
        loadFileExplorerFeatureFlag()
        initializeFromArgs()
        observeSearchPipeline()
        observeConnectivity()
        monitorOfflineNodeAvailability()
        loadCurrentNode()
        monitorNodeUpdates()
    }

    private fun loadFileExplorerFeatureFlag() {
        viewModelScope.launch {
            val enabled =
                runCatching { getFeatureFlagValueUseCase(AppFeatures.CloudExplorer) }.getOrElse { false }
            _state.update { it.copy(isFileExplorerEnabled = enabled) }
        }
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

    private fun monitorOfflineNodeAvailability() {
        if (args.nodeSourceType != NodeSourceType.OFFLINE) return
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase()
                .catch { Timber.e(it, "Offline node monitoring failed") }
                .collect { offlineList ->
                    val handle = args.nodeHandle.toString()
                    val stillAvailable = offlineList.any { it.handle == handle }
                    if (!stillAvailable) {
                        _state.update { it.copy(dismissEvent = triggered) }
                    }
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
                isExternalFile = args.isExternalFile,
                source = createSourceFromArgs(),
            )
        }

        if (args.isExternalFile) {
            // External files don't save currentPage
            _state.update { it.copy(currentPage = 1) }
        } else {
            // Show loading until currentPage is resolved
            loadLastViewedPage()
        }

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
                synchronized(engineLock) {
                    if (!isActive) {
                        engine.close()
                        return@launch
                    }
                    searchEngine?.close()
                    searchEngine = engine
                }
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
                synchronized(engineLock) {
                    if (!isActive) {
                        engine.close()
                        return@launch
                    }
                    searchEngine?.close()
                    searchEngine = engine
                }
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
        is PdfViewerSource.ZipFile -> Uri.parse(source.contentUri)
        is PdfViewerSource.ExternalFile -> Uri.parse(source.contentUri)
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
                            val results = runCatching {
                                searchEngine?.searchAllPages(query) ?: emptyList()
                            }.onFailure { e ->
                                // Re-throw so flatMapLatest can cancel this flow when a new query arrives.
                                if (e is CancellationException) throw e
                                Timber.e(e, "Search error")
                            }.getOrDefault(emptyList())
                            emit(results)
                        }.flowOn(ioDispatcher)
                    }
                }
                .collect { results ->
                    searchMatchesByPage = results.groupBy { it.pageIndex }
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
                                allMatchRectsByPage = emptyMap(),
                            )
                        )
                    }
                    if (firstMatch != null) {
                        fetchRectsForVisiblePages(firstMatch.pageIndex)
                    }
                }
        }
    }

    /**
     * Invalidates all in-flight rect fetches by incrementing the generation counter and
     * clearing the in-flight tracking set. Callers are responsible for clearing
     * [PdfViewerSearchState.allMatchRectsByPage] in their own state update.
     */
    private fun clearRectCache() {
        rectFetchGeneration++
        fetchingPages.clear()
        searchMatchesByPage = emptyMap()
    }

    /**
     * Fetches bounding rects for all matches on pages surrounding [centerPageIndex] (±[PREFETCH_RADIUS] pages).
     * Results are stored in [PdfViewerSearchState.allMatchRectsByPage]. Already-fetched or
     * in-flight pages are skipped. All results are retained until the search query changes.
     */
    private fun fetchRectsForVisiblePages(centerPageIndex: Int) {
        val searchState = _state.value.searchState
        if (!searchState.hasResults) return

        val totalPages = _state.value.totalPages
        if (totalPages == 0) return
        val from = (centerPageIndex - PREFETCH_RADIUS).coerceAtLeast(0)
        val to = (centerPageIndex + PREFETCH_RADIUS).coerceAtMost(totalPages - 1)
        val pageRange = from..to
        val generation = rectFetchGeneration

        for (pageIndex in pageRange) {
            if (searchState.allMatchRectsByPage.containsKey(pageIndex)) continue
            val matchesForPage = searchMatchesByPage[pageIndex] ?: emptyList()
            if (matchesForPage.isEmpty()) continue
            if (!fetchingPages.add(pageIndex)) continue  // already in-flight

            viewModelScope.launch {
                val allRects = withContext(ioDispatcher) {
                    matchesForPage.flatMap { match ->
                        runCatching { searchEngine?.getPdfRects(match) ?: emptyList() }
                            .getOrElse { emptyList() }
                    }
                }
                if (generation == rectFetchGeneration) {
                    fetchingPages.remove(pageIndex)
                    _state.update { current ->
                        current.copy(
                            searchState = current.searchState.copy(
                                allMatchRectsByPage = current.searchState.allMatchRectsByPage + (pageIndex to allRects)
                            )
                        )
                    }
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
        if (_state.value.searchState.hasResults) {
            fetchRectsForVisiblePages(page - 1) // page is 1-indexed; convert to 0-indexed
        }
        if (!args.isExternalFile) {
            viewModelScope.launch {
                runCatching {
                    setOrUpdateLastPageViewedInPdfUseCase(
                        LastPageViewedInPdf(
                            nodeHandle = args.nodeHandle,
                            lastPageViewed = page.toLong()
                        )
                    )
                }.onFailure { Timber.e(it, "Failed to persist last viewed PDF page") }
            }
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
                isLoading = true,
                error = null,
            )
        }
        reinitSearchEngine()
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

    fun resetDismissEvent() {
        _state.update { it.copy(dismissEvent = consumed) }
    }

    fun activateSearch() {
        _state.update { it.copy(searchState = it.searchState.copy(isSearchActive = true)) }
    }

    fun deactivateSearch() {
        clearRectCache()
        _rawQuery.value = ""
        _state.update { it.copy(searchState = PdfViewerSearchState(isSearchActive = false)) }
    }

    /**
     * Called as the user types in the search field.
     * The reactive pipeline ([observeSearchPipeline]) handles debounce and cancellation.
     */
    fun onSearchQueryChanged(text: String) {
        clearRectCache()
        _rawQuery.value = text
        _state.update {
            it.copy(
                searchState = it.searchState.copy(
                    query = text,
                    isSearching = text.length >= 2,
                    allMatchRectsByPage = emptyMap(),
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

    public override fun onCleared() {
        super.onCleared()
        persistContinueWhereLeftOffOnExit()
        synchronized(engineLock) {
            searchEngine?.close()
            searchEngine = null
        }
    }

    /**
     * Decides whether the PDF belongs in Continue Where Left Off when the user leaves the viewer,
     * rather than adding it on open. A document read through to the near-completion fraction is
     * dropped from the carousel (and any stale entry removed) so a finished document is not
     * surfaced back as resumable; anything less is saved as resumable.
     *
     * The last viewed page is persisted independently in [onPageChanged], so reading progress is
     * retained regardless of this widget decision. Runs on [applicationScope] because
     * [viewModelScope] is already cancelled by the time [onCleared] is called.
     */
    private fun persistContinueWhereLeftOffOnExit() {
        if (args.isExternalFile) return
        val currentPage = _state.value.currentPage ?: return
        val totalPages = _state.value.totalPages
        if (totalPages <= 0) return
        val nodeHandle = args.nodeHandle
        val fileName = args.title.orEmpty()
        applicationScope.launch {
            runCatching {
                saveRecentlyUsedItemIfQualifiesUseCase(
                    nodeHandle = nodeHandle,
                    type = RecentlyUsedType.PDF,
                    fileName = fileName,
                    progress = currentPage.toFloat() / totalPages,
                )
            }.onFailure { Timber.e(it, "Failed to persist PDF Continue Where Left Off on exit") }
        }
    }

    private fun createSourceFromArgs(): PdfViewerSource {
        val contentUri = args.contentUri
        val isLocal = args.isLocalContent

        if (args.isExternalFile) {
            return PdfViewerSource.ExternalFile(
                contentUri = contentUri,
                fileName = args.title,
            )
        }

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
            val lastPage = runCatching { getLastPageViewedInPdfUseCase(args.nodeHandle)?.toInt() ?: 1 }
                    .onFailure { Timber.e(it, "Failed to load last viewed page for node ${args.nodeHandle}") }
                    .getOrDefault(1)
            _state.update { it.copy(currentPage = lastPage) }
        }
    }

    /**
     * Resolves the current [TypedNode] and stores it on state.
     *
     * The destination feeds this node into [NodeOptionsActionViewModel.updateSelectionModeAvailableActions]
     * to populate the floating-toolbar action list — the same path used by Cloud Drive's
     * selection-mode toolbar.
     *
     * For OFFLINE sources whose original cloud node is gone, falls back to the offline file
     * information so the toolbar still works on orphan offline copies (mirrors
     * `NodeOptionsBottomSheetViewModel.loadOfflineFallbackNode`).
     *
     * For FILE_LINK sources the node is not in the signed-in account, so it is resolved from the
     * public link URL via [GetPublicNodeUseCase] instead of by handle.
     *
     * For FOLDER_LINK sources the node lives in the folder-link session (MegaApiFolder), not the
     * account, so [GetNodeByIdUseCase] (account-only) would return null. It is resolved via
     * [GetPublicNodeByIdUseCase], which falls back to the folder API and authorizes the node.
     *
     * No-op for external files (no MEGA node).
     */
    private fun loadCurrentNode() {
        if (args.isExternalFile || args.nodeHandle == INVALID_NODE_HANDLE) return
        viewModelScope.launch {
            val node: TypedNode? = when (args.nodeSourceType) {
                NodeSourceType.FILE_LINK -> loadPublicLinkNode()

                NodeSourceType.FOLDER_LINK ->
                    runCatching { getPublicNodeByIdUseCase(NodeId(args.nodeHandle)) }
                        .onFailure { Timber.e(it, "Failed to resolve folder-link node for floating toolbar") }
                        .getOrNull()

                else -> {
                    val nodeId = NodeId(args.nodeHandle)
                    runCatching { getNodeByIdUseCase(nodeId) }
                        .getOrElse {
                            Timber.e(it, "Failed to resolve node for floating toolbar")
                            null
                        } ?: loadOfflineFallbackNode(nodeId)
                }
            }
            node ?: return@launch
            _state.update { it.copy(currentNode = node, title = node.name) }
        }
    }

    /**
     * Refreshes the title and current node when the open node changes elsewhere (rename, or a
     * permission change on another device / web), keeping the toolbar and action list in sync.
     */
    private fun monitorNodeUpdates() {
        if (args.isExternalFile || args.nodeHandle == INVALID_NODE_HANDLE) return
        monitorNodeUpdatesUseCase()
            .filter { update -> update.changes.keys.any { it.id.longValue == args.nodeHandle } }
            .onEach { loadCurrentNode() }
            .catch { Timber.e(it, "PDF viewer: node updates flow failed") }
            .launchIn(viewModelScope)
    }

    private suspend fun loadPublicLinkNode(): TypedNode? {
        val url = args.publicLinkUrl ?: return null
        return runCatching { getPublicNodeUseCase(url) }
            .onFailure { Timber.e(it, "Failed to resolve public node for file-link toolbar") }
            .getOrNull()
            ?.let { PublicLinkFile(it, null) }
    }

    private suspend fun loadOfflineFallbackNode(nodeId: NodeId): TypedNode? {
        if (args.nodeSourceType != NodeSourceType.OFFLINE) return null
        return runCatching { getOfflineFileInformationByIdUseCase(nodeId) }
            .onFailure { Timber.e(it, "Failed to load offline file information for $nodeId") }
            .getOrNull()
            ?.let(offlineTypedNodeMapper::invoke)
    }

    /**
     * Arguments for the PdfViewerViewModel.
     *
     * @param nodeHandle The handle of the node to display. -1L for external files.
     * @param contentUri The content URI string for the PDF (local file path or remote URL)
     * @param isLocalContent True if content is local, false if remote streaming
     * @param nodeSourceType The source type of the node (use FOLDER_LINK/FILE_LINK for links)
     * @param mimeType The MIME type of the file
     * @param title Optional title to display in the toolbar
     * @param chatId The chat ID if opening from chat (optional)
     * @param messageId The message ID if opening from chat (optional)
     * @param shouldStopHttpServer True if HTTP server should be stopped when done
     * @param isExternalFile True if the PDF was opened from an external app intent (no MEGA node)
     * @param publicLinkUrl The public file-link URL when opened from a file link
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
        val isExternalFile: Boolean = false,
        val publicLinkUrl: String? = null,
    )

    @AssistedFactory
    interface Factory {
        fun create(args: Args): PdfViewerViewModel
    }

    private companion object {
        /** Number of pages to prefetch on each side of the current page. */
        const val PREFETCH_RADIUS = 1

        /** Sentinel node handle used for external files that have no MEGA node. */
        const val INVALID_NODE_HANDLE = -1L
    }
}
