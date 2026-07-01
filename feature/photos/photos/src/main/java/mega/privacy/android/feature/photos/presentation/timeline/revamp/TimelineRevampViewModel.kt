package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabNormalModeActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampViewModel.Companion.MAX_CACHED_ITEMS
import mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper.MediaTimelineNodeUiItemMapper
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeCompactSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeDefaultSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeLargeSelectedEvent
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the Timeline Revamp screen.
 *
 * Sections come from [GetMediaTimelineSectionsUseCase] and lay out the grid (a header plus
 * `count` slots each). Each section is paginated **independently** through
 * [ListMediaNodesByOffsetUseCase] (its query is scoped to the section's date range), so a deep section
 * loads directly without paging through the ones before it. Loaded items are cached by global index
 * with a bounded LRU and only the currently visible window is fetched.
 */
@HiltViewModel
class TimelineRevampViewModel @Inject constructor(
    private val getMediaTimelineSectionsUseCase: GetMediaTimelineSectionsUseCase,
    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase,
    private val mediaTimelineNodeUiItemMapper: MediaTimelineNodeUiItemMapper,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    private val getCameraUploadFolderHandlesUseCase: GetCameraUploadFolderHandlesUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val sections = MutableStateFlow<List<MediaTimelineSection>>(emptyList())
    private val loadedNodes = MutableStateFlow<Map<Int, PhotosNodeContentItemV2>>(emptyMap())
    private val targetRange = MutableStateFlow(IntRange.EMPTY)
    private val mediaLoaderStarted = AtomicBoolean(false)

    /**
     * Backs [TimelineRevampUiState.Data.takenDownDialogEvent]; folded into [uiState] so the screen
     * observes a single state object.
     */
    private val _takenDownDialogEvent = MutableStateFlow<StateEvent>(consumed)

    /**
     * The grid size (Compact / Default / Large). Drives only the grid column count, so it lives in
     * [uiState] (not [currentFilter]) — changing it must not reload the timeline.
     */
    private val gridSizeFlow = MutableStateFlow(TimelineGridSize.Default)

    /**
     * The selected sort option (Newest / Oldest). Drives the section order and the per-section page
     * order; changing it reloads the timeline (via [monitorTimelineSections]).
     */
    private val sortOptionsFlow = MutableStateFlow(TimelineTabSortOptions.Newest)

    /**
     * Whether the sort toolbar action is enabled. Mirrors the tab: disabled when there is no content
     * to sort, or when the Camera Uploads promo page is showing for a non-Cloud-Drive source. Driven
     * imperatively by [updateSortActionEnablement] / [updateSortActionBasedOnCUPageEnablement].
     */
    private val sortActionEnabledFlow = MutableStateFlow(true)

    /**
     * The selected media time period (All / Days / Months / Years). Hoisted here — rather than in the
     * UI state — so it survives navigation and changing it does not trigger a UI-state rebuild.
     */
    var selectedTimePeriod by mutableStateOf(MediaTimePeriod.All)
        private set

    private val selectedFilterFlow = MutableStateFlow<Map<String, String?>?>(null)

    /**
     * The filter UI state shown by the filter sheet. Combines the persisted preferences with the
     * in-session [selectedFilterFlow]; the stored filter is only re-applied on a fresh load when the
     * user previously chose to remember it (handled by [timelineFilterUiStateMapper]).
     */
    internal val filterUiState: StateFlow<TimelineFilterUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            flow { emit(getTimelineFilterPreferencesUseCase()) }
                .catch { Timber.e(it) },
            selectedFilterFlow,
        ) { preferenceMap, newFilter ->
            timelineFilterUiStateMapper(
                preferenceMap = newFilter ?: preferenceMap,
                shouldApplyFilterFromPreference = newFilter != null,
            )
        }.asUiStateFlow(viewModelScope, TimelineFilterUiState())
    }

    /**
     * Whether the hidden-nodes feature is enabled for the account. Shared (used by both
     * [currentFilter], to scope sensitivity, and [uiState], to drive the grid blur).
     */
    private val isHiddenNodesEnabled: StateFlow<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        monitorHiddenNodesEnabledUseCase()
            .catch { Timber.e(it, "Unable to monitor hidden nodes enabled") }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }

    /**
     * The filter that scopes both the timeline sections and the per-section node pages, derived
     * reactively from [filterUiState] and the hidden-nodes state. Sensitive nodes are excluded only
     * when the hidden-nodes feature is enabled and "show hidden items" is off (matching the tab).
     * Sort and granularity remain at their defaults until the later parity steps. Eagerly shared so
     * [fetchSectionNodes] can read the current value synchronously.
     */
    private val currentFilter: StateFlow<MediaTimelineFilter> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            filterUiState,
            isHiddenNodesEnabled,
            monitorShowHiddenItemsUseCase().catch { Timber.e(it); emit(false) },
        ) { filter, hiddenNodesEnabled, showHiddenItems ->
            val sensitivity = if (hiddenNodesEnabled && !showHiddenItems) {
                Sensitivity.HideSensitive
            } else {
                Sensitivity.ShowAll
            }
            timelineFilterUiStateMapper(filter, getCameraUploadFolderHandles())
                .copy(sensitivity = sensitivity)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_FILTER)
    }

    /**
     * Resolves the Camera Upload / Media Upload folder handles, defaulting to empty if resolution
     * fails so the error can't tear down the reactive [currentFilter] pipeline.
     */
    private suspend fun getCameraUploadFolderHandles(): List<NodeId> =
        runCatching { getCameraUploadFolderHandlesUseCase() }
            .onFailure { Timber.e(it, "Failed to resolve camera upload folder handles") }
            .getOrDefault(emptyList())

    /**
     * The section layout the cache was built against. When the sections change, the global-index
     * keying is invalidated, so the cache is reset.
     */
    private var cachedSections: List<MediaTimelineSection>? = null

    /**
     * Prefix index over [cachedSections], rebuilt only when the sections change:
     * [sectionStartOffsets]`[i]` is the global slot index where section `i` begins, and
     * [totalSlots] is the grand total. Lets the media loader locate visible sections without re-summing
     * every section's count on each scroll. Only touched by the single media-loader coroutine.
     */
    private var sectionStartOffsets: List<Int> = emptyList()

    private var totalSlots = 0

    /**
     * Cache loaded items, keyed by global index. Drops the oldest entries once
     * [MAX_CACHED_ITEMS] is exceeded. Only mutated by the single media-loader coroutine.
     */
    private val mediaCache = object : LinkedHashMap<Int, PhotosNodeContentItemV2>() {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Int, PhotosNodeContentItemV2>,
        ): Boolean = size > MAX_CACHED_ITEMS
    }

    val uiState: StateFlow<TimelineRevampUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorTimelineSections(),
            loadedNodes,
            isHiddenNodesEnabled,
            gridSizeFlow,
            sortOptionsFlow,
        ) { sectionsState, loaded, hiddenNodesEnabled, gridSize, currentSort ->
            when (sectionsState) {
                SectionsState.Loading -> TimelineRevampUiState.Loading
                is SectionsState.Loaded ->
                    toUiState(
                        sections = sectionsState.sections,
                        loaded = loaded,
                        isHiddenNodesEnabled = hiddenNodesEnabled,
                        gridSize = gridSize,
                        currentSort = currentSort
                    )
            }
        }
            // Inject the taken-down dialog event into the loaded state so the screen observes a
            // single uiState object; kept out of the main combine (already at its 5-arg limit).
            .combine(_takenDownDialogEvent) { state, takenDownDialogEvent ->
                if (state is TimelineRevampUiState.Data) {
                    state.copy(takenDownDialogEvent = takenDownDialogEvent)
                } else {
                    state
                }
            }
            .catch { e -> Timber.e(e, "Failed to load media timeline sections") }
            .asUiStateFlow(
                viewModelScope,
                TimelineRevampUiState.Loading,
            )
    }

    /**
     * Toolbar action state for the shared top bar (same type as the tab). [TimelineTabActionUiState.isReady]
     * follows the loading state reactively (ready once sections finish loading), while
     * [TimelineTabNormalModeActionUiState.enableSort] is driven by [sortActionEnabledFlow].
     */
    internal val actionUiState: StateFlow<TimelineTabActionUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            uiState.map { it !is TimelineRevampUiState.Loading }.distinctUntilChanged(),
            sortActionEnabledFlow,
        ) { isReady, enableSort ->
            TimelineTabActionUiState(
                isReady = isReady,
                normalModeItem = TimelineTabNormalModeActionUiState(enableSort = enableSort),
            )
        }.asUiStateFlow(viewModelScope, TimelineTabActionUiState())
    }

    /**
     * For each [currentFilter] or [sortOptionsFlow] change, resets the timeline to loading and
     * re-fetches its sections: emits [SectionsState.Loading] while clearing the previous sections and
     * loaded media (the media-loader coroutine clears its own cache when it sees [sections] cleared),
     * then emits [SectionsState.Loaded] with the freshly-fetched sections (or empty if the fetch fails).
     * The sort order also flips the section orientation (newest-first vs oldest-first).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorTimelineSections(): Flow<SectionsState> =
        combine(currentFilter, sortOptionsFlow) { filter, sort -> filter to sort }
            .flatMapLatest { (filter, sort) ->
                flow {
                    emit(SectionsState.Loading)
                    sections.update { emptyList() }
                    loadedNodes.update { emptyMap() }

                    val loadedSections = getTimelineSections(filter, sort.sortOrder)
                    sections.update { loadedSections }
                    emit(SectionsState.Loaded(loadedSections))
                }
            }

    private suspend fun getTimelineSections(filter: MediaTimelineFilter, order: SortOrder) =
        runCatching { getMediaTimelineSectionsUseCase(filter, order) }
            .getOrDefault(emptyList())

    private fun toUiState(
        sections: List<MediaTimelineSection>,
        loaded: Map<Int, PhotosNodeContentItemV2>,
        isHiddenNodesEnabled: Boolean,
        gridSize: TimelineGridSize,
        currentSort: TimelineTabSortOptions,
    ): TimelineRevampUiState =
        if (sections.isEmpty()) {
            TimelineRevampUiState.Empty
        } else {
            TimelineRevampUiState.Data(
                sections = sections,
                sectionStartOffsets = sectionStartOffsetsOf(sections),
                loadedNodes = loaded,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                gridSize = gridSize,
                currentSort = currentSort,
            )
        }

    /**
     * The loading state of the timeline sections for the current filter, kept separate from the
     * per-node [loadedNodes] so a filter change can reset to loading immediately.
     */
    private sealed interface SectionsState {
        data object Loading : SectionsState
        data class Loaded(val sections: List<MediaTimelineSection>) : SectionsState
    }

    /**
     * Called by the screen when the set of visible media slots changes.
     *
     * @param firstIndex the first visible global media index
     * @param lastIndex the last visible global media index
     */
    fun onVisibleRangeChanged(firstIndex: Int, lastIndex: Int) {
        if (firstIndex !in 0..lastIndex) return
        val loadBuffer = lastIndex - firstIndex
        // Load what is visible plus a buffer (same count of items as visible items) of items before and after it,
        // so scrolling either way is instant.
        targetRange.update {
            (firstIndex - loadBuffer)
                .coerceAtLeast(0)..(lastIndex + loadBuffer)
        }
        ensureMediaLoaderStarted()
    }

    @OptIn(FlowPreview::class)
    private fun ensureMediaLoaderStarted() {
        if (!mediaLoaderStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            combine(targetRange, sections) { range, currentSections -> range to currentSections }
                .debounce(SCROLL_SETTLE_DEBOUNCE_MS.milliseconds)
                .collectLatest { (range, newSections) ->
                    resetMediaCache(newSections)
                    ensureRangeLoaded(range, newSections)
                }
        }
    }

    private fun resetMediaCache(newSections: List<MediaTimelineSection>) {
        if (newSections != cachedSections) {
            cachedSections = newSections
            rebuildSectionsOffset(newSections)
            mediaCache.clear()
            loadedNodes.update { emptyMap() }
        }
    }

    private fun rebuildSectionsOffset(sections: List<MediaTimelineSection>) {
        sectionStartOffsets = sectionStartOffsetsOf(sections)
        totalSlots = sections.sumOf { it.count }.toInt()
    }

    private fun sectionStartOffsetsOf(sections: List<MediaTimelineSection>): List<Int> {
        var start = 0
        return sections.map { section ->
            start.also { start += section.count.toInt() }
        }
    }

    /**
     * Loads the media for the currently visible slots (plus the load buffer already baked into the
     * target range by [onVisibleRangeChanged]).
     *
     * The grid is a single list of slots; each section owns a contiguous block of it. Using the
     * cached [sectionStartOffsets], this jumps to the first on-screen section and loads the visible
     * slice (in section-local indices) of each on-screen section until it passes the visible window.
     */
    private suspend fun ensureRangeLoaded(
        range: IntRange,
        sections: List<MediaTimelineSection>,
    ) {
        if (sections.isEmpty() || range.isEmpty()) return

        // The visible window in global slot indices, clamped to the slots that actually exist.
        val visibleFrom = range.first.coerceAtLeast(0)
        val visibleTo = range.last.coerceAtMost(totalSlots - 1)
        if (visibleTo < visibleFrom) return

        var index = findSectionIndexOf(visibleFrom)
        while (index < sections.size) {
            val sectionStartOffset = sectionStartOffsets[index]
            if (sectionStartOffset > visibleTo) break // every later section is past the visible window
            val section = sections[index]
            val slotCount = section.count.toInt()
            val sectionEnd = sectionStartOffset + slotCount - 1
            if (sectionEnd >= visibleFrom) {
                // The part of this section inside the window, in its own (section-local) indices.
                val localWindowStart = (visibleFrom - sectionStartOffset).coerceAtLeast(0)
                val localWindowEnd = (visibleTo - sectionStartOffset).coerceAtMost(slotCount - 1)
                loadSectionWindow(
                    section = section,
                    sectionGlobalStart = sectionStartOffset,
                    localWindowStart = localWindowStart,
                    localWindowEnd = localWindowEnd,
                )
            }
            index++
        }
    }

    /**
     * Index of the section whose slot block where [targetIndex] is located at.
     */
    private fun findSectionIndexOf(targetIndex: Int): Int =
        sectionStartOffsets
            .indexOfLast { startOffset -> startOffset <= targetIndex }
            .coerceAtLeast(0)

    /**
     * Loads the section-local window `[localWindowStart, localWindowEnd]` of [section]: locates the
     * contiguous run of slots not yet cached, fetches just that run, and caches it. Already-cached
     * slots are never re-requested, and only the window — never the whole section — is materialized.
     */
    private suspend fun loadSectionWindow(
        section: MediaTimelineSection,
        sectionGlobalStart: Int,
        localWindowStart: Int,
        localWindowEnd: Int,
    ) {
        val uncachedRange = findUncachedRange(
            sectionGlobalStart = sectionGlobalStart,
            localWindowStart = localWindowStart,
            localWindowEnd = localWindowEnd,
        ) ?: return // the whole window is already cached

        val nodes = fetchSectionNodes(section, uncachedRange) ?: return
        cacheSectionNodes(sectionGlobalStart, uncachedRange.first, nodes)
    }

    /**
     * The contiguous run of section-local slots within `[localWindowStart, localWindowEnd]` that are
     * not yet in [mediaCache], or null when every slot in the window is already cached.
     */
    private fun findUncachedRange(
        sectionGlobalStart: Int,
        localWindowStart: Int,
        localWindowEnd: Int,
    ): IntRange? {
        var firstUncached = -1
        var lastUncached = -1
        for (localIndex in localWindowStart..localWindowEnd) {
            if (sectionGlobalStart + localIndex !in mediaCache) {
                if (firstUncached == -1) {
                    firstUncached = localIndex
                }
                lastUncached = localIndex
            }
        }
        return if (firstUncached == -1) null else firstUncached..lastUncached
    }

    /**
     * Fetches the nodes for the section-local [localRange] of [section], or null on failure. The
     * query is scoped to the section, so the offset is the section-local position — only the range
     * is materialized, never the whole section.
     */
    private suspend fun fetchSectionNodes(
        section: MediaTimelineSection,
        localRange: IntRange,
    ): List<TypedFileNode>? {
        Timber.d("Fetching section '${section.groupId}' offsets [${localRange.first}, ${localRange.last}]")
        return runCatching {
            listMediaNodesByOffsetUseCase(
                filter = currentFilter.value,
                section = section,
                order = sortOptionsFlow.value.sortOrder,
                maxElements = localRange.last - localRange.first + 1,
                offset = localRange.first.toLong(),
            )
        }.onFailure {
            Timber.e(
                it,
                "Failed to load [${localRange.first}, ${localRange.last}] of section ${section.groupId}"
            )
        }.getOrNull()
    }

    /**
     * Caches [nodes] at their global indices — the run starts at section-local [localStart], i.e.
     * global index `sectionGlobalStart + localStart` — and publishes the updated cache to the UI.
     */
    private fun cacheSectionNodes(
        sectionGlobalStart: Int,
        localStart: Int,
        nodes: List<TypedFileNode>,
    ) {
        if (nodes.isEmpty()) return
        nodes.forEachIndexed { i, node ->
            mediaCache[sectionGlobalStart + localStart + i] = mediaTimelineNodeUiItemMapper(node)
        }
        loadedNodes.update { HashMap(mediaCache) }
    }

    /**
     * Updates the selected media time period. Hoisted state, so it does not trigger a UI-state rebuild.
     */
    fun onMediaTimePeriodSelected(value: MediaTimePeriod) {
        selectedTimePeriod = value
    }

    /**
     * Updates the grid size (and tracks the selection). Only changes the grid column count — it does
     * not reload the timeline.
     */
    fun onGridSizeChange(size: TimelineGridSize) {
        gridSizeFlow.update { size }
        trackGridSizeSelection(size)
    }

    /**
     * Pinch-zoom in: steps the grid towards larger tiles (fewer columns), stopping at the largest
     * size. No-op when already at the largest size.
     */
    fun onZoomIn() {
        val index = TimelineGridSize.entries.indexOf(gridSizeFlow.value)
        if (index > 0) {
            onGridSizeChange(TimelineGridSize.entries[index - 1])
        }
    }

    /**
     * Pinch-zoom out: steps the grid towards smaller tiles (more columns), stopping at the smallest
     * size. No-op when already at the smallest size.
     */
    fun onZoomOut() {
        val index = TimelineGridSize.entries.indexOf(gridSizeFlow.value)
        if (index < TimelineGridSize.entries.lastIndex) {
            onGridSizeChange(TimelineGridSize.entries[index + 1])
        }
    }

    /**
     * Updates the sort option (Newest / Oldest). Changing it reloads the timeline through
     * [monitorTimelineSections], flipping the section order and the per-section page order.
     */
    fun onSortOptionsChange(value: TimelineTabSortOptions) {
        sortOptionsFlow.update { value }
    }

    /**
     * Disables the sort action when the Camera Uploads promo page is showing for a non-Cloud-Drive
     * source; otherwise defers to [updateSortActionEnablement]. Mirrors the tab.
     */
    internal fun updateSortActionBasedOnCUPageEnablement(
        isEnableCameraUploadPageShowing: Boolean,
        mediaSource: FilterMediaSource,
        isCUPageEnabled: Boolean,
    ) {
        if (isCUPageEnabled && mediaSource != FilterMediaSource.CloudDrive) {
            disableSortToolbarMenuAction()
        } else {
            updateSortActionEnablement(
                isEnableCameraUploadPageShowing = isEnableCameraUploadPageShowing,
                mediaSource = mediaSource,
            )
        }
    }

    /**
     * Enables the sort action when there is content to sort, unless the Camera Uploads promo page is
     * showing for a non-Cloud-Drive source. Uses the revamp's loaded-state as the empty signal in
     * place of the tab's `displayedPhotos.isEmpty()`.
     */
    internal fun updateSortActionEnablement(
        isEnableCameraUploadPageShowing: Boolean,
        mediaSource: FilterMediaSource,
    ) {
        val hasNoContent = uiState.value !is TimelineRevampUiState.Data
        if (hasNoContent || (isEnableCameraUploadPageShowing && mediaSource != FilterMediaSource.CloudDrive)) {
            disableSortToolbarMenuAction()
        } else {
            enableSortToolbarMenuAction()
        }
    }

    private fun disableSortToolbarMenuAction() {
        sortActionEnabledFlow.update { false }
    }

    private fun enableSortToolbarMenuAction() {
        sortActionEnabledFlow.update { true }
    }

    private fun trackGridSizeSelection(gridSize: TimelineGridSize) {
        val event = when (gridSize) {
            TimelineGridSize.Compact -> MediaScreenGridSizeCompactSelectedEvent
            TimelineGridSize.Default -> MediaScreenGridSizeDefaultSelectedEvent
            TimelineGridSize.Large -> MediaScreenGridSizeLargeSelectedEvent
        }
        Analytics.tracker.trackEvent(event)
    }

    /**
     * Persists the chosen filter to the account-level preferences and applies it to the current
     * session; [currentFilter] then re-derives and the timeline re-loads.
     */
    internal fun onFilterChange(request: TimelineFilterRequest) {
        viewModelScope.launch {
            runCatching {
                mapOf(
                    TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to request.isRemembered.toString(),
                    TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to request.mediaType.toMediaTypeValue(),
                    TimelinePreferencesJSON.JSON_KEY_LOCATION.value to request.mediaSource.toLocationValue(),
                ).also { setTimelineFilterPreferencesUseCase(it) }
            }.onSuccess { newPreferences ->
                selectedFilterFlow.update { newPreferences }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Handles a tap on a timeline item. Taken-down nodes must not be opened, so they trigger the
     * dispute dialog instead of being previewed.
     *
     * Opening non-taken-down items is intentionally not wired yet — when the revamp implements
     * tap-to-open, add the open call in the else branch; the taken-down guard is already in place.
     */
    fun onNodeClicked(node: PhotosNodeContentItemV2?) {
        if (node?.isTakenDown == true) {
            _takenDownDialogEvent.update { triggered }
        } else {
            // TODO: open the node in the viewer once the revamp wires tap-to-open.
        }
    }

    /**
     * Marks the taken-down dispute dialog event as handled.
     */
    fun onTakenDownDialogEventConsumed() {
        _takenDownDialogEvent.update { consumed }
    }

    private companion object {
        private const val MAX_CACHED_ITEMS = 480
        private const val SCROLL_SETTLE_DEBOUNCE_MS = 500L

        /**
         * The filter [currentFilter] starts with. Its options become user-controllable in later
         * steps; until then the timeline always loads with these defaults.
         */
        private val DEFAULT_FILTER = MediaTimelineFilter(
            granularity = Granularity.Day,
            category = Category.All,
            location = Location.CloudDriveAndVault,
            sensitivity = Sensitivity.ShowAll,
        )
    }
}
