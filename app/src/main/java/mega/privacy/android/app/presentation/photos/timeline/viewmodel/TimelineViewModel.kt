package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.mapper.TimelinePreferencesMapper
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.LocationPreference
import mega.privacy.android.app.presentation.photos.model.MediaTypePreference
import mega.privacy.android.app.presentation.photos.model.RememberPreferences
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.TimelineFilterPreferences
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.MonitorCameraUploadProgress
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import nz.mega.sdk.MegaNode
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for Timeline
 *
 * @property IsCameraUploadsEnabledUseCase
 * @property getTimelinePhotosUseCase
 * @property getCameraUploadPhotos
 * @property getCloudDrivePhotos
 * @property setInitialCUPreferences
 * @property enableCameraUploadsInPhotosUseCase
 * @property getNodeListByIds
 * @property startCameraUploadUseCase
 * @property ioDispatcher
 * @property mainDispatcher
 * @property defaultDispatcher
 * @property checkEnableCameraUploadsStatus
 * @property stopCameraUploadAndHeartbeatUseCase
 * @param monitorCameraUploadProgress
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    val getCameraUploadPhotos: FilterCameraUploadPhotos,
    val getCloudDrivePhotos: FilterCloudDrivePhotos,
    val setInitialCUPreferences: SetInitialCUPreferences,
    private val enableCameraUploadsInPhotosUseCase: EnableCameraUploadsInPhotosUseCase,
    val getNodeListByIds: GetNodeListByIds,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher val mainDispatcher: CoroutineDispatcher,
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    private val checkEnableCameraUploadsStatus: CheckEnableCameraUploadsStatus,
    private val stopCameraUploadAndHeartbeatUseCase: StopCameraUploadAndHeartbeatUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelinePreferencesMapper: TimelinePreferencesMapper,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    monitorCameraUploadProgress: MonitorCameraUploadProgress,
) : ViewModel() {

    internal val _state = MutableStateFlow(TimelineViewState(loadPhotosDone = false))
    val state = _state.asStateFlow()

    internal val selectedPhotosIds = mutableSetOf<Long>()

    private var job: Job? = null

    init {
        job = viewModelScope.launch {
            getTimelinePhotosUseCase()
                .catch { throwable ->
                    Timber.e(throwable)
                }.collectLatest(::handlePhotos)
        }
        viewModelScope.launch {
            monitorCameraUploadProgress().collectLatest {
                updateCameraUploadProgressIfNeeded(progress = it.first, pending = it.second)
            }
        }
    }

    private suspend fun handlePhotos(photos: List<Photo>) {
        Timber.v("TimelineViewModel photos flow=>" + photos.size)
        if (getFeatureFlagValueUseCase(AppFeatures.RememberTimelinePreferences)) {
            handleTimelinePhotosUseCase()
        }
        val showingPhotos = withContext(defaultDispatcher) { filterMedias(photos) }
        handleAndUpdatePhotosUIState(
            sourcePhotos = photos,
            showingPhotos = showingPhotos
        )
    }

    private suspend fun handleTimelinePhotosUseCase() {
        runCatching {
            getTimelineFilterPreferencesUseCase()
        }.onSuccess { timelineFilterPreferences ->
            val latestPreferences = timelineFilterPreferences?.let {
                timelinePreferencesMapper(it)
            }
            latestPreferences?.let {
                val rememberPreferences: RememberPreferences =
                    it[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value] as RememberPreferences

                if (rememberPreferences.value) {
                    setInitialCUFilter(it)
                }
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Update the camera upload progress if needed
     *
     * The progress is set to the state only if no photos are currently selected or
     * the current view is not [TimeBarTab.All]
     *
     * @param progress value between 0 and 100
     * @param pending count of pending items to be uploaded
     */
    private fun updateCameraUploadProgressIfNeeded(progress: Int, pending: Int) {
        if (state.value.selectedPhotoCount > 0 || !isInAllView()) {
            setShowProgressBar(show = false)
        } else {
            // Check to avoid keeping setting same visibility
            updateProgress(
                pending = pending,
                showProgress = pending > 0,
                progress = progress.toFloat() / 100
            )

            Timber.d("CU Upload Progress: Pending: {$pending}, Progress: {$progress}")
        }
    }

    /**
     * Handle specific behavior when permissions are granted / denied
     */
    fun handlePermissionsResult() {
        if (hasMediaPermissionUseCase()) handleEnableCameraUploads()
        else setTriggerMediaPermissionsDeniedLogicState(shouldTrigger = true)
    }

    /**
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatus]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        runCatching { checkEnableCameraUploadsStatus() }.onSuccess { status ->
            when (status) {
                CAN_ENABLE_CAMERA_UPLOADS -> {
                    setTriggerCameraUploadsState(shouldTrigger = true)
                }

                SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                    setBusinessAccountPromptState(shouldShow = true)
                }

                SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT -> {
                    setBusinessAccountSuspendedPromptState(shouldShow = true)
                }
            }
        }.onFailure { Timber.w("Exception checking CU status: $it") }
    }

    /**
     * Sets the value of [TimelineViewState.shouldTriggerMediaPermissionsDeniedLogic]
     * @param shouldTrigger The new state value
     */
    fun setTriggerMediaPermissionsDeniedLogicState(shouldTrigger: Boolean) {
        _state.update { it.copy(shouldTriggerMediaPermissionsDeniedLogic = shouldTrigger) }
    }

    /**
     * Sets the value of [TimelineViewState.shouldTriggerCameraUploads]
     * @param shouldTrigger The new state value
     */
    fun setTriggerCameraUploadsState(shouldTrigger: Boolean) {
        _state.update { it.copy(shouldTriggerCameraUploads = shouldTrigger) }
    }

    /**
     * Sets the value of [TimelineViewState.shouldShowBusinessAccountPrompt]
     * @param shouldShow The new state value
     */
    fun setBusinessAccountPromptState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowBusinessAccountPrompt = shouldShow) }
    }

    /**
     * Sets the value of [TimelineViewState.shouldShowBusinessAccountSuspendedPrompt]
     * @param shouldShow The new state value
     */
    fun setBusinessAccountSuspendedPromptState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = shouldShow) }
    }

    internal fun handleAndUpdatePhotosUIState(
        sourcePhotos: List<Photo>,
        showingPhotos: List<Photo>,
    ) = viewModelScope.launch(defaultDispatcher) {
        val sortedPhotos = sortPhotos(showingPhotos)

        async {
            val items = handleAllPhotoItems(showingPhotos = sortedPhotos)
            _state.update {
                it.copy(photosListItems = items)
            }
        }

        val dayPhotos = groupPhotosByDay(sortedPhotos = sortedPhotos)

        async {
            val items = createYearsCardList(dayPhotos = dayPhotos)
            _state.update {
                it.copy(yearsCardPhotos = items)
            }
        }

        async {
            val items = createMonthsCardList(dayPhotos = dayPhotos)
            _state.update {
                it.copy(monthsCardPhotos = items)
            }
        }

        async {
            val items = createDaysCardList(dayPhotos = dayPhotos)
            _state.update {
                it.copy(daysCardPhotos = items)
            }
        }

        _state.update {
            it.copy(
                photos = sourcePhotos,
                loadPhotosDone = true,
                currentShowingPhotos = sortedPhotos,
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

    private fun sortPhotos(photos: List<Photo>): List<Photo> {
        return if (_state.value.currentSort == Sort.NEWEST) {
            photos.sortedByDescending { it.modificationTime }
        } else {
            photos.sortedBy { it.modificationTime }
        }
    }

    fun sortByOrder() {
        viewModelScope.launch {
            handleAndUpdatePhotosUIState(
                _state.value.photos,
                _state.value.currentShowingPhotos
            )
        }
    }

    fun enableCU() {
        _state.update {
            it.copy(
                enableCameraUploadButtonShowing = false,
                enableCameraUploadPageShowing = false,
            )
        }
        handleEnableZoomAndSortOptions()

        viewModelScope.launch(ioDispatcher) {
            enableCameraUploadsInPhotosUseCase(
                shouldSyncVideos = _state.value.cuUploadsVideos,
                shouldUseWiFiOnly = _state.value.cuUseCellularConnection.not(),
                videoCompressionSizeLimit = SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE,
                videoUploadQuality = VideoQuality.ORIGINAL,
            )
            Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
            startCameraUploadUseCase()
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
            if (isCameraUploadsEnabledUseCase()) {
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

    internal fun saveTimelineFilterPreferences(rememberPreferences: Boolean) =
        viewModelScope.launch {
            runCatching {
                setTimelineFilterPreferencesUseCase(
                    mapOf(
                        Pair(
                            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value,
                            rememberPreferences.toString()
                        ),
                        Pair(
                            TimelinePreferencesJSON.JSON_KEY_LOCATION.value,
                            timelinePreferencesMapper.mapLocationToString(_state.value.currentMediaSource)
                        ),
                        Pair(
                            TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value,
                            timelinePreferencesMapper.mapMediaTypeToString(_state.value.currentFilterMediaType)
                        ),
                    )
                )
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    private fun setInitialCUFilter(preferences: Map<String, TimelineFilterPreferences>) {
        _state.update {
            it.copy(
                rememberFilter =
                (preferences[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value] as RememberPreferences).value,
                currentMediaSource =
                (preferences[TimelinePreferencesJSON.JSON_KEY_LOCATION.value] as LocationPreference).value,
                currentFilterMediaType =
                (preferences[TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value] as MediaTypePreference).value,
            )
        }

        createAndUpdateFilterType()
    }

    /**
     * Cancel camera upload and heartbeat workers
     */
    fun stopCameraUploadAndHeartbeat() = viewModelScope.launch {
        stopCameraUploadAndHeartbeatUseCase()
    }
}

