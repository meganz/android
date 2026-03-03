package mega.privacy.android.feature.photos.presentation.timeline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotoToTypedNodeMapper
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeCompactSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeDefaultSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeLargeSelectedEvent
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimelineTabViewModel @Inject constructor(
    private val monitorTimelinePhotosUseCase: MonitorTimelinePhotosUseCase,
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val photosNodeListCardMapper: PhotosNodeListCardMapper,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val photoToTypedNodeMapper: PhotoToTypedNodeMapper,
) : ViewModel() {

    private var isHiddenNodesEnabled: Boolean = false
    private val gridSizeFlow = MutableStateFlow(TimelineGridSize.Default)
    private val sortOptionsFlow = MutableStateFlow(TimelineTabSortOptions.Newest)

    /**
     * We need to hoist the state here in the ViewModel class because we want to preserve the
     * selection even after a navigation occurs.
     *
     * We don't need to put this in [TimelineTabUiState] because there is no need to rebuild the
     * uiState when this property changes.
     */
    internal var selectedTimePeriod by mutableStateOf(PhotoModificationTimePeriod.All)
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
            flow { emit(getTimelineFilterPreferencesUseCase()) },
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
        combine(
            monitorPhotos(),
            monitorHiddenNodesEnabledUseCase().catch {
                Timber.e(it, "Unable to monitor hidden nodes enabled")
            }
        ) { timelineTabUiState, isEnabled ->
            isHiddenNodesEnabled = isEnabled
            timelineTabUiState
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
            transform = ::Pair
        ).map { (photos, gridSize) ->
            buildTimelineTabUiState(
                allPhotos = photos.first,
                sortResult = photos.second,
                gridSize = gridSize,
            )
        }

    private fun photosFlow() = combine(
        flow = monitorTimelinePhotosUseCase(
            request = TimelinePhotosRequest(
                selectedFilterFlow = selectedFilterFlow
            )
        ),
        flow2 = sortOptionsFlow,
    ) { photosResult, sortOptions ->
        val sortResult = monitorTimelinePhotosUseCase.sortPhotos(
            photos = photosResult.nonSensitivePhotos,
            sortOrder = sortOptions.sortOrder
        )
        Pair(photosResult.allPhotos, sortResult)
    }

    private suspend fun buildTimelineTabUiState(
        allPhotos: List<PhotoResult>,
        sortResult: TimelineSortedPhotosResult,
        gridSize: TimelineGridSize,
    ): TimelineTabUiState = coroutineScope {
        val displayedPhotos = async {
            buildList {
                sortResult.sortedPhotos.forEachIndexed { index, photoResult ->
                    val shouldShowDate = index == 0 || needsDateSeparator(
                        current = photoResult.photo,
                        previous = sortResult.sortedPhotos[index - 1].photo,
                        gridSize = gridSize
                    )
                    if (shouldShowDate) {
                        add(
                            PhotosNodeContentItem.HeaderItem(
                                time = photoResult.photo.modificationTime
                            )
                        )
                    }
                    add(
                        PhotosNodeContentItem.PhotoNodeItem(
                            node = PhotoNodeUiState(
                                photo = photoUiStateMapper(photoResult.photo),
                                isSensitive = photoResult.isMarkedSensitive,
                                defaultIcon = fileTypeIconMapper(photoResult.photo.fileTypeInfo.extension)
                            )
                        )
                    )
                }
            }
        }
        val daysCardPhotos = async {
            photosNodeListCardMapper(photosDateResults = sortResult.photosInDay)
        }
        val monthsCardPhotos = async {
            photosNodeListCardMapper(photosDateResults = sortResult.photosInMonth)
        }
        val yearsCardPhotos = async {
            photosNodeListCardMapper(photosDateResults = sortResult.photosInYear)
        }
        TimelineTabUiState(
            isLoading = false,
            allPhotos = allPhotos,
            displayedPhotos = displayedPhotos.await(),
            daysCardPhotos = daysCardPhotos.await(),
            monthsCardPhotos = monthsCardPhotos.await(),
            yearsCardPhotos = yearsCardPhotos.await(),
            gridSize = gridSize,
            currentSort = sortOptionsFlow.value,
        )
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        gridSize: TimelineGridSize,
    ): Boolean = if (gridSize == TimelineGridSize.Large) {
        current.modificationTime != previous.modificationTime
    } else {
        current.modificationTime.month != previous.modificationTime.month
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
        val nodes = uiState.value.allPhotos
            .asSequence()
            .filter { it.photo.id in selectedIds }
            .map { photoToTypedNodeMapper(it.photo) }
            .toList()
        _selectedPhotosInTypedNodesFlow.update { nodes }
        return nodes
    }

    internal fun onPhotoTimePeriodSelected(value: PhotoModificationTimePeriod) {
        selectedTimePeriod = value
    }
}
