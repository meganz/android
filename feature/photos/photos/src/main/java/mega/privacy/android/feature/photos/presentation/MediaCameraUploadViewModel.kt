package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.permisison.HasCameraUploadsPermissionUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.MonitorCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.MonitorEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.ResetEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.SetCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.SetEnableCameraUploadBannerDismissedTimestampUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * At the moment, all the code here is a copy of the CU-related code from TimelineViewModel.
 */
@HiltViewModel
class MediaCameraUploadViewModel @Inject constructor(
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    private val setInitialCUPreferences: SetInitialCUPreferences,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val monitorCameraUploadShownUseCase: MonitorCameraUploadShownUseCase,
    private val setCameraUploadShownUseCase: SetCameraUploadShownUseCase,
    private val monitorEnableCameraUploadBannerVisibilityUseCase: MonitorEnableCameraUploadBannerVisibilityUseCase,
    private val resetEnableCameraUploadBannerVisibilityUseCase: ResetEnableCameraUploadBannerVisibilityUseCase,
    private val setEnableCameraUploadBannerDismissedTimestampUseCase: SetEnableCameraUploadBannerDismissedTimestampUseCase,
    private val hasCameraUploadsPermissionUseCase: HasCameraUploadsPermissionUseCase,
) : ViewModel() {

    // Due to time constraint, this approach will be updated to the lazy approach in phase 2.
    private val _uiState = MutableStateFlow(MediaCameraUploadUiState())
    internal val uiState = _uiState.asUiStateFlow(
        scope = viewModelScope,
        initialValue = MediaCameraUploadUiState()
    )

    private var isCameraUploadsFirstSyncTriggered = false

    private val permissionsStateFlow = MutableStateFlow(MediaCUPermissionsState.Unknown)
    private val cuStatusInfoFlow = MutableStateFlow<CameraUploadsStatusInfo?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cuStatusFlow: StateFlow<CUStatusUiState> =
        combine(
            flow = permissionsStateFlow,
            flow2 = cuStatusInfoFlow,
            transform = ::Pair
        ).scan(
            initial = CUStatusFlowTransition(
                permissionsState = MediaCUPermissionsState.Unknown,
                previousStatus = null,
                currentStatus = null
            )
        ) { acc, (permissionsState, currentStatus) ->
            CUStatusFlowTransition(
                permissionsState = permissionsState,
                previousStatus = acc.currentStatus,
                currentStatus = currentStatus
            )
        }.flatMapLatest { transition ->
            if (transition.permissionsState == MediaCUPermissionsState.Denied) {
                flowOf(CUStatusUiState.Warning.HasLimitedAccess)
            } else {
                val previousStatus = transition.previousStatus
                val currentStatus = transition.currentStatus
                when (currentStatus) {
                    is CameraUploadsStatusInfo.CheckFilesForUpload -> {
                        flowOf(CUStatusUiState.Sync)
                    }

                    is CameraUploadsStatusInfo.UploadProgress -> {
                        flowOf(
                            CUStatusUiState.UploadInProgress(
                                progress = currentStatus.progress.floatValue,
                                pending = currentStatus.totalToUpload - currentStatus.totalUploaded,
                            )
                        )
                    }

                    is CameraUploadsStatusInfo.Finished -> {
                        val isCameraUploadsUploading =
                            currentStatus.reason == CameraUploadsFinishedReason.COMPLETED &&
                                    previousStatus is CameraUploadsStatusInfo.UploadProgress
                        if (isCameraUploadsUploading) {
                            flow {
                                emit(CUStatusUiState.UploadComplete)
                                delay(4.seconds)
                                emit(CUStatusUiState.UpToDate)
                            }
                        } else {
                            val status = when (currentStatus.reason) {
                                CameraUploadsFinishedReason.COMPLETED -> CUStatusUiState.UpToDate
                                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET -> {
                                    CUStatusUiState.Warning.DeviceChargingRequirementNotMet
                                }

                                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW -> {
                                    CUStatusUiState.Warning.BatteryLevelTooLow
                                }

                                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET -> {
                                    CUStatusUiState.Warning.NetworkConnectionRequirementNotMet
                                }

                                CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA -> {
                                    CUStatusUiState.Warning.AccountStorageOverQuota
                                }

                                else -> CUStatusUiState.None
                            }
                            flowOf(status)
                        }
                    }

                    else -> flowOf(CUStatusUiState.None)
                }
            }
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = CUStatusUiState.None
        )

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        monitorCameraUploadsStatus()
        monitorCUBannerVisibility()
        monitorCameraUploadShownStatus()
        syncCameraUploadsStatus()
    }

    private fun monitorCameraUploadsStatus() {
        viewModelScope.launch {
            monitorCameraUploadsStatusInfoUseCase()
                .catch { Timber.e(it, "Unable to monitor camera uploads status info") }
                .collectLatest { statusInfo ->
                    when (statusInfo) {
                        is CameraUploadsStatusInfo.UploadProgress -> {
                            _uiState.update {
                                it.copy(cameraUploadsTotalUploaded = statusInfo.totalToUpload)
                            }
                        }

                        is CameraUploadsStatusInfo.Finished -> {
                            if (statusInfo.reason != CameraUploadsFinishedReason.UNKNOWN) {
                                _uiState.update {
                                    it.copy(cameraUploadsFinishedReason = statusInfo.reason)
                                }
                            }
                        }

                        else -> Unit
                    }
                    cuStatusInfoFlow.update { statusInfo }
                }
        }
    }

    private fun monitorCUBannerVisibility() {
        viewModelScope.launch {
            combine(
                flow = isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled,
                flow2 = monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow,
                flow3 = cuStatusFlow,
                transform = ::Triple
            ).catch {
                Timber.e(it, "Unable to monitor CU banner visibility")
            }.collect { (isCUEnabled, isVisible, cuStatus) ->
                if (!isCUEnabled) {
                    _uiState.update {
                        it.copy(
                            status = CUStatusUiState.Disabled(
                                shouldNotifyUser = isVisible
                            )
                        )
                    }
                    if (isVisible) {
                        runCatching { resetEnableCameraUploadBannerVisibilityUseCase() }
                            .onFailure {
                                Timber.e(it, "Unable to reset enable CU banner visibility")
                            }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            status = cuStatus,
                            enableCameraUploadPageShowing = false
                        )
                    }

                    if (cuStatus == CUStatusUiState.UploadComplete) {
                        _uiState.update {
                            it.copy(showCameraUploadsCompletedMessage = true)
                        }
                    }
                }
            }
        }
    }

    private fun monitorCameraUploadShownStatus() {
        viewModelScope.launch {
            monitorCameraUploadShownUseCase
                .cameraUploadShownFlow
                .catch { Timber.e(it, "Unable to monitor CU shown") }
                .collectLatest { hasShown ->
                    if (hasShown) {
                        setInitialPreferences()
                        setCameraUploadShown()
                    }
                }
        }
    }

    private fun setCameraUploadShown() {
        viewModelScope.launch {
            runCatching { setCameraUploadShownUseCase() }
                .onFailure { Timber.e(it, "Unable to set CU shown") }
        }
    }

    internal fun setCameraUploadsCompletedMessage(show: Boolean) {
        _uiState.update { it.copy(showCameraUploadsCompletedMessage = show) }
    }

    private fun syncCameraUploadsStatus() {
        if (isCameraUploadsFirstSyncTriggered) return
        isCameraUploadsFirstSyncTriggered = true

        viewModelScope.launch {
            startCameraUploadUseCase()
        }
    }

    internal fun handleCameraUploadsPermissionsResult() {
        val hasPermissions = hasMediaPermissionUseCase()
        permissionsStateFlow.update {
            if (hasPermissions) MediaCUPermissionsState.Granted
            else MediaCUPermissionsState.Denied
        }
    }

    internal fun setInitialPreferences() {
        viewModelScope.launch {
            runCatching { setInitialCUPreferences() }
                .onFailure { Timber.e(it, "Unable to set the initial CU preferences") }
        }
    }

    internal fun stopCameraUploads() {
        viewModelScope.launch {
            runCatching {
                stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
            }.onFailure { Timber.e(it, "Unable to stop camera uploads") }
        }
    }

    internal fun setCameraUploadsMessage(message: String) {
        _uiState.update { it.copy(cameraUploadsMessage = message) }
    }

    internal fun shouldEnableCUPage(mediaSource: FilterMediaSource, show: Boolean) {
        val isShown = show && mediaSource != FilterMediaSource.CloudDrive
        _uiState.update { it.copy(enableCameraUploadPageShowing = isShown) }
    }

    internal suspend fun updateCUPageEnablementBasedOnDisplayedPhotos(photos: List<PhotosNodeContentType>) {
        runCatching { isCameraUploadsEnabledUseCase() }
            .onSuccess { isCameraUploadsEnabled ->
                _uiState.update { it.copy(enableCameraUploadPageShowing = photos.isEmpty() && !isCameraUploadsEnabled) }
            }
    }

    internal fun checkCameraUploadsPermissions() {
        val hasPermissions = hasCameraUploadsPermissionUseCase()
        permissionsStateFlow.update {
            if (hasPermissions) MediaCUPermissionsState.Granted
            else MediaCUPermissionsState.Denied
        }
    }

    internal fun dismissCUBanner(status: CUStatusUiState) {
        if (status is CUStatusUiState.Disabled) {
            dismissEnableCUBanner()
        }
        _uiState.update { it.copy(status = CUStatusUiState.None) }
    }

    private fun dismissEnableCUBanner() {
        viewModelScope.launch {
            runCatching { setEnableCameraUploadBannerDismissedTimestampUseCase() }
                .onFailure { Timber.e(it, "Unable to set enable CU banner dismiss timestamp") }
        }
    }
}
