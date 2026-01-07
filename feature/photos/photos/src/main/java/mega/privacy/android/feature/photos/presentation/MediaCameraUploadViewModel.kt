package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    private var isCameraUploadsUploading = false

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        monitorCameraUploadShownStatus()
        monitorCameraUploadsStatus()
        monitorEnableCUBannerVisibility()
        syncCameraUploadsStatus()
        monitorCameraUploadsEnable()
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


    private fun monitorCameraUploadsStatus() = viewModelScope.launch {
        monitorCameraUploadsStatusInfoUseCase()
            .collectLatest {
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

    private fun handleCameraUploadsCheckStatus() {
        setCameraUploadsSyncState()
        setCameraUploadsCompleteMenu(isVisible = false)
        setCameraUploadsWarningMenu(false)
    }

    private fun setCameraUploadsSyncState() {
        _uiState.update { it.copy(status = CUStatusUiState.Sync) }
    }

    private fun setCameraUploadsCompleteMenu(isVisible: Boolean) {
        _uiState.update { it.copy(showCameraUploadsComplete = isVisible) }
    }

    private fun handleCameraUploadsProgressStatus(info: CameraUploadsStatusInfo.UploadProgress) {
        _uiState.update {
            it.copy(
                status = CUStatusUiState.UploadInProgress(
                    progress = info.progress.floatValue,
                    pending = info.totalToUpload - info.totalUploaded,
                ),
                cameraUploadsTotalUploaded = info.totalToUpload,
            )
        }
        isCameraUploadsUploading = true
    }

    private suspend fun handleCameraUploadsFinishedStatus(info: CameraUploadsStatusInfo.Finished) {
        val cameraUploadsFinishedReason = info.reason

        if (cameraUploadsFinishedReason == CameraUploadsFinishedReason.UNKNOWN) return

        setCameraUploadsFinishedReason(reason = cameraUploadsFinishedReason)

        when (cameraUploadsFinishedReason) {
            CameraUploadsFinishedReason.COMPLETED -> {
                if (isCameraUploadsUploading) {
                    _uiState.update {
                        it.copy(
                            status = CUStatusUiState.UploadComplete,
                            showCameraUploadsCompletedMessage = true
                        )
                    }

                    isCameraUploadsUploading = false
                    delay(4.seconds)
                }

                _uiState.update { uiState ->
                    uiState.copy(status = CUStatusUiState.UpToDate)
                }
                setCameraUploadsCompleteMenu(isVisible = true)
            }

            CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET -> {
                setCameraUploadsCompleteMenu(isVisible = false)
                setCameraUploadsWarningMenu(isVisible = true)
                updateIsWarningBannerShown(true)
                hideCameraUploadsFab()
            }

            else -> {
                setCameraUploadsCompleteMenu(isVisible = false)
                if (shouldShowWarningBanner()) {
                    hideCameraUploadsFab()
                } else {
                    _uiState.update {
                        it.copy(
                            status = CUStatusUiState.Warning,
                            cameraUploadsProgress = 0.5f,
                        )
                    }
                }
                setCameraUploadsWarningMenu(shouldShowWarningMenu())
                updateIsWarningBannerShown(shouldShowWarningBanner())
            }
        }
    }

    private fun setCameraUploadsFinishedReason(reason: CameraUploadsFinishedReason) {
        _uiState.update { it.copy(cameraUploadsFinishedReason = reason) }
    }

    internal fun setCameraUploadsCompletedMessage(show: Boolean) {
        _uiState.update { it.copy(showCameraUploadsCompletedMessage = show) }
    }

    private fun hideCameraUploadsFab() {
        _uiState.update { it.copy(status = CUStatusUiState.None) }
    }

    internal fun shouldShowWarningBanner(): Boolean {
        return _uiState.value.cameraUploadsFinishedReason?.let { reason ->
            reason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
            )
        } ?: false
    }

    internal fun shouldShowWarningMenu(): Boolean {
        return _uiState.value.cameraUploadsFinishedReason?.let { reason ->
            reason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
            )
        } ?: false
    }

    private fun syncCameraUploadsStatus() {
        if (isCameraUploadsFirstSyncTriggered) return
        isCameraUploadsFirstSyncTriggered = true

        viewModelScope.launch {
            startCameraUploadUseCase()
        }
    }

    private fun monitorCameraUploadsEnable() {
        viewModelScope.launch {
            isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled
                .catch { Timber.e(it, "Unable to monitor camera uploads enabled") }
                .collectLatest { isEnabled ->
                    if (isEnabled) {
                        _uiState.update {
                            it.copy(
                                enableCameraUploadButtonShowing = false,
                                enableCameraUploadPageShowing = false,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(enableCameraUploadButtonShowing = true) }
                    }
                }
        }
    }

    internal fun handleCameraUploadsPermissionsResult() {
        val hasPermissions = hasMediaPermissionUseCase()

        setCameraUploadsLimitedAccess(isLimitedAccess = !hasPermissions)
        setCameraUploadsWarningMenu(isVisible = !hasPermissions || shouldShowWarningMenu())
        updateIsWarningBannerShown(!hasPermissions || shouldShowWarningMenu())
    }

    internal fun setCameraUploadsLimitedAccess(isLimitedAccess: Boolean) {
        _uiState.update { it.copy(isCameraUploadsLimitedAccess = isLimitedAccess) }
    }

    internal fun setCameraUploadsWarningMenu(isVisible: Boolean) {
        _uiState.update { it.copy(showCameraUploadsWarning = isVisible) }
    }

    internal fun updateIsWarningBannerShown(isShown: Boolean) {
        _uiState.update { it.copy(isWarningBannerShown = isShown) }
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

    private fun monitorEnableCUBannerVisibility() {
        viewModelScope.launch {
            combine(
                isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled,
                monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow,
                ::Pair
            ).catch {
                Timber.e(it, "Unable to monitor enable CU banner visibility")
            }.collectLatest { (isCUEnabled, isVisible) ->
                _uiState.update { it.copy(shouldShowEnableCUBanner = isVisible && !isCUEnabled) }
                if (isVisible) {
                    runCatching { resetEnableCameraUploadBannerVisibilityUseCase() }
                        .onFailure {
                            Timber.e(it, "Unable to reset enable CU banner visibility")
                        }
                }
            }
        }
    }

    internal fun dismissEnableCUBanner() {
        viewModelScope.launch {
            runCatching { setEnableCameraUploadBannerDismissedTimestampUseCase() }
                .onFailure { Timber.e(it, "Unable to set enable CU banner dismiss timestamp") }
        }
    }

    internal fun checkCameraUploadsPermissions() {
        val hasPermissions = hasCameraUploadsPermissionUseCase()
        _uiState.update { currentState ->
            val showWarningMenu = !hasPermissions || shouldShowWarningMenu()
            val showWarningBanner = !hasPermissions || shouldShowWarningBanner()
            if (showWarningBanner) {
                hideCameraUploadsFab()
            }
            currentState.copy(
                isCameraUploadsLimitedAccess = !hasPermissions,
                isWarningBannerShown = showWarningBanner,
                showCameraUploadsWarning = showWarningMenu,
            )
        }
    }
}
