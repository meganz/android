package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampViewModel.Companion.DEFAULT_FILTER
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampViewModel.Companion.MAX_CACHED_ITEMS
import mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper.MediaTimelineNodeUiItemMapper
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
internal class TimelineRevampViewModel @Inject constructor(
    private val getMediaTimelineSectionsUseCase: GetMediaTimelineSectionsUseCase,
    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase,
    private val mediaTimelineNodeUiItemMapper: MediaTimelineNodeUiItemMapper,
) : ViewModel() {

    private val sections = MutableStateFlow<List<MediaTimelineSection>>(emptyList())
    private val loadedNodes = MutableStateFlow<Map<Int, PhotosNodeContentItemV2>>(emptyMap())
    private val targetRange = MutableStateFlow(IntRange.EMPTY)
    private val mediaLoaderStarted = AtomicBoolean(false)

    /**
     * The filter that scopes both the timeline sections and the per-section node pages. Seeded with
     * [DEFAULT_FILTER]; the user-controllable options (sort, media type, location, sensitivity,
     * granularity) update this in later steps and the sections re-load reactively.
     */
    private val currentFilter = MutableStateFlow(DEFAULT_FILTER)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimelineRevampUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorTimelineSections(),
            loadedNodes.asStateFlow(),
        ) { sectionItems, loaded ->
            if (sectionItems.isEmpty()) {
                TimelineRevampUiState.Empty
            } else {
                TimelineRevampUiState.Data(
                    sections = sectionItems,
                    sectionStartOffsets = sectionStartOffsetsOf(sectionItems),
                    loadedNodes = loaded,
                )
            }
        }
            .catch { e -> Timber.e(e, "Failed to load media timeline sections") }
            .asUiStateFlow(
                viewModelScope,
                TimelineRevampUiState.Loading,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorTimelineSections(): Flow<List<MediaTimelineSection>> =
        currentFilter
            .mapLatest { getMediaTimelineSectionsUseCase(it) }
            .onEach { newSections ->
                sections.update { newSections }
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
                order = SortOrder.ORDER_MODIFICATION_DESC,
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
