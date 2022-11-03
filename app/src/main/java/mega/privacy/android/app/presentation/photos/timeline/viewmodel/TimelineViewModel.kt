package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.EnablePhotosCameraUpload
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetTimelinePhotos
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import nz.mega.sdk.MegaNode
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import java.time.LocalDateTime
import java.util.Collections
import javax.inject.Inject

/**
 * View Model for Timeline
 *
 * @property isCameraSyncPreferenceEnabled
 * @property getTimelinePhotos
 * @property getCameraUploadPhotos
 * @property getCloudDrivePhotos
 * @property setInitialCUPreferences
 * @property enablePhotosCameraUpload
 * @property getNodeListByIds
 * @property jobUtilWrapper
 * @property ioDispatcher
 * @property mainDispatcher
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    val isCameraSyncPreferenceEnabled: IsCameraSyncPreferenceEnabled,
    private val getTimelinePhotos: GetTimelinePhotos,
    val getCameraUploadPhotos: FilterCameraUploadPhotos,
    val getCloudDrivePhotos: FilterCloudDrivePhotos,
    val setInitialCUPreferences: SetInitialCUPreferences,
    val enablePhotosCameraUpload: EnablePhotosCameraUpload,
    val getNodeListByIds: GetNodeListByIds,
    private val jobUtilWrapper: JobUtilWrapper,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    internal val _state = MutableStateFlow(TimelineViewState(loadPhotosDone = false))
    val state = _state.asStateFlow()

    internal val selectedPhotosIds = mutableSetOf<Long>()

    private var job: Job? = null

    init {
        job = viewModelScope.launch {
            getTimelinePhotos()
                .catch { throwable ->
                    Timber.e(throwable)
                }.collectLatest { photos ->
                    Timber.v("TimelineViewModel photos flow=>" + photos.size)
                    val showingPhotos = filterMedias(photos)
                    handleAndUpdatePhotosUIState(
                        sourcePhotos = photos,
                        showingPhotos = showingPhotos
                    )
                }
        }
    }

    internal suspend fun handleAndUpdatePhotosUIState(
        sourcePhotos: List<Photo>,
        showingPhotos: List<Photo>,
    ) {
        sortPhotos(showingPhotos)
        val photosListItems = handleAllPhotoItems(showingPhotos)
        val dayPhotos = groupPhotosByDay(showingPhotos, _state.value.currentSort)
        val yearCardList = createYearsCardList(dayPhotos)
        val monthCardList = createMonthsCardList(dayPhotos)
        val dayCardList = createDaysCardList(dayPhotos)

        _state.update {
            it.copy(
                photos = sourcePhotos,
                photosListItems = photosListItems,
                loadPhotosDone = true,
                currentShowingPhotos = showingPhotos,
                yearsCardPhotos = yearCardList,
                monthsCardPhotos = monthCardList,
                daysCardPhotos = dayCardList,
            )
        }
        handleEnableZoomAndSortOptions()
    }

    private fun handleAllPhotoItems(showingPhotos: List<Photo>): List<PhotoListItem> {
        val currentZoomLevel = _state.value.currentZoomLevel
        val photoListItem = mutableListOf<PhotoListItem>()
        showingPhotos.forEachWithIndex { index, photo ->
            val shouldShowDate = if (index == 0)
                true
            else
                needsDateSeparator(
                    current = photo,
                    previous = showingPhotos[index - 1],
                    currentZoomLevel = currentZoomLevel
                )
            if (shouldShowDate) {
                photoListItem.add(PhotoListItem.Separator(photo.modificationTime))
            }
            photoListItem.add(
                PhotoListItem.PhotoGridItem(
                    photo = photo,
                    isSelected = selectedPhotosIds.contains(photo.id),
                )
            )
        }
        return photoListItem
    }

    internal fun setSelectedPhotos(items: List<PhotoListItem>): List<PhotoListItem> = items.map {
        if (it is PhotoListItem.PhotoGridItem) {
            it.copy(isSelected = it.photo.id in selectedPhotosIds)
        } else it
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        currentZoomLevel: ZoomLevel,
    ): Boolean {
        val currentDate = current.modificationTime.toLocalDate()
        val previousDate = previous.modificationTime.toLocalDate()
        return if (currentZoomLevel == ZoomLevel.Grid_1) {
            currentDate != previousDate
        } else {
            currentDate.month != previousDate.month
        }
    }

    private fun sortPhotos(photos: List<Photo>) {
        if (_state.value.currentSort == Sort.NEWEST) {
            Collections.sort(
                photos,
                Comparator.comparing<Photo?, LocalDateTime?> { photo -> photo.modificationTime }
                    .reversed()
            )
        } else {
            Collections.sort(
                photos,
                Comparator.comparing { photo -> photo.modificationTime }
            )
        }
    }

    fun sortByOrder() {
        viewModelScope.launch {
            handleAndUpdatePhotosUIState(_state.value.photos,
                _state.value.currentShowingPhotos)
        }
    }

    fun enableCU(context: Context) {
        _state.update {
            it.copy(
                enableCameraUploadButtonShowing = false,
                enableCameraUploadPageShowing = false,
            )
        }
        handleEnableZoomAndSortOptions()

        viewModelScope.launch(ioDispatcher) {
            val localFile = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            )
            enablePhotosCameraUpload(
                path = localFile?.absolutePath,
                syncVideo = _state.value.cuUploadsVideos,
                enableCellularSync = _state.value.cuUseCellularConnection,
                videoQuality = VideoQuality.ORIGINAL.value,
                conversionChargingOnSize = SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE
            )
            Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
            jobUtilWrapper.fireCameraUploadJob(context, false)
        }
    }

    fun getSelectedIds(): List<Long> =
        selectedPhotosIds.toList()

    suspend fun getSelectedNodes(): List<MegaNode> =
        getNodeListByIds(selectedPhotosIds.toList())


    fun setInitialPreferences() {
        viewModelScope.launch(ioDispatcher) {
            setInitialCUPreferences()
        }
    }

    fun isInAllView(): Boolean = _state.value.selectedTimeBarTab == TimeBarTab.All

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }

    internal fun resetCUButtonAndProgress() {
        viewModelScope.launch(ioDispatcher) {
            if (isCameraSyncPreferenceEnabled()) {
                _state.update {
                    it.copy(
                        enableCameraUploadButtonShowing = false,
                        enableCameraUploadPageShowing = false,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        enableCameraUploadButtonShowing = true,
                        progressBarShowing = false
                    )
                }
            }
        }
    }

    fun onLongPress(photo: Photo) {
        togglePhotoSelection(photo.id)
        _state.update {
            it.copy(
                photosListItems = setSelectedPhotos(it.photosListItems),
                selectedPhotoCount = selectedPhotosIds.size,
            )
        }
    }

    private fun togglePhotoSelection(id: Long) {
        if (id in selectedPhotosIds) {
            selectedPhotosIds.remove(id)
        } else {
            selectedPhotosIds.add(id)
        }
    }

    fun onClick(photo: Photo) {
        if (selectedPhotosIds.size == 0) {
            _state.update {
                it.copy(selectedPhoto = photo)
            }
        } else {
            onLongPress(photo)
        }
    }

    fun onNavigateToSelectedPhoto() {
        _state.update {
            it.copy(selectedPhoto = null)
        }
    }

    fun onCardClick(dateCard: DateCard) {
        when (dateCard) {
            is DateCard.YearsCard -> {
                val monthsCardList = _state.value.monthsCardPhotos
                val photo = monthsCardList.find {
                    it.photo.modificationTime == dateCard.photo.modificationTime
                }
                updateSelectedTimeBarState(TimeBarTab.Months, monthsCardList.indexOf(photo))
            }
            is DateCard.MonthsCard -> {
                val daysCardList = _state.value.daysCardPhotos
                val photo = daysCardList.find {
                    it.photo.modificationTime == dateCard.photo.modificationTime
                }
                updateSelectedTimeBarState(TimeBarTab.Days, daysCardList.indexOf(photo))
            }
            is DateCard.DaysCard -> {
                val photosList = _state.value.photosListItems
                val photo = photosList.find {
                    it.key == dateCard.photo.id.toString()
                }
                updateSelectedTimeBarState(
                    TimeBarTab.All,
                    photosList.indexOf(photo)
                )
            }
        }
    }

    fun onTimeBarTabSelected(tab: TimeBarTab) {
        when (tab) {
            TimeBarTab.Years -> {
                updateSelectedTimeBarState(TimeBarTab.Years)
            }
            TimeBarTab.Months -> {
                updateSelectedTimeBarState(TimeBarTab.Months)
            }
            TimeBarTab.Days -> {
                updateSelectedTimeBarState(TimeBarTab.Days)
            }
            TimeBarTab.All -> {
                updateSelectedTimeBarState(TimeBarTab.All)
            }
        }
    }

    fun updateSelectedTimeBarState(
        selectedTimeBarTab: TimeBarTab,
        startIndex: Int = 0,
        startOffset: Int = 0,
    ) {
        _state.update {
            it.copy(
                selectedTimeBarTab = selectedTimeBarTab,
                scrollStartIndex = startIndex,
                scrollStartOffset = startOffset
            )
        }
    }
}

