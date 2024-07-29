package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
 * @property checkEnableCameraUploadsStatusUseCase
 * @property stopCameraUploadsUseCase
 * @property broadcastBusinessAccountExpiredUseCase
 * @param monitorCameraUploadsStatusInfoUseCase
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
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher val mainDispatcher: CoroutineDispatcher,
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase,
    private val timelinePreferencesMapper: TimelinePreferencesMapper,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : ViewModel() {

    internal val _state = MutableStateFlow(TimelineViewState(loadPhotosDone = false))

    /**
     * The Timeline State
     */
    val state = _state.asStateFlow()

    internal val selectedPhotosIds = mutableSetOf<Long>()

    private var isCameraUploadsFirstSyncTriggered = false
    private var isCameraUploadsUploading = false
    private var showHiddenItems: Boolean? = null

    init {
        monitorPhotos()
        monitorCameraUploadsStatus()

        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                monitorShowHiddenItems()
                monitorAccountDetail()
                monitorIsHiddenNodesOnboarded()
            }
        }
    }

    private fun monitorPhotos() = viewModelScope.launch {
        getTimelinePhotosUseCase()
            .catch { throwable ->
                Timber.e(throwable)
            }.collectLatest(::handlePhotos)
    }

    private fun monitorCameraUploadsStatus() = viewModelScope.launch {
        monitorCameraUploadsStatusInfoUseCase()
            .collect {
                when (it) {
                    is CameraUploadsStatusInfo.CheckFilesForUpload -> {
                        handleCameraUploadsCheckStatus()
                    }

                    is CameraUploadsStatusInfo.UploadProgress -> {
                        handleCameraUploadsProgressStatus(it)
                    }

                    is CameraUploadsStatusInfo.Finished -> {
                        handleCameraUploadsFinishedStatus(it)
                    }

                    else -> Unit
                }
            }
    }

    private fun monitorShowHiddenItems() = monitorShowHiddenItemsUseCase()
        .onEach {
            showHiddenItems = it
            if (!_state.value.loadPhotosDone) return@onEach

            handleAndUpdatePhotosUIState(
                sourcePhotos = _state.value.photos,
                showingPhotos = filterMedias(_state.value.photos),
            )
        }.launchIn(viewModelScope)

    private fun monitorAccountDetail() = monitorAccountDetailUseCase()
        .onEach { accountDetail ->
            _state.update {
                it.copy(accountType = accountDetail.levelDetail?.accountType)
            }
            if (!_state.value.loadPhotosDone) return@onEach

            handleAndUpdatePhotosUIState(
                sourcePhotos = _state.value.photos,
                showingPhotos = filterMedias(_state.value.photos),
            )
        }.launchIn(viewModelScope)

    private fun handleCameraUploadsCheckStatus() {
        setCameraUploadsSyncFab(isVisible = true)
        setCameraUploadsPausedMenuIconVisibility(isVisible = false)
        setCameraUploadsCompleteMenu(isVisible = false)
        setCameraUploadsWarningMenu(isVisible = false)
    }

    private fun handleCameraUploadsProgressStatus(info: CameraUploadsStatusInfo.UploadProgress) {
        updateCameraUploadProgressIfNeeded(
            progress = info.progress,
            pending = info.totalToUpload - info.totalUploaded,
        )

        setCameraUploadsUploadingFab(
            isVisible = true,
            progress = info.progress.floatValue,
        )
        setCameraUploadsTotalUploaded(info.totalToUpload)

        isCameraUploadsUploading = true
    }

    private suspend fun handleCameraUploadsFinishedStatus(info: CameraUploadsStatusInfo.Finished) {
        updateCameraUploadProgressIfNeeded(progress = Progress(1f), pending = 0)
        val cameraUploadsFinishedReason = info.reason

        if (cameraUploadsFinishedReason == CameraUploadsFinishedReason.UNKNOWN) return

        setCameraUploadsFinishedReason(reason = cameraUploadsFinishedReason)

        when (cameraUploadsFinishedReason) {
            CameraUploadsFinishedReason.COMPLETED -> {
                if (isCameraUploadsUploading) {
                    setCameraUploadsCompleteFab(isVisible = true)
                    setCameraUploadsCompletedMessage(show = true)

                    isCameraUploadsUploading = false
                    delay(4.seconds)
                }

                setCameraUploadsCompleteMenu(isVisible = true)
                setCameraUploadsCompleteFab(isVisible = false)
            }

            CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET -> {
                // For this Scenario, show the Paused Menu Icon in the Toolbar
                setCameraUploadsPausedMenuIconVisibility(isVisible = true)
                setCameraUploadsCompleteMenu(isVisible = false)
                setCameraUploadsWarningMenu(isVisible = false)
                hideCameraUploadsFab()
            }

            else -> {
                setCameraUploadsCompleteMenu(isVisible = false)
                setCameraUploadsWarningFab(isVisible = true, progress = 0.5f)
            }
        }
    }

    /**
     * Syncs the Camera Uploads Status
     */
    fun syncCameraUploadsStatus() {
        if (isCameraUploadsFirstSyncTriggered) return
        isCameraUploadsFirstSyncTriggered = true

        viewModelScope.launch {
            startCameraUploadUseCase()
        }
    }

    /**
     * Updates the Camera Uploads completed message to the UI State
     */
    fun setCameraUploadsCompletedMessage(show: Boolean) {
        _state.update {
            it.copy(showCameraUploadsCompletedMessage = show)
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
    private fun updateCameraUploadProgressIfNeeded(progress: Progress, pending: Int) {
        if (state.value.selectedPhotoCount > 0 || !isInAllView()) {
            setShowProgressBar(show = false)
        } else {
            // Check to avoid keeping setting same visibility
            updateProgress(
                pending = pending,
                showProgress = pending > 0,
                progress = progress.floatValue,
            )

            Timber.d("CU Upload Progress: Pending: {$pending}, Progress: {${progress.floatValue}}")
        }
    }

    /**
     * Handles the behavior when the Camera Uploads permissions have changed
     */
    fun handleCameraUploadsPermissionsResult() {
        val hasPermissions = hasMediaPermissionUseCase()

        setCameraUploadsLimitedAccess(isLimitedAccess = !hasPermissions)
        setCameraUploadsWarningMenu(isVisible = !hasPermissions)
        setTriggerMediaPermissionsDeniedLogicState(shouldTrigger = !hasPermissions)
    }

    /**
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatusUseCase]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        runCatching { checkEnableCameraUploadsStatusUseCase() }.onSuccess { status ->
            when (status) {
                CAN_ENABLE_CAMERA_UPLOADS -> {
                    setTriggerCameraUploadsState(shouldTrigger = true)
                }

                SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                    setBusinessAccountPromptState(shouldShow = true)
                }

                SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
                SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
                -> {
                    broadcastBusinessAccountExpiredUseCase()
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

    internal fun handleAndUpdatePhotosUIState(
        sourcePhotos: List<Photo>,
        showingPhotos: List<Photo>,
    ) = viewModelScope.launch(defaultDispatcher) {
        val nonSensitivePhotos = filterNonSensitivePhotos(showingPhotos)
        val sortedPhotos = sortPhotos(nonSensitivePhotos)

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
                enableCameraUploadPageShowing = sortedPhotos.isEmpty(),
            )
        }
        handleEnableZoomAndSortOptions()
    }

    private fun handleAllPhotoItems(showingPhotos: List<Photo>): List<PhotoListItem> {
        val currentZoomLevel = _state.value.currentZoomLevel
        val photoListItem = mutableListOf<PhotoListItem>()
        showingPhotos.forEachIndexed { index, photo ->
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
            photos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
        } else {
            photos.sortedWith(compareBy<Photo> { it.modificationTime }.thenByDescending { it.id })
        }
    }

    private fun filterNonSensitivePhotos(photos: List<Photo>): List<Photo> {
        val showHiddenItems = showHiddenItems ?: return photos
        val isPaid = _state.value.accountType?.isPaid ?: return photos

        return if (showHiddenItems || !isPaid) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    /**
     * Sorts the content by order
     */
    fun sortByOrder() {
        viewModelScope.launch {
            handleAndUpdatePhotosUIState(
                _state.value.photos,
                _state.value.currentShowingPhotos
            )
        }
    }

    /**
     * Enables the Camera Uploads functionality
     */
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
                videoCompressionSizeLimit = VIDEO_COMPRESSION_SIZE_LIMIT,
                videoUploadQuality = VideoQuality.ORIGINAL,
            )
            Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
            startCameraUploadUseCase()
        }
    }

    /**
     * Retrieves the selected Photo IDs
     *
     * @return a List of Photo IDs
     */
    fun getSelectedIds(): List<Long> =
        selectedPhotosIds.toList()

    /**
     * Retrieves the selected Nodes
     *
     * @return a list of Nodes
     */
    suspend fun getSelectedNodes(): List<MegaNode> = runCatching {
        getNodeListByIds(selectedPhotosIds.toList())
    }.onFailure { Timber.e(it) }.getOrDefault(emptyList())

    suspend fun getSelectedTypedNodes(): List<TypedNode> = runCatching {
        selectedPhotosIds.mapNotNull {
            getNodeByIdUseCase(NodeId(it))
        }
    }.onFailure { Timber.e(it) }.getOrDefault(emptyList())

    /**
     * Establishes the initial Camera Uploads preferences
     */
    fun setInitialPreferences() {
        viewModelScope.launch(ioDispatcher) {
            setInitialCUPreferences()
        }
    }

    private fun isInAllView(): Boolean = _state.value.selectedTimeBarTab == TimeBarTab.All

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

                hideCameraUploadsFab()
            }
        }
    }

    /**
     * Perform actions when long pressing a Photo
     *
     * @param photo The Photo being long pressed
     */
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

    /**
     * Perform actions when clicking a Photo
     *
     * @param photo The Photo being clicked
     */
    fun onClick(photo: Photo) {
        if (selectedPhotosIds.size == 0) {
            _state.update {
                it.copy(selectedPhoto = photo)
            }
        } else {
            onLongPress(photo)
        }
    }

    /**
     * Invalidates the selected Photo in the UI State
     */
    fun onNavigateToSelectedPhoto() {
        _state.update {
            it.copy(selectedPhoto = null)
        }
    }

    /**
     * Perform actions when clicking a DateCard
     *
     * @param dateCard The DateCard that was clicked
     */
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

    /**
     * Perform actions when clicking a TimeBarTab
     *
     * @param tab The TimeBarTab that was clicked
     */
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

    private fun updateSelectedTimeBarState(
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
    fun stopCameraUploads() = viewModelScope.launch {
        stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
    }

    /**
     * Updates the CU Complete Menu Icon visibility in the Top App Bar
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1300-16994&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param isVisible true if this Menu Icon should be shown
     */
    private fun setCameraUploadsCompleteMenu(isVisible: Boolean) {
        _state.update {
            it.copy(showCameraUploadsComplete = isVisible)
        }
    }

    /**
     * Updates the CU Paused Menu Icon visibility in the Top App Bar
     *
     * Figma File for reference:
     * https://www.figma.com/file/T5vL9rzzMtRqrVmZy51nLZ/%5BDSN-1501%5D-AND---Scheduled-camera-uploads-(While-charging)?type=design&node-id=1717-3148&mode=dev
     *
     * @param isVisible true if this Menu Icon should be shown
     */
    private fun setCameraUploadsPausedMenuIconVisibility(isVisible: Boolean) {
        _state.update { it.copy(showCameraUploadsPaused = isVisible) }
    }

    /**
     * Updates the CU Warning Menu Icon visibility in the Top App Bar
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1300-16945&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param isVisible true if this Menu Icon should be shown
     */
    fun setCameraUploadsWarningMenu(isVisible: Boolean) {
        _state.update {
            it.copy(showCameraUploadsWarning = isVisible)
        }
    }

    /**
     * Updates the CU Syncing Fab Icon visibility
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1702-27891&mode=design&t=vcAb54xEixEVIEvN-0
     *
     * @param isVisible true if this Fab Icon should be shown
     */
    private fun setCameraUploadsSyncFab(isVisible: Boolean) {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Sync.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
            )
        }
    }

    /**
     * Updates the CU Uploading Fab Icon visibility and its progress
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1702-27947&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param isVisible true if this Fab Icon should be shown
     * @param progress the Camera Uploads Uploading progress
     */
    private fun setCameraUploadsUploadingFab(isVisible: Boolean, progress: Float) {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Uploading.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
                cameraUploadsProgress = progress,
            )
        }
    }

    /**
     * Updates the CU Complete Fab Icon visibility
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1702-28067&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param isVisible true if this Fab Icon should be shown
     */
    private fun setCameraUploadsCompleteFab(isVisible: Boolean) {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Complete.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
            )
        }
    }

    /**
     * Updates the CU Warning Fab Icon visibility and its progress
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=2089-12855&mode=design&t=vcAb54xEixEVIEvN-0
     *
     * @param isVisible true if this Fab Icon should be shown
     * @param progress the Camera Uploads Uploading progress
     */
    private fun setCameraUploadsWarningFab(isVisible: Boolean, progress: Float) {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Warning.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
                cameraUploadsProgress = progress,
            )
        }
    }

    /**
     * Hides the Camera Uploads Fab Icon
     */
    private fun hideCameraUploadsFab() {
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.None)
        }
    }

    /**
     * Updates the CU Limited Access Banner visibility
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1702-27609&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param isLimitedAccess true if Camera Uploads only has limited access
     */
    fun setCameraUploadsLimitedAccess(isLimitedAccess: Boolean) {
        _state.update {
            it.copy(isCameraUploadsLimitedAccess = isLimitedAccess)
        }
    }

    /**
     * Updates the CU Change Permissions Message visibility, which will also be shown in the Fab
     * when they appear together
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1791-39721&mode=design&t=vcAb54xEixEVIEvN-0
     *
     * @param show true if this message should be shown
     */
    fun showCameraUploadsChangePermissionsMessage(show: Boolean) {
        _state.update {
            it.copy(showCameraUploadsChangePermissionsMessage = show)
        }
    }

    /**
     * Updates the CU Change Message for the Snackbar, which will also be shown in the Fab when
     * they appear together
     *
     * Figma File for reference:
     * https://www.figma.com/file/1Giwkl6NvNXkFXrX51PiyQ/%5BDSN-1291%5D-Timeline-%2F-Camera-Uploads-Redesign?type=design&node-id=1702-28657&mode=design&t=apaI1dEqa2bUBPIP-0
     *
     * @param message the new Camera Uploads message
     */
    fun setCameraUploadsMessage(message: String) {
        _state.update {
            it.copy(cameraUploadsMessage = message)
        }
    }

    private fun setCameraUploadsTotalUploaded(totalUploaded: Int) {
        _state.update { state ->
            state.copy(cameraUploadsTotalUploaded = totalUploaded)
        }
    }

    private fun setCameraUploadsFinishedReason(reason: CameraUploadsFinishedReason) {
        _state.update {
            it.copy(cameraUploadsFinishedReason = reason)
        }
    }

    /**
     * Control the visibility of the selected Nodes
     *
     * @param hide true if the selected Nodes should be hidden
     */
    fun hideOrUnhideNodes(hide: Boolean) = viewModelScope.launch {
        for (node in getSelectedNodes()) {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = NodeId(node.handle), isSensitive = hide)
                }.onFailure { Timber.e("Update sensitivity failed: $it") }
            }
        }
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }


    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    companion object {
        private const val VIDEO_COMPRESSION_SIZE_LIMIT = 200
    }
}

