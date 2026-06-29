package mega.privacy.android.feature.photos.presentation.timeline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineMediaUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.MediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeCompactSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeDefaultSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeLargeSelectedEvent
import timber.log.Timber
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TimelineTabViewModel @Inject constructor(
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorTimelineMediaUseCase: MonitorTimelineMediaUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private var allMediaInTypedFileNodes: List<TypedFileNode> = emptyList()
    private var lastDisplayedPhotos: List<PhotosNodeContentItemV2> = emptyList()
    private val gridSizeFlow = MutableStateFlow(TimelineGridSize.Default)
    private val sortOptionsFlow = MutableStateFlow(TimelineTabSortOptions.Newest)

    /**
     * We need to hoist the state here in the ViewModel class because we want to preserve the
     * selection even after a navigation occurs.
     *
     * We don't need to put this in [TimelineTabUiState] because there is no need to rebuild the
     * uiState when this property changes.
     */
    internal var selectedTimePeriod by mutableStateOf(MediaTimePeriod.All)
    private val _selectedPhotosInTypedNodesFlow =
        MutableStateFlow<List<TypedNode>>(value = emptyList())
    internal val selectedPhotosInTypedNodesFlow = _selectedPhotosInTypedNodesFlow.asStateFlow()
    private val actionFlow = MutableStateFlow(TimelineTabActionUiState())
    internal val actionUiState: StateFlow<TimelineTabActionUiState> by lazy {
        actionFlow.asUiStateFlow(
            scope = viewModelScope,
            initialValue = TimelineTabActionUiState()
        )
    }

    private val selectedFilterFlow = MutableStateFlow<Map<String, String?>?>(null)
    internal val filterUiState: StateFlow<TimelineFilterUiState> by lazy {
        combine(
            flow { emit(getTimelineFilterPreferencesUseCase()) }
                .catch { Timber.e(it) },
            selectedFilterFlow,
        ) { preferenceMap, newFilter ->
            timelineFilterUiStateMapper(
                preferenceMap = newFilter ?: preferenceMap,
                shouldApplyFilterFromPreference = newFilter != null
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = TimelineFilterUiState()
        )
    }

    internal val uiState: StateFlow<TimelineTabUiState> by lazy {
        flow {
            // When revamp flag is on, we prevent monitorPhotos() because this is a heavy operation
            val isRevampEnabled = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.TimelineRevamp)
            }.getOrDefault(false)
            if (!isRevampEnabled) {
                emitAll(monitorPhotos().distinctUntilChanged())
            }
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = TimelineTabUiState()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorPhotos() =
        combine(
            flow = photosFlow(),
            flow2 = gridSizeFlow,
            flow3 = monitorHiddenNodesEnabledUseCase().catch {
                Timber.e(it, "Unable to monitor hidden nodes enabled")
            },
            transform = ::Triple
        ).map { (mediaGroup, gridSize, isHiddenNodesEnabled) ->
            buildTimelineTabUiState(
                mediaGroup = mediaGroup,
                gridSize = gridSize,
                isHiddenNodesEnabled = isHiddenNodesEnabled
            )
        }

    private fun photosFlow() = combine(
        flow = monitorTimelineMediaUseCase(
            request = TimelinePhotosRequest(
                selectedFilterFlow = selectedFilterFlow
            )
        ).catch { Timber.e(it) },
        flow2 = sortOptionsFlow,
    ) { nodes, sortOptions ->
        val sortResult = monitorTimelineMediaUseCase.sortMedia(
            nodes = nodes,
            sortOrder = sortOptions.sortOrder
        )
        buildMediaGroup(nodes = sortResult)
    }

    private fun buildMediaGroup(nodes: List<TypedFileNode>): MediaGroup {
        if (nodes.isEmpty()) {
            return MediaGroup(
                sortedPhotos = emptyList(),
                photosInDay = emptyList(),
                photosInMonth = emptyList(),
                photosInYear = emptyList()
            )
        }

        // Generate a map of days (12 March, 25 May, etc), months, and years to its total media.
        val dayMap = LinkedHashMap<Long, Pair<TypedFileNode, Int>>()
        val monthCountMap = HashMap<YearMonth, Int>()
        val yearCountMap = HashMap<Int, Int>()
        for (node in nodes) {
            val key = TimelineDateCache.epochDay(node.modificationTime)
            val zdt = TimelineDateCache.get(node.modificationTime)
            val month = YearMonth.from(zdt)
            val year = zdt.year
            val current = dayMap[key]
            if (current == null) {
                dayMap[key] = node to 1
            } else {
                dayMap[key] = current.first to (current.second + 1)
            }
            monthCountMap[month] = (monthCountMap[month] ?: 0) + (dayMap[key]?.second ?: 0)
            yearCountMap[year] = (yearCountMap[year] ?: 0) + (dayMap[key]?.second ?: 0)
        }

        val nowYear = Year.of(
            Instant.ofEpochMilli(getDeviceCurrentTimeUseCase())
                .atZone(java.time.ZoneId.systemDefault())
                .year
        )
        val dayList = mutableListOf<PhotosNodeListCard>()
        val monthList = mutableListOf<PhotosNodeListCard>()
        val yearList = mutableListOf<PhotosNodeListCard>()

        val seenMonths = HashSet<YearMonth>()
        val seenYears = HashSet<Int>()

        for ((node, count) in dayMap.values) {
            val zdt = TimelineDateCache.get(node.modificationTime)
            val year = zdt.year
            val month = YearMonth.from(zdt)
            val key = node.id.longValue
            if (seenYears.add(year)) {
                yearList.add(
                    PhotosNodeListCard(
                        period = PhotosNodeListCardPeriod.Year,
                        key = key,
                        id = node.id.longValue,
                        day = zdt.dayOfMonth,
                        month = zdt.monthValue,
                        year = zdt.year,
                        formattedDate = TimelineFormatters.year.format(zdt),
                        thumbnailFilePath = node.thumbnailPath,
                        previewFilePath = node.previewPath,
                        extension = node.type.extension,
                        isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
                        count = yearCountMap[year] ?: count
                    )
                )
            }

            if (seenMonths.add(month)) {
                val date = if (year == nowYear.value) {
                    TimelineFormatters.month.format(zdt)
                } else {
                    TimelineFormatters.monthYear.format(zdt)
                }
                monthList.add(
                    PhotosNodeListCard(
                        period = PhotosNodeListCardPeriod.Month,
                        key = key,
                        id = node.id.longValue,
                        day = zdt.dayOfMonth,
                        month = zdt.monthValue,
                        year = zdt.year,
                        formattedDate = date,
                        thumbnailFilePath = node.thumbnailPath,
                        previewFilePath = node.previewPath,
                        extension = node.type.extension,
                        isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
                        count = monthCountMap[month] ?: count
                    )
                )
            }

            val date = if (year == nowYear.value) {
                TimelineFormatters.dayMonth.format(zdt)
            } else {
                TimelineFormatters.dayMonthYear.format(zdt)
            }
            dayList.add(
                PhotosNodeListCard(
                    period = PhotosNodeListCardPeriod.Day,
                    key = key,
                    id = node.id.longValue,
                    day = zdt.dayOfMonth,
                    month = zdt.monthValue,
                    year = zdt.year,
                    formattedDate = date,
                    thumbnailFilePath = node.thumbnailPath,
                    previewFilePath = node.previewPath,
                    extension = node.type.extension,
                    isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
                    count = count
                )
            )
        }

        return MediaGroup(
            sortedPhotos = nodes,
            photosInDay = dayList,
            photosInMonth = monthList,
            photosInYear = yearList
        )
    }

    private fun buildTimelineTabUiState(
        mediaGroup: MediaGroup,
        gridSize: TimelineGridSize,
        isHiddenNodesEnabled: Boolean,
    ): TimelineTabUiState {
        allMediaInTypedFileNodes = mediaGroup.sortedPhotos
        val newDisplayedPhotos = buildDisplayedPhotos(mediaGroup, gridSize)
        val displayedPhotos = if (newDisplayedPhotos == lastDisplayedPhotos) {
            lastDisplayedPhotos
        } else {
            lastDisplayedPhotos = newDisplayedPhotos
            newDisplayedPhotos
        }
        actionFlow.update { it.copy(isReady = true) }
        return TimelineTabUiState(
            isLoading = false,
            displayedPhotos = displayedPhotos,
            daysCardPhotos = mediaGroup.photosInDay,
            monthsCardPhotos = mediaGroup.photosInMonth,
            yearsCardPhotos = mediaGroup.photosInYear,
            gridSize = gridSize,
            currentSort = sortOptionsFlow.value,
            isHiddenNodesEnabled = isHiddenNodesEnabled
        )
    }

    private fun buildDisplayedPhotos(
        mediaGroup: MediaGroup,
        gridSize: TimelineGridSize,
    ): List<PhotosNodeContentItemV2> = buildList {
        mediaGroup.sortedPhotos.forEachIndexed { index, node ->
            val currentTime = TimelineDateCache.get(node.modificationTime)
            val shouldShowDate = index == 0 || needsDateSeparator(
                currentTime = currentTime,
                previousTime = TimelineDateCache.get(
                    mediaGroup.sortedPhotos[index - 1].modificationTime
                ),
                gridSize = gridSize
            )
            val mediaType =
                if (node.type is ImageFileTypeInfo) MediaType.Image else MediaType.Video
            val isSensitive = node.isMarkedSensitive || node.isSensitiveInherited
            val duration = if (node.type is VideoFileTypeInfo) {
                durationInSecondsTextMapper(duration = (node.type as VideoFileTypeInfo).duration)
            } else {
                ""
            }

            if (shouldShowDate) {
                add(
                    PhotosNodeContentItemV2(
                        key = -node.id.longValue,
                        contentType = PhotosNodeContentType.Header,
                        id = node.id.longValue,
                        mediaType = mediaType,
                        day = currentTime.dayOfMonth,
                        month = currentTime.monthValue,
                        year = currentTime.year,
                        fullModificationTime = node.modificationTime,
                        thumbnailFilePath = node.thumbnailPath,
                        previewFilePath = node.previewPath,
                        extension = node.type.extension,
                        isFavourite = node.isFavourite,
                        isSensitive = isSensitive,
                        duration = duration
                    )
                )
            }

            add(
                PhotosNodeContentItemV2(
                    key = node.id.longValue,
                    contentType = PhotosNodeContentType.PhotoNode,
                    id = node.id.longValue,
                    mediaType = mediaType,
                    day = currentTime.dayOfMonth,
                    month = currentTime.monthValue,
                    year = currentTime.year,
                    fullModificationTime = node.modificationTime,
                    thumbnailFilePath = node.thumbnailPath,
                    previewFilePath = node.previewPath,
                    extension = node.type.extension,
                    isFavourite = node.isFavourite,
                    isSensitive = isSensitive,
                    duration = duration
                )
            )
        }
    }

    private fun needsDateSeparator(
        currentTime: ZonedDateTime,
        previousTime: ZonedDateTime,
        gridSize: TimelineGridSize,
    ): Boolean = if (gridSize == TimelineGridSize.Large) {
        currentTime != previousTime
    } else {
        currentTime.month != previousTime.month
    }

    internal fun onSortOptionsChange(value: TimelineTabSortOptions) {
        sortOptionsFlow.value = value
    }

    internal fun onGridSizeChange(
        size: TimelineGridSize,
        isEnableCameraUploadPageShowing: Boolean,
        mediaSource: FilterMediaSource,
    ) {
        gridSizeFlow.update { size }
        trackGridSizeSelection(size)
        updateSortActionEnablement(
            isEnableCameraUploadPageShowing = isEnableCameraUploadPageShowing,
            mediaSource = mediaSource
        )
    }

    /** Track analytics for grid size selection */
    private fun trackGridSizeSelection(gridSize: TimelineGridSize) {
        when (gridSize) {
            TimelineGridSize.Compact -> {
                Analytics.tracker.trackEvent(MediaScreenGridSizeCompactSelectedEvent)
            }

            TimelineGridSize.Default -> {
                Analytics.tracker.trackEvent(MediaScreenGridSizeDefaultSelectedEvent)
            }

            TimelineGridSize.Large -> {
                Analytics.tracker.trackEvent(MediaScreenGridSizeLargeSelectedEvent)
            }
        }
    }

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
                mediaSource = mediaSource
            )
        }
    }

    internal fun updateSortActionEnablement(
        isEnableCameraUploadPageShowing: Boolean,
        mediaSource: FilterMediaSource,
    ) {
        if (uiState.value.displayedPhotos.isEmpty() || (isEnableCameraUploadPageShowing && mediaSource != FilterMediaSource.CloudDrive)) {
            disableSortToolbarMenuAction()
        } else {
            enableSortToolbarMenuAction()
        }
    }

    private fun disableSortToolbarMenuAction() {
        actionFlow.update {
            it.copy(normalModeItem = it.normalModeItem.copy(enableSort = false))
        }
    }

    private fun enableSortToolbarMenuAction() {
        actionFlow.update {
            it.copy(normalModeItem = it.normalModeItem.copy(enableSort = true))
        }
    }

    internal fun onFilterChange(request: TimelineFilterRequest) {
        viewModelScope.launch {
            runCatching {
                val newPreferences = mapOf(
                    TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to request.isRemembered.toString(),
                    TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to request.mediaType.toMediaTypeValue(),
                    TimelinePreferencesJSON.JSON_KEY_LOCATION.value to request.mediaSource.toLocationValue(),
                )
                setTimelineFilterPreferencesUseCase(newPreferences)
                newPreferences
            }.onSuccess { newPreferences ->
                selectedFilterFlow.update { newPreferences }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    internal fun retrieveTypedNodeFromSelection(selectedIds: Set<Long>): List<TypedNode> {
        val nodes = allMediaInTypedFileNodes.filter { it.id.longValue in selectedIds }
        _selectedPhotosInTypedNodesFlow.update { nodes }
        return nodes
    }

    internal fun onMediaTimePeriodSelected(value: MediaTimePeriod) {
        selectedTimePeriod = value
    }

    private data class MediaGroup(
        val sortedPhotos: List<TypedFileNode>,
        val photosInDay: List<PhotosNodeListCard>,
        val photosInMonth: List<PhotosNodeListCard>,
        val photosInYear: List<PhotosNodeListCard>,
    )
}
