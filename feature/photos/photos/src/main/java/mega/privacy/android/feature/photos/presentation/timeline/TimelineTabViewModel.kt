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
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.ZoomLevel
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
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
) : ViewModel() {

    private val zoomLevelFlow = MutableStateFlow(ZoomLevel.Grid_3)
    private val sortOrderFlow = MutableStateFlow(SortOrder.ORDER_MODIFICATION_DESC)
    private val selectedPhotoIdsFlow = MutableStateFlow<List<Long>>(emptyList())

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
                    isPaginationEnabled = isPaginationEnabled
                )
            }
            .flatMapLatest(::monitorPhotos)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorPhotos(request: TimelinePhotosRequest) = combine(
        flow = monitorTimelinePhotosUseCase(request = request),
        flow2 = sortOrderFlow
    ) { photosResult, sortOrder ->
        val sortResult = monitorTimelinePhotosUseCase.sortPhotos(
            isPaginationEnabled = request.isPaginationEnabled,
            photos = photosResult.nonSensitivePhotos,
            sortOrder = sortOrder
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
        flow = zoomLevelFlow,
        flow2 = selectedPhotoIdsFlow,
        transform = ::Pair
    ).map { (zoomLevel, selectedPhotoIds) ->
        buildTimelineTabUiState(
            allPhotos = allPhotos,
            sortResult = sortResult,
            zoomLevel = zoomLevel,
            selectedPhotoIds = selectedPhotoIds,
            isPaginationEnabled = isPaginationEnabled
        )
    }

    private fun buildTimelineTabUiState(
        allPhotos: List<PhotoResult>,
        sortResult: TimelineSortedPhotosResult,
        zoomLevel: ZoomLevel,
        selectedPhotoIds: List<Long>,
        isPaginationEnabled: Boolean,
    ): TimelineTabUiState {
        val displayedPhotos = buildList {
            sortResult.sortedPhotos.forEachIndexed { index, photoResult ->
                val shouldShowDate = index == 0 || needsDateSeparator(
                    current = photoResult.photo,
                    previous = sortResult.sortedPhotos[index - 1].photo,
                    zoomLevel = zoomLevel
                )
                if (shouldShowDate) {
                    add(PhotosNodeContentType.DateItem(photoResult.photo.modificationTime))
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
            zoomLevel = zoomLevel,
            selectedPhotoCount = selectedPhotoIds.size,
            currentSort = sortOrderFlow.value,
            isPaginationEnabled = isPaginationEnabled
        )
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        zoomLevel: ZoomLevel,
    ): Boolean {
        val currentDate = current.modificationTime.toLocalDate()
        val previousDate = previous.modificationTime.toLocalDate()
        return if (zoomLevel == ZoomLevel.Grid_1) {
            currentDate != previousDate
        } else {
            currentDate.month != previousDate.month
        }
    }

    internal fun onSortOrderChange(value: SortOrder) {
        sortOrderFlow.value = value
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
}
