package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
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
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.GetNodeListByIdsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.SignalMediaCountChangesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.TimelineDateCache
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFormatters
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabNormalModeActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampViewModel.Companion.MAX_CACHED_ITEMS
import mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper.MediaTimelineNodeUiItemMapper
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeCompactSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeDefaultSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeLargeSelectedEvent
import timber.log.Timber
import java.time.Year
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the Timeline Revamp screen.
 *
 * Sections come from [GetMediaTimelineSectionsUseCase] and lay out the grid (a header plus
 * `count` slots each). Each section is paginated **independently** through
 * [ListMediaNodesByOffsetUseCase] (its query is scoped to the section's date range), so a deep section
 * loads directly without paging through the ones before it. Loaded items are cached by section identity
 * (group id + section-local index) with a bounded LRU, and only the currently visible window is fetched.
 *
 * Media added or removed mid-session triggers a silent refresh (see [monitorTimelineSections]):
 * sections are re-fetched without a loading state and the cache is reconciled section by section.
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
    private val getNodeListByIdsUseCase: GetNodeListByIdsUseCase,
    private val signalMediaCountChangesUseCase: SignalMediaCountChangesUseCase,
) : ViewModel() {

    private val sectionsState = MutableStateFlow<SectionsState>(SectionsState.Loading)
    private val loadedNodes = MutableStateFlow<Map<Int, PhotosNodeContentItemV2>>(emptyMap())
    private val targetRange = MutableStateFlow(IntRange.EMPTY)
    private val isScrollingFlow = MutableStateFlow(false)
    private val mediaLoaderStarted = AtomicBoolean(false)
    private val sectionsMonitorStarted = AtomicBoolean(false)

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
     * The selected media time period (All / Months / Years). Drives which view is shown — the flat
     * day-level grid ([MediaTimePeriod.All], and [MediaTimePeriod.Days] which is treated the same) or a
     * Year / Month summary-card list — rather than re-bucketing the grid.
     */
    private val selectedTimePeriodFlow = MutableStateFlow(MediaTimePeriod.All)
    val selectedTimePeriod: StateFlow<MediaTimePeriod> = selectedTimePeriodFlow.asStateFlow()

    /**
     * The currently-selected media resolved to [TypedNode]s, for the selection-mode node actions.
     * Unlike the tab (which filters an in-memory list), the revamp fetches them by id on demand.
     */
    private val _selectedPhotosInTypedNodesFlow = MutableStateFlow<List<TypedNode>>(emptyList())
    internal val selectedPhotosInTypedNodesFlow: StateFlow<List<TypedNode>> =
        _selectedPhotosInTypedNodesFlow.asStateFlow()

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
     * when the hidden-nodes feature is enabled and "show hidden items" is off (matching the tab). The
     * grid is always grouped at day granularity; the Year / Month card lists are queried separately.
     * Eagerly shared so [fetchSectionNodes] can read the current value synchronously.
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
     * Global start slot of each section, keyed by [MediaTimelineSection.groupId] ([totalSlots] is the
     * total). Lets [getAndUpdateLoadedNodes] map a cache entry to its global index. Rebuilt on section change.
     */
    private var sectionStartByGroupId: Map<String, Int> = emptyMap()

    private var totalSlots = 0

    /**
     * Loaded items keyed by [SlotKey], with an LRU bound of [MAX_CACHED_ITEMS]. Only mutated by the
     * media-loader coroutine.
     */
    private val mediaCache = object : LinkedHashMap<SlotKey, PhotosNodeContentItemV2>() {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<SlotKey, PhotosNodeContentItemV2>,
        ): Boolean = size > MAX_CACHED_ITEMS
    }

    /**
     * Emits when a media node changes in a way that can alter the timeline sections. Debounced +
     * conflated so a burst (e.g. a Camera Uploads run) collapses into one refresh, and shared so the
     * grid and period cards use one monitor.
     */
    @OptIn(FlowPreview::class)
    private val monitorMediaChanges: Flow<Unit> by lazy(LazyThreadSafetyMode.NONE) {
        signalMediaCountChangesUseCase()
            .debounce(MEDIA_CHANGE_DEBOUNCE_MS.milliseconds)
            .conflate()
            .catch { Timber.e(it, "Failed to monitor media node updates") }
            .shareIn(viewModelScope, SharingStarted.Lazily, replay = 0)
    }

    val uiState: StateFlow<TimelineRevampUiState> by lazy(LazyThreadSafetyMode.NONE) {
        val gridFlow = combine(
            sectionsState.onStart { monitorTimelineSections() },
            loadedNodes,
            isHiddenNodesEnabled,
            gridSizeFlow,
            sortOptionsFlow,
        ) { sectionsState, loaded, hiddenNodesEnabled, gridSize, currentSort ->
            GridBundle(sectionsState, loaded, hiddenNodesEnabled, gridSize, currentSort)
        }

        combine(
            gridFlow,
            selectedTimePeriodFlow,
            periodCards,
            _takenDownDialogEvent,
        ) { grid, period, cards, takenDownDialogEvent ->
            when (val sectionsState = grid.sectionsState) {
                SectionsState.Loading -> TimelineRevampUiState.Loading
                is SectionsState.Loaded ->
                    toUiState(
                        sections = sectionsState.sections,
                        loaded = grid.loadedNodes,
                        isHiddenNodesEnabled = grid.isHiddenNodesEnabled,
                        gridSize = grid.gridSize,
                        currentSort = grid.currentSort,
                        selectedPeriod = period,
                        periodCards = cards.forPeriod(period),
                    ).let { state ->
                        // Inject the taken-down dialog event so the screen observes a single object.
                        if (state is TimelineRevampUiState.Data) {
                            state.copy(takenDownDialogEvent = takenDownDialogEvent)
                        } else {
                            state
                        }
                    }
            }
        }
            .catch { e -> Timber.e(e, "Failed to load media timeline sections") }
            .asUiStateFlow(
                viewModelScope,
                TimelineRevampUiState.Loading,
            )
    }

    /**
     * The Year and Month summary cards (with their thumbnails). Built lazily the first time a card
     * period is selected for the current [currentFilter], then reused while that filter/sort holds —
     * toggling between periods does not rebuild them. Rebuilt when the filter or sort changes, and
     * (like the grid) when media is added or removed, via [monitorMediaChanges].
     *
     * The Year and Month queries are independent, so they run concurrently.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodCards: StateFlow<PeriodCards> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            currentFilter,
            sortOptionsFlow,
            monitorMediaChanges.onStart { emit(Unit) },
        ) { filter, sort, _ -> filter to sort }
            .flatMapLatest { (filter, sort) ->
                selectedTimePeriodFlow
                    .map { it.isCardPeriod() }
                    .distinctUntilChanged()
                    .filter { it }
                    .take(1)
                    .map {
                        coroutineScope {
                            val yearCards = async {
                                buildPeriodCards(
                                    period = MediaTimePeriod.Years,
                                    filter = filter.copy(granularity = Granularity.Year),
                                    order = sort.sortOrder,
                                )
                            }
                            val monthCards = async {
                                buildPeriodCards(
                                    period = MediaTimePeriod.Months,
                                    filter = filter.copy(granularity = Granularity.Month),
                                    order = sort.sortOrder,
                                )
                            }
                            PeriodCards(
                                yearCards = yearCards.await(),
                                monthCards = monthCards.await(),
                            )
                        }
                    }
                    .onStart { emit(PeriodCards.Empty) }
            }
            .asUiStateFlow(viewModelScope, PeriodCards.Empty)
    }

    /**
     * Builds the summary cards for [period] by querying the timeline sections at the matching
     * granularity and resolving one representative node (for the thumbnail) per section. Returns an
     * empty list for periods without a card view.
     */
    private suspend fun buildPeriodCards(
        period: MediaTimePeriod,
        filter: MediaTimelineFilter,
        order: SortOrder,
    ): List<PhotosNodeListCard> {
        val periodSections = runCatching {
            getMediaTimelineSectionsUseCase(filter, order)
        }.getOrDefault(emptyList())

        return periodSections.mapNotNull { section ->
            val representativeNode = runCatching {
                listMediaNodesByOffsetUseCase(
                    filter = filter,
                    section = section,
                    order = order,
                    maxElements = 1,
                    offset = 0L,
                )
            }.getOrNull()
                ?.firstOrNull()
                ?: return@mapNotNull null

            buildPeriodCard(period, section, representativeNode)
        }
    }

    private fun buildPeriodCard(
        period: MediaTimePeriod,
        section: MediaTimelineSection,
        node: TypedFileNode,
    ): PhotosNodeListCard {
        val zonedDateTime = TimelineDateCache.get(node.modificationTime)
        val isCurrentYear = zonedDateTime.year == Year.now().value
        val cardPeriod: PhotosNodeListCardPeriod
        val formattedDate: String
        when (period) {
            MediaTimePeriod.Years -> {
                cardPeriod = PhotosNodeListCardPeriod.Year
                formattedDate = TimelineFormatters.year.format(zonedDateTime)
            }

            else -> {
                cardPeriod = PhotosNodeListCardPeriod.Month
                formattedDate = if (isCurrentYear) {
                    TimelineFormatters.month.format(zonedDateTime)
                } else {
                    TimelineFormatters.monthYear.format(zonedDateTime)
                }
            }
        }
        return PhotosNodeListCard(
            period = cardPeriod,
            key = node.id.longValue,
            id = node.id.longValue,
            day = zonedDateTime.dayOfMonth,
            month = zonedDateTime.monthValue,
            year = zonedDateTime.year,
            formattedDate = formattedDate,
            thumbnailFilePath = node.thumbnailPath,
            previewFilePath = node.previewPath,
            extension = node.type.extension,
            isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
            count = section.count.toInt(),
        )
    }

    private fun MediaTimePeriod.isCardPeriod(): Boolean =
        this == MediaTimePeriod.Years || this == MediaTimePeriod.Months

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
     * Starts the single coroutine driving [sectionsState] (once, on first [uiState] subscription).
     *
     * A filter or sort change resets to [SectionsState.Loading] and clears the loaded media before
     * publishing the new sections. A [monitorMediaChanges] emission instead refreshes silently — no loading
     * state, held until the grid stops scrolling ([awaitScrollIdle]) so the layout never reflows mid-scroll.
     */
    private fun monitorTimelineSections() {
        if (!sectionsMonitorStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            combine(currentFilter, sortOptionsFlow) { filter, sort -> filter to sort }
                .collectLatest { (filter, sort) ->
                    loadSections(filter, sort.sortOrder)
                    refreshSectionsOnMediaChange(filter, sort.sortOrder)
                }
        }
    }

    /**
     * Loads the sections for [filter]/[order] from scratch, showing the loading state first.
     */
    private suspend fun loadSections(filter: MediaTimelineFilter, order: SortOrder) {
        sectionsState.update { SectionsState.Loading }
        loadedNodes.update { emptyMap() }
        val loaded = getTimelineSections(filter, order)
        sectionsState.update { SectionsState.Loaded(loaded) }
    }

    /**
     * On every media change, silently re-fetches the sections and applies them once the grid is idle.
     * Never returns; cancelled by [monitorTimelineSections] when the filter/sort changes.
     */
    private suspend fun refreshSectionsOnMediaChange(filter: MediaTimelineFilter, order: SortOrder) {
        monitorMediaChanges.collectLatest {
            val refreshed = getTimelineSections(filter, order)
            if (refreshed != sectionsState.value.sectionsOrEmpty()) {
                Timber.d("TimelineRevamp::awaitIdle")
                awaitScrollIdle()
                sectionsState.update { SectionsState.Loaded(refreshed) }
                Timber.d("TimelineRevamp::refreshed")
            }
        }
    }

    /**
     * The currently-loaded sections, or empty while still loading.
     */
    private fun SectionsState.sectionsOrEmpty(): List<MediaTimelineSection> =
        (this as? SectionsState.Loaded)?.sections.orEmpty()

    /**
     * Suspends until the grid is not being scrolled, so a silent refresh's re-layout lands only once
     * the user is at rest. Returns immediately when already idle.
     */
    private suspend fun awaitScrollIdle() {
        isScrollingFlow.first { !it }
    }

    /**
     * Reports the grid's scroll state from the screen, gating silent refreshes (see [awaitScrollIdle]).
     */
    fun onScrollingChanged(isScrolling: Boolean) {
        isScrollingFlow.update { isScrolling }
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
        selectedPeriod: MediaTimePeriod,
        periodCards: List<PhotosNodeListCard>,
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
                selectedPeriod = selectedPeriod,
                periodCards = periodCards,
            )
        }

    /**
     * Called by the screen when the set of visible media slots changes.
     *
     * @param firstIndex the first visible global media index
     * @param lastIndex the last visible global media index
     */
    fun onVisibleRangeChanged(firstIndex: Int, lastIndex: Int) {
        if (firstIndex !in 0..lastIndex) return
        val loadBuffer = (lastIndex - firstIndex).coerceAtLeast(MIN_LOAD_BUFFER)
        targetRange.update {
            (firstIndex - loadBuffer)
                .coerceAtLeast(0)..(lastIndex + loadBuffer)
        }
        ensureMediaLoaderStarted()
    }

    private fun ensureMediaLoaderStarted() {
        if (!mediaLoaderStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            combine(
                targetRange,
                sectionsState.map { it.sectionsOrEmpty() }.distinctUntilChanged(),
            ) { range, currentSections -> range to currentSections }
                .collectLatest { (range, newSections) ->
                    updateMediaCache(newSections)
                    delay(SCROLL_SETTLE_DEBOUNCE_MS.milliseconds)
                    ensureRangeLoaded(range, newSections)
                }
        }
    }

    private fun updateMediaCache(newSections: List<MediaTimelineSection>) {
        if (newSections == cachedSections) return
        val previousCounts = cachedSections
            ?.associate { it.groupId to it.count.toInt() }
            .orEmpty()
        val newCounts = newSections.associate { it.groupId to it.count.toInt() }
        mediaCache.keys.removeAll { slot ->
            val newCount = newCounts[slot.groupId]
            newCount == null ||
                    newCount != previousCounts[slot.groupId] ||
                    slot.localIndex >= newCount
        }
        cachedSections = newSections
        updateSectionOffsets(newSections)
        loadedNodes.update { getAndUpdateLoadedNodes() }
    }

    private fun updateSectionOffsets(sections: List<MediaTimelineSection>) {
        val offsets = sectionStartOffsetsOf(sections)
        sectionStartByGroupId = sections.indices.associate { sections[it].groupId to offsets[it] }
        totalSlots = sections.sumOf { it.count }.toInt()
    }

    private fun getAndUpdateLoadedNodes(): Map<Int, PhotosNodeContentItemV2> {
        if (mediaCache.isEmpty()) return emptyMap()
        val loadedNodes = HashMap<Int, PhotosNodeContentItemV2>(mediaCache.size)
        for ((slot, item) in mediaCache) {
            val base = sectionStartByGroupId[slot.groupId] ?: continue
            loadedNodes[base + slot.localIndex] = item
        }
        return loadedNodes
    }

    private fun sectionStartOffsetsOf(sections: List<MediaTimelineSection>): List<Int> {
        var start = 0
        return sections.map { section ->
            start.also { start += section.count.toInt() }
        }
    }

    /**
     * Loads the media for the visible slots in [range]. Walks the sections with a running start offset
     * and loads the visible slice of each section overlapping the window, stopping once past it.
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

        var sectionStartOffset = 0
        for (section in sections) {
            if (sectionStartOffset > visibleTo) break // every later section is past the visible window
            val slotCount = section.count.toInt()
            val sectionEnd = sectionStartOffset + slotCount - 1
            if (sectionEnd >= visibleFrom) {
                // The part of this section inside the window, in its own (section-local) indices.
                val localWindowStart = (visibleFrom - sectionStartOffset).coerceAtLeast(0)
                val localWindowEnd = (visibleTo - sectionStartOffset).coerceAtMost(slotCount - 1)
                loadSectionWindow(
                    section = section,
                    localWindowStart = localWindowStart,
                    localWindowEnd = localWindowEnd,
                )
            }
            sectionStartOffset += slotCount
        }
    }

    /**
     * Loads the section-local window `[localWindowStart, localWindowEnd]` of [section]: locates the
     * contiguous run of slots not yet cached, fetches just that run, and caches it. Already-cached
     * slots are never re-requested, and only the window — never the whole section — is materialized.
     */
    private suspend fun loadSectionWindow(
        section: MediaTimelineSection,
        localWindowStart: Int,
        localWindowEnd: Int,
    ) {
        val uncachedRange = findUncachedRange(
            groupId = section.groupId,
            localWindowStart = localWindowStart,
            localWindowEnd = localWindowEnd,
        ) ?: return // the whole window is already cached

        val nodes = fetchSectionNodes(section, uncachedRange) ?: return
        cacheSectionNodes(section, uncachedRange.first, nodes)
    }

    /**
     * The contiguous run of section-local slots within `[localWindowStart, localWindowEnd]` that are
     * not yet in [mediaCache], or null when every slot in the window is already cached.
     */
    private fun findUncachedRange(
        groupId: String,
        localWindowStart: Int,
        localWindowEnd: Int,
    ): IntRange? {
        var firstUncached = -1
        var lastUncached = -1
        for (localIndex in localWindowStart..localWindowEnd) {
            if (SlotKey(groupId, localIndex) !in mediaCache) {
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
     * Caches [nodes] against [section] — the run starts at section-local [localStart] — and publishes
     * the re-projected cache to the UI.
     */
    private fun cacheSectionNodes(
        section: MediaTimelineSection,
        localStart: Int,
        nodes: List<TypedFileNode>,
    ) {
        if (nodes.isEmpty()) return
        nodes.forEachIndexed { i, node ->
            mediaCache[SlotKey(section.groupId, localStart + i)] =
                mediaTimelineNodeUiItemMapper(node)
        }
        loadedNodes.update { getAndUpdateLoadedNodes() }
    }

    /**
     * Updates the selected media time period, switching between the flat day-level grid and the
     * Year / Month summary-card views.
     */
    fun onMediaTimePeriodSelected(value: MediaTimePeriod) {
        selectedTimePeriodFlow.update { value }
    }

    /**
     * Resolves the given selected media ids to [TypedNode]s (fetched by id, since the revamp does not
     * hold all nodes in memory), publishes them to [selectedPhotosInTypedNodesFlow], and returns them.
     */
    internal suspend fun retrieveTypedNodeFromSelection(selectedIds: Set<Long>): List<TypedNode> {
        val nodes = runCatching { getNodeListByIdsUseCase(selectedIds.map { NodeId(it) }) }
            .onFailure { Timber.e(it, "Failed to resolve selected nodes") }
            .getOrDefault(emptyList())
        _selectedPhotosInTypedNodesFlow.update { nodes }
        return nodes
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

    /**
     * The loading state of the timeline sections for the current filter, kept separate from the
     * per-node [loadedNodes] so a filter change can reset to loading immediately.
     */
    private sealed interface SectionsState {
        data object Loading : SectionsState
        data class Loaded(
            val sections: List<MediaTimelineSection>,
        ) : SectionsState
    }

    private data class GridBundle(
        val sectionsState: SectionsState,
        val loadedNodes: Map<Int, PhotosNodeContentItemV2>,
        val isHiddenNodesEnabled: Boolean,
        val gridSize: TimelineGridSize,
        val currentSort: TimelineTabSortOptions,
    )

    private data class PeriodCards(
        val yearCards: List<PhotosNodeListCard>,
        val monthCards: List<PhotosNodeListCard>,
    ) {
        fun forPeriod(period: MediaTimePeriod): List<PhotosNodeListCard> = when (period) {
            MediaTimePeriod.Years -> yearCards
            MediaTimePeriod.Months -> monthCards
            else -> emptyList()
        }

        companion object {
            val Empty = PeriodCards(emptyList(), emptyList())
        }
    }

    /**
     * A media slot keyed by its section ([MediaTimelineSection.groupId]) and section-local position.
     * Keying the cache this way (not by global index) keeps a thumbnail valid when another section
     * shifts its global index, so an insertion elsewhere doesn't flash the grid back to shimmer.
     */
    private data class SlotKey(val groupId: String, val localIndex: Int)

    private companion object {
        private const val MAX_CACHED_ITEMS = 480
        private const val SCROLL_SETTLE_DEBOUNCE_MS = 250L
        private const val MEDIA_CHANGE_DEBOUNCE_MS = 1_000L
        private const val MIN_LOAD_BUFFER = 24

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
