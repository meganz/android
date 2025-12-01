package mega.privacy.android.feature.photos.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimelineTabViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorTimelinePhotosUseCase: MonitorTimelinePhotosUseCase,
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val photosNodeListCardMapper: PhotosNodeListCardMapper,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
) : ViewModel() {

    private val gridSizeFlow = MutableStateFlow(TimelineGridSize.Default)
    private val sortOptionsFlow = MutableStateFlow(TimelineTabSortOptions.Newest)
    private val selectedPhotoIdsFlow = MutableStateFlow<List<Long>>(emptyList())

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
            ::Pair
        ).map { (preferenceMap, newFilter) ->
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
        timelineTabUiState().asUiStateFlow(
            scope = viewModelScope,
            initialValue = TimelineTabUiState()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun timelineTabUiState() =
        flow { emit(isPaginationEnabled()) }
            .map { isPaginationEnabled ->
                TimelinePhotosRequest(
                    isPaginationEnabled = isPaginationEnabled,
                    selectedFilterFlow = selectedFilterFlow
                )
            }
            .flatMapLatest(::monitorPhotos)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorPhotos(request: TimelinePhotosRequest) = combine(
        flow = monitorTimelinePhotosUseCase(request = request),
        flow2 = sortOptionsFlow
    ) { photosResult, sortOptions ->
        val sortResult = monitorTimelinePhotosUseCase.sortPhotos(
            isPaginationEnabled = request.isPaginationEnabled,
            photos = photosResult.nonSensitivePhotos,
            sortOrder = sortOptions.sortOrder
        )
        Pair(photosResult.allPhotos, sortResult)
    }.flatMapLatest { (allPhotos, sortResult) ->
        buildUiState(
            isPaginationEnabled = request.isPaginationEnabled,
            allPhotos = allPhotos,
            sortResult = sortResult
        )
    }

    private fun buildUiState(
        isPaginationEnabled: Boolean,
        allPhotos: List<PhotoResult>,
        sortResult: TimelineSortedPhotosResult,
    ) = combine(
        flow = gridSizeFlow,
        flow2 = selectedPhotoIdsFlow,
        transform = ::Pair
    ).map { (gridSize, selectedPhotoIds) ->
        buildTimelineTabUiState(
            allPhotos = allPhotos,
            sortResult = sortResult,
            gridSize = gridSize,
            selectedPhotoIds = selectedPhotoIds,
            isPaginationEnabled = isPaginationEnabled
        )
    }

    private fun buildTimelineTabUiState(
        allPhotos: List<PhotoResult>,
        sortResult: TimelineSortedPhotosResult,
        gridSize: TimelineGridSize,
        selectedPhotoIds: List<Long>,
        isPaginationEnabled: Boolean,
    ): TimelineTabUiState {
        val displayedPhotos = buildList {
            sortResult.sortedPhotos.forEachIndexed { index, photoResult ->
                val shouldShowDate = index == 0 || needsDateSeparator(
                    current = photoResult.photo,
                    previous = sortResult.sortedPhotos[index - 1].photo,
                    gridSize = gridSize
                )
                if (shouldShowDate) {
                    add(
                        PhotosNodeContentType.HeaderItem(
                            time = photoResult.photo.modificationTime,
                            shouldShowGridSizeSettings = index == 0
                        )
                    )
                }
                add(
                    PhotosNodeContentType.PhotoNodeItem(
                        node = PhotoNodeUiState(
                            photo = photoUiStateMapper(photo = photoResult.photo),
                            isSensitive = photoResult.isMarkedSensitive,
                            isSelected = selectedPhotoIds.contains(photoResult.photo.id),
                            defaultIcon = fileTypeIconMapper(fileExtension = photoResult.photo.fileTypeInfo.extension)
                        )
                    )
                )
            }
        }.toImmutableList()

        return TimelineTabUiState(
            isLoading = false,
            allPhotos = allPhotos
                .map { photoUiStateMapper(photo = it.photo) }
                .toImmutableList(),
            displayedPhotos = displayedPhotos,
            daysCardPhotos = photosNodeListCardMapper(photosDateResults = sortResult.photosInDay),
            monthsCardPhotos = photosNodeListCardMapper(photosDateResults = sortResult.photosInMonth),
            yearsCardPhotos = photosNodeListCardMapper(photosDateResults = sortResult.photosInYear),
            gridSize = gridSize,
            selectedPhotoCount = selectedPhotoIds.size,
            currentSort = sortOptionsFlow.value,
            isPaginationEnabled = isPaginationEnabled
        )
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        gridSize: TimelineGridSize,
    ): Boolean {
        val currentDate = current.modificationTime.toLocalDate()
        val previousDate = previous.modificationTime.toLocalDate()
        return if (gridSize == TimelineGridSize.Large) {
            currentDate != previousDate
        } else {
            currentDate.month != previousDate.month
        }
    }

    internal fun onSortOptionsChange(value: TimelineTabSortOptions) {
        sortOptionsFlow.value = value
    }

    internal fun loadNextPage() {
        if (!uiState.value.isPaginationEnabled) return

        viewModelScope.launch {
            runCatching {
                monitorTimelinePhotosUseCase.loadNextPage()
            }.onFailure {
                Timber.e(it, "Error loading next page of photos")
            }.onSuccess {
                Timber.d("Next page of photos loaded successfully")
            }
        }
    }

    private suspend fun isPaginationEnabled(): Boolean = runCatching {
        getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
    }.getOrElse { false }

    internal fun onGridSizeChange(
        size: TimelineGridSize,
        isEnableCameraUploadPageShowing: Boolean,
        mediaSource: FilterMediaSource,
    ) {
        gridSizeFlow.update { size }
        updateSortActionEnablement(
            isEnableCameraUploadPageShowing = isEnableCameraUploadPageShowing,
            mediaSource = mediaSource
        )
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
        actionFlow.update { it.copy(enableSort = false) }
    }

    private fun enableSortToolbarMenuAction() {
        actionFlow.update { it.copy(enableSort = true) }
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
}
