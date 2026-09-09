package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.getAndUpdate
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
import mega.privacy.android.domain.usecase.photos.MonitorMediaNodeContentChangesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineGridSizeUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineGridSizeUseCase
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
    private val monitorTimelineGridSizeUseCase: MonitorTimelineGridSizeUseCase,
    private val setTimelineGridSizeUseCase: SetTimelineGridSizeUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    private val getCameraUploadFolderHandlesUseCase: GetCameraUploadFolderHandlesUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getNodeListByIdsUseCase: GetNodeListByIdsUseCase,
    private val signalMediaCountChangesUseCase: SignalMediaCountChangesUseCase,
    private val monitorMediaNodeContentChangesUseCase: MonitorMediaNodeContentChangesUseCase,
) : ViewModel() {

    private val sectionsState = MutableStateFlow<SectionsState>(SectionsState.Loading)
    private val loadedNodes = MutableStateFlow<Map<Int, PhotosNodeContentItemV2>>(emptyMap())
    private val targetRange = MutableStateFlow(IntRange.EMPTY)
    private val isScrollingFlow = MutableStateFlow(false)
    private val mediaLoaderStarted = AtomicBoolean(false)
    private val sectionsMonitorStarted = AtomicBoolean(false)
    private val contentMonitorStarted = AtomicBoolean(false)
    private val gridSizeMonitorStarted = AtomicBoolean(false)

    /**
     * Backs [TimelineRevampUiState.Data.takenDownDialogEvent]; folded into [uiState] so the screen
     * observes a single state object.
     */
    private val _takenDownDialogEvent = MutableStateFlow<StateEvent>(consumed)

    /**
     * The grid size (Compact / Default / Large). Drives only the grid column count, so it lives in
     * [uiState] (not [currentFilter]) — changing it must not reload the timeline.
     *
     * [onGridSizeChange] writes it optimistically instead of waiting for the persisted preference to
     * round trip, so the successive steps of a single pinch compose rather than each reading the
     * same pre-pinch size and collapsing into one. Seeded from the preference by
     * [monitorTimelineGridSize].
     */
    private val gridSizeFlow = MutableStateFlow(TimelineGridSize.Default)

    /**
     * The selected timestamp column and direction. Drives the section order and the per-section page
     * order; changing it reloads the timeline (via [monitorTimelineSections]).
     */
    private val sortOptionsFlow = MutableStateFlow(TimelineRevampSortConfiguration.Default)

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

    /**
     * The ids of the selected media. Owned here rather than by the composition so the selection, and
     * the typed nodes resolved from it, survive a configuration change without a saved-state round
     * trip — which for a selection of thousands is slow and re-resolves every node.
     */
    private val selectedIdsFlow = MutableStateFlow<PersistentSet<Long>>(persistentSetOf())

    /**
     * Cells selected while their node was not loaded, keyed by slot: null until a fetch resolves the
     * cell, its node id afterwards so the cell can still be deselected while outside the loaded window.
     * Kept by slot (not global index) so a section change elsewhere doesn't shift the marks.
     */
    private val pendingSelectionFlow =
        MutableStateFlow<PersistentMap<SlotKey, Long?>>(persistentMapOf())
    private val selectionResolverStarted = AtomicBoolean(false)

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
        }
            .catch { e ->
                Timber.e(e, "Failed to map timeline filter ui state")
                emit(TimelineFilterUiState())
            }
            .asUiStateFlow(viewModelScope, TimelineFilterUiState())
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
        }
            .catch { e ->
                Timber.e(e, "Failed to derive current filter")
                emit(DEFAULT_FILTER)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_FILTER)
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
            gridSizeFlow.onStart { monitorTimelineGridSize() },
            sortOptionsFlow,
        ) { sectionsState, loaded, hiddenNodesEnabled, gridSize, currentSort ->
            GridBundle(sectionsState, loaded, hiddenNodesEnabled, gridSize, currentSort)
        }

        val selectionFlow = combine(
            selectedIdsFlow.onStart { ensureSelectionResolverStarted() },
            pendingSelectionFlow,
        ) { ids, pending -> Selection(ids, pending) }

        combine(
            gridFlow,
            selectedTimePeriodFlow,
            periodCards,
            selectionFlow,
            _takenDownDialogEvent,
        ) { grid, period, cards, selection, takenDownDialogEvent ->
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
                        arePeriodCardsLoading = cards.isLoading,
                        selection = selection,
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
                    .onStart { emit(PeriodCards.Loading) }
            }
            .catch { e ->
                Timber.e(e, "Failed to build period cards")
                emit(PeriodCards.Empty)
            }
            .asUiStateFlow(viewModelScope, PeriodCards.Loading)
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

    /**
     * The card's date comes from the section, not from the representative node: the section is the
     * capture-time bucket the grid headers and the card-to-grid navigation both key on, whereas the
     * node's modification time can fall in a different month entirely.
     */
    private fun buildPeriodCard(
        period: MediaTimePeriod,
        section: MediaTimelineSection,
        node: TypedFileNode,
    ): PhotosNodeListCard {
        val zonedDateTime = TimelineDateCache.get(section.startDate)
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
    /**
     * Seeds [gridSizeFlow] from the persisted preference and keeps it in sync with changes made
     * elsewhere (e.g. the legacy Timeline tab). Started from [uiState] rather than eagerly, so the
     * preference is only read once something observes the grid.
     */
    private fun monitorTimelineGridSize() {
        if (!gridSizeMonitorStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            monitorTimelineGridSizeUseCase()
                .map { ordinal ->
                    ordinal?.let { TimelineGridSize.entries.getOrNull(it) }
                        ?: TimelineGridSize.Default
                }
                .catch {
                    Timber.e(it, "Unable to monitor timeline grid size")
                    emit(TimelineGridSize.Default)
                }
                .collect { gridSizeFlow.value = it }
        }
    }

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
            .distinctBy { it.groupId }

    private fun toUiState(
        sections: List<MediaTimelineSection>,
        loaded: Map<Int, PhotosNodeContentItemV2>,
        isHiddenNodesEnabled: Boolean,
        gridSize: TimelineGridSize,
        currentSort: TimelineRevampSortConfiguration,
        selectedPeriod: MediaTimePeriod,
        periodCards: List<PhotosNodeListCard>,
        arePeriodCardsLoading: Boolean,
        selection: Selection,
    ): TimelineRevampUiState =
        if (sections.isEmpty()) {
            TimelineRevampUiState.Empty
        } else {
            val sectionStartOffsets = sectionStartOffsetsOf(sections)
            TimelineRevampUiState.Data(
                sections = sections,
                sectionStartOffsets = sectionStartOffsets,
                loadedNodes = loaded,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                gridSize = gridSize,
                currentSort = currentSort,
                selectedPeriod = selectedPeriod,
                periodCards = periodCards,
                arePeriodCardsLoading = arePeriodCardsLoading,
                selection = selection.toTimelineSelection(sections, sectionStartOffsets),
            )
        }

    private fun Selection.toTimelineSelection(
        sections: List<MediaTimelineSection>,
        sectionStartOffsets: List<Int>,
    ): TimelineSelection {
        val offsetByGroupId = offsetByGroupIdOf(sections, sectionStartOffsets)
        return TimelineSelection(
            selectedIds = ids,
            pendingIndices = persistentHashSetOf<Int>().mutate { indices ->
                pending.keys.forEach { slot ->
                    offsetByGroupId[slot.groupId]?.let { indices += it + slot.localIndex }
                }
            },
            count = ids.size + pending.values.count { it == null },
        )
    }

    private fun offsetByGroupIdOf(
        sections: List<MediaTimelineSection>,
        sectionStartOffsets: List<Int>,
    ): Map<String, Int> =
        sections.indices.associate { sections[it].groupId to sectionStartOffsets[it] }

    /**
     * The slot at global [index] in the displayed [layout], or null when the index is out of range
     * (e.g. the sections changed under a stale gesture).
     */
    private fun slotKeyOf(index: Int, layout: TimelineRevampUiState.Data): SlotKey? {
        val offsets = layout.sectionStartOffsets
        if (index < 0 || offsets.isEmpty()) return null
        var sectionIndex = offsets.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        if (sectionIndex < 0) return null
        // Sections without slots share their start offset with the next one; take the last match.
        while (sectionIndex + 1 < offsets.size && offsets[sectionIndex + 1] <= index) sectionIndex++
        val section = layout.sections[sectionIndex]
        val localIndex = index - offsets[sectionIndex]
        return if (localIndex < section.count) SlotKey(section.groupId, localIndex) else null
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
        ensureContentMonitorStarted()
    }

    /**
     * Loads and returns the media in the given global slot range, keyed by slot. Used by the section
     * select-all and the drag-select resolution, which need every node of the range, on-screen or
     * not. The fetched nodes are deliberately not cached: a range can exceed the LRU cache capacity,
     * where caching would evict the visible window and lose part of the range before it could ever
     * be observed through [TimelineRevampUiState.Data.loadedNodes].
     *
     * @param firstIndex the first global media index of the range
     * @param lastIndex the last global media index of the range
     */
    private suspend fun loadMediaSlots(
        firstIndex: Int,
        lastIndex: Int,
    ): Map<SlotKey, PhotosNodeContentItemV2> {
        if (firstIndex !in 0..lastIndex) return emptyMap()
        val result = HashMap<SlotKey, PhotosNodeContentItemV2>()
        var sectionStartOffset = 0
        for (section in sectionsState.value.sectionsOrEmpty()) {
            if (sectionStartOffset > lastIndex) break
            val slotCount = section.count.toInt()
            val sectionEnd = sectionStartOffset + slotCount - 1
            if (sectionEnd >= firstIndex) {
                val localWindowStart = (firstIndex - sectionStartOffset).coerceAtLeast(0)
                val localWindowEnd = (lastIndex - sectionStartOffset).coerceAtMost(slotCount - 1)
                fetchSectionWindowItems(
                    section = section,
                    localWindowStart = localWindowStart,
                    localWindowEnd = localWindowEnd,
                ).forEach { (localIndex, item) ->
                    result[SlotKey(section.groupId, localIndex)] = item
                }
            }
            sectionStartOffset += slotCount
        }
        return result
    }

    /**
     * The items of the section-local window `[localWindowStart, localWindowEnd]`: taken from the
     * cache where already loaded, with the rest fetched — without caching (see [loadMediaSlots]).
     */
    private suspend fun fetchSectionWindowItems(
        section: MediaTimelineSection,
        localWindowStart: Int,
        localWindowEnd: Int,
    ): Map<Int, PhotosNodeContentItemV2> {
        val windowItems = HashMap<Int, PhotosNodeContentItemV2>()
        for (localIndex in localWindowStart..localWindowEnd) {
            mediaCache[SlotKey(section.groupId, localIndex)]?.let { windowItems[localIndex] = it }
        }

        val uncachedRange = findUncachedRange(
            groupId = section.groupId,
            localWindowStart = localWindowStart,
            localWindowEnd = localWindowEnd,
        ) ?: return windowItems // the whole window is already cached

        val nodes = fetchSectionNodes(section, uncachedRange) ?: return windowItems
        nodes.forEachIndexed { i, node ->
            windowItems[uncachedRange.first + i] = mediaTimelineNodeUiItemMapper(node)
        }
        return windowItems
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

    /**
     * Starts the single coroutine (once) that patches cached items in place when a media node's
     * render-affecting content (favourite / sensitivity) changes, so the affected thumbnails re-render
     * without a section reload or a shimmer.
     */
    private fun ensureContentMonitorStarted() {
        if (!contentMonitorStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            monitorMediaNodeContentChangesUseCase().collect(::replaceAndUpdateCache)
        }
    }

    /**
     * Re-fetches the changed nodes we currently hold, re-maps them, and replaces their cache entries in
     * place (same [SlotKey]); nodes not in the cache are ignored. Runs on the same dispatcher as the
     * media loader, so cache access stays confined to one thread.
     */
    private suspend fun replaceAndUpdateCache(targetNodeIds: Set<Long>) {
        val cachedIds = mediaCache.values.mapTo(mutableSetOf()) { it.id }
        val idsToPatch = targetNodeIds intersect cachedIds
        if (idsToPatch.isEmpty()) return

        val refreshedById = runCatching { getNodeListByIdsUseCase(idsToPatch.map { NodeId(it) }) }
            .onFailure { Timber.e(it, "Failed to refresh changed media nodes") }
            .getOrDefault(emptyList())
            .filterIsInstance<TypedFileNode>()
            .associateBy { it.id.longValue }
        if (refreshedById.isEmpty()) return

        val replacements = mediaCache.entries
            .mapNotNull { (slot, item) ->
                refreshedById[item.id]?.let { slot to mediaTimelineNodeUiItemMapper(it) }
            }
        if (replacements.isEmpty()) return
        replacements.forEach { (slot, item) -> mediaCache[slot] = item }
        loadedNodes.update { getAndUpdateLoadedNodes() }
        reconcilePendingSelection()
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
        pendingSelectionFlow.update { pending ->
            val stale = pending.keys.filter { slot -> (newCounts[slot.groupId] ?: 0) <= slot.localIndex }
            if (stale.isEmpty()) pending else pending.mutate { it.keys.removeAll(stale) }
        }
        cachedSections = newSections
        updateSectionOffsets(newSections)
        loadedNodes.update { getAndUpdateLoadedNodes() }
        reconcilePendingSelection()
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
        reconcilePendingSelection()
    }

    /**
     * Updates the selected media time period, switching between the flat day-level grid and the
     * Year / Month summary-card views.
     */
    fun onMediaTimePeriodSelected(value: MediaTimePeriod) {
        selectedTimePeriodFlow.update { value }
    }

    /**
     * Toggles the selection of the media with [id] (a long-press, or a tap while in selection mode).
     */
    fun toggleSelection(id: Long) {
        selectedIdsFlow.update { if (id in it) it.remove(id) else it.add(id) }
    }

    /**
     * Selects or deselects the cell at global [index] as a drag-select sweep crosses it. A cell whose
     * node is not loaded is marked pending instead (see [TimelineSelection.pendingIndices]) and is
     * resolved by [resolvePendingSelection] once the drag ends.
     */
    fun setCellSelected(index: Int, selected: Boolean) {
        val layout = uiState.value as? TimelineRevampUiState.Data ?: return
        val node = loadedNodes.value[index]
        if (node == null) {
            val slot = slotKeyOf(index, layout) ?: return
            if (selected) markPending(listOf(slot)) else unmarkPending(listOf(slot))
        } else if (!node.isTakenDown) {
            selectedIdsFlow.update { if (selected) it.add(node.id) else it.remove(node.id) }
        }
    }

    /**
     * Selects every cell of [range] (a month, from its header checkbox), or deselects them all when
     * they are all selected already. Loaded cells toggle right away; the others are marked pending and
     * resolved from a fetch of the range that runs in [viewModelScope], so a configuration change
     * mid-fetch neither cancels it nor loses the marks.
     */
    fun toggleRangeSelection(range: IntRange) {
        val layout = uiState.value as? TimelineRevampUiState.Data ?: return
        val selectAll = !isRangeSelected(range, layout)
        val unloadedSlots = mutableListOf<SlotKey>()
        val loadedIds = mutableListOf<Long>()
        val loaded = loadedNodes.value
        for (index in range) {
            val node = loaded[index]
            when {
                node == null -> slotKeyOf(index, layout)?.let(unloadedSlots::add)
                !node.isTakenDown -> loadedIds += node.id
            }
        }
        if (selectAll) {
            markPending(unloadedSlots)
            selectedIdsFlow.update { it.addAll(loadedIds) }
        } else {
            unmarkPending(unloadedSlots)
            selectedIdsFlow.update { it.removeAll(loadedIds) }
        }
        if (unloadedSlots.isEmpty()) return
        viewModelScope.launch {
            val resolved = loadMediaSlots(range.first, range.last)
            if (selectAll) {
                selectResolvedPending(resolved)
            } else {
                // Selected cells outside the loaded window are only reachable through their nodes.
                val resolvedIds = resolved.values.map { it.id }
                selectedIdsFlow.update { it.removeAll(resolvedIds) }
            }
        }
    }

    /**
     * [TimelineSelection.isRangeSelected] evaluated against the source flows rather than the published
     * [uiState], which can trail them by a dispatch when the header is tapped right after a change.
     */
    private fun isRangeSelected(range: IntRange, layout: TimelineRevampUiState.Data): Boolean {
        val ids = selectedIdsFlow.value
        val pending = pendingSelectionFlow.value
        val loaded = loadedNodes.value
        return range.all { index ->
            val node = loaded[index]
            if (node == null) slotKeyOf(index, layout) in pending
            else node.isTakenDown || node.id in ids
        }
    }

    /**
     * Resolves the cells drag-selected while unloaded (see [setCellSelected]) as soon as the drag
     * ends, so they join the selection without waiting to scroll into the loading window. Spans only
     * the unresolved cells, so past selections don't stretch the fetch.
     */
    fun resolvePendingSelection() {
        val layout = uiState.value as? TimelineRevampUiState.Data ?: return
        val offsetByGroupId = offsetByGroupIdOf(layout.sections, layout.sectionStartOffsets)
        val unresolved = pendingSelectionFlow.value
            .filterValues { it == null }
            .keys
            .mapNotNull { slot -> offsetByGroupId[slot.groupId]?.plus(slot.localIndex) }
        if (unresolved.isEmpty()) return
        viewModelScope.launch {
            selectResolvedPending(loadMediaSlots(unresolved.min(), unresolved.max()))
        }
    }

    /**
     * Leaves selection mode. A range fetch still in flight finds no pending marks to resolve, so it
     * cannot revive the selection.
     */
    fun clearSelection() {
        pendingSelectionFlow.update { persistentMapOf() }
        selectedIdsFlow.update { persistentSetOf() }
    }

    private fun markPending(slots: List<SlotKey>) {
        if (slots.isEmpty()) return
        pendingSelectionFlow.update { pending ->
            pending.mutate { m -> slots.forEach { slot -> if (slot !in m) m[slot] = null } }
        }
    }

    /**
     * Drops the pending marks of [slots]; a mark already resolved to a node deselects that node too.
     */
    private fun unmarkPending(slots: List<SlotKey>) {
        if (slots.isEmpty()) return
        val previous = pendingSelectionFlow.getAndUpdate { pending ->
            pending.mutate { m -> slots.forEach(m::remove) }
        }
        val resolvedIds = slots.mapNotNull { previous[it] }
        if (resolvedIds.isNotEmpty()) selectedIdsFlow.update { it.removeAll(resolvedIds) }
    }

    /**
     * Records the node of each still-pending cell in [resolved] and selects it. Cells no longer
     * pending are skipped, so leaving selection mode mid-fetch doesn't revive the selection.
     */
    private fun selectResolvedPending(resolved: Map<SlotKey, PhotosNodeContentItemV2>) {
        if (resolved.isEmpty()) return
        val previous = pendingSelectionFlow.getAndUpdate { pending ->
            pending.mutate { m ->
                resolved.forEach { (slot, node) ->
                    if (m.isUnresolved(slot)) m[slot] = node.id
                }
            }
        }
        val newlySelected = resolved.mapNotNull { (slot, node) ->
            node.id.takeIf { previous.isUnresolved(slot) && !node.isTakenDown }
        }
        if (newlySelected.isNotEmpty()) selectedIdsFlow.update { it.addAll(newlySelected) }
    }

    private fun Map<SlotKey, Long?>.isUnresolved(slot: SlotKey): Boolean =
        slot in this && this[slot] == null

    /**
     * Hands pending cells whose node has reached the cache over to the id-based selection: the cell
     * is selected by id and its mark dropped, so it behaves like any loaded cell from then on.
     */
    private fun reconcilePendingSelection() {
        val pending = pendingSelectionFlow.value
        if (pending.isEmpty()) return
        val loadedSlots = mutableListOf<SlotKey>()
        val newlySelected = mutableListOf<Long>()
        for ((slot, resolvedId) in pending) {
            val node = mediaCache[slot] ?: continue
            loadedSlots += slot
            if (resolvedId == null && !node.isTakenDown) newlySelected += node.id
        }
        if (loadedSlots.isEmpty()) return
        pendingSelectionFlow.update { it.mutate { m -> loadedSlots.forEach(m::remove) } }
        if (newlySelected.isNotEmpty()) selectedIdsFlow.update { it.addAll(newlySelected) }
    }

    /**
     * Starts (once) the coroutine resolving the selected ids to [TypedNode]s for the selection-mode
     * actions. Re-runs on a selection or section change so a node deleted meanwhile drops out of the
     * selection; debounced so a drag sweep doesn't restart the lookup on every cell.
     */
    @OptIn(FlowPreview::class)
    private fun ensureSelectionResolverStarted() {
        if (!selectionResolverStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            combine(
                selectedIdsFlow,
                sectionsState.map { it.sectionsOrEmpty() }.distinctUntilChanged(),
            ) { ids, _ -> ids }
                .debounce(SELECTION_RESOLVE_DEBOUNCE_MS.milliseconds)
                .collectLatest(::resolveSelectedNodes)
        }
    }

    /**
     * Publishes the [TypedNode]s of [ids] and prunes the ids that no longer resolve. Only ids covered
     * by this lookup are pruned, never ones selected while it ran, and never on an empty result,
     * which a transient failure also produces.
     */
    private suspend fun resolveSelectedNodes(ids: Set<Long>) {
        if (ids.isEmpty()) {
            _selectedPhotosInTypedNodesFlow.update { emptyList() }
            return
        }
        val nodes = runCatching { getNodeListByIdsUseCase(ids.map { NodeId(it) }) }
            .onFailure {
                if (it is CancellationException) throw it
                Timber.e(it, "Failed to resolve selected nodes")
            }
            .getOrNull() ?: return
        _selectedPhotosInTypedNodesFlow.update { nodes }
        if (nodes.isEmpty()) return
        val resolvedIds = nodes.mapTo(HashSet()) { it.id.longValue }
        val missing = ids.filterNot { it in resolvedIds }
        if (missing.isNotEmpty()) selectedIdsFlow.update { it.removeAll(missing) }
    }

    /**
     * Updates the grid size (and tracks the selection). Only changes the grid column count — it does
     * not reload the timeline.
     */
    fun onGridSizeChange(size: TimelineGridSize) {
        gridSizeFlow.value = size
        persistGridSize(size)
        trackGridSizeSelection(size)
    }

    private fun persistGridSize(size: TimelineGridSize) {
        viewModelScope.launch {
            runCatching { setTimelineGridSizeUseCase(size.ordinal) }
                .onFailure { Timber.e(it, "Unable to persist timeline grid size") }
        }
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
     * Updates the timestamp column and direction to sort by. Changing it reloads the timeline through
     * [monitorTimelineSections], re-bucketing the sections and re-ordering the per-section pages.
     */
    fun onSortOptionsChange(value: TimelineRevampSortConfiguration) {
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
        val currentSort: TimelineRevampSortConfiguration,
    )

    private data class Selection(
        val ids: PersistentSet<Long>,
        val pending: PersistentMap<SlotKey, Long?>,
    )

    private data class PeriodCards(
        val yearCards: List<PhotosNodeListCard>,
        val monthCards: List<PhotosNodeListCard>,
        val isLoading: Boolean = false,
    ) {
        fun forPeriod(period: MediaTimePeriod): List<PhotosNodeListCard> = when (period) {
            MediaTimePeriod.Years -> yearCards
            MediaTimePeriod.Months -> monthCards
            else -> emptyList()
        }

        companion object {
            val Loading = PeriodCards(emptyList(), emptyList(), isLoading = true)
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
        private const val SELECTION_RESOLVE_DEBOUNCE_MS = 100L
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
