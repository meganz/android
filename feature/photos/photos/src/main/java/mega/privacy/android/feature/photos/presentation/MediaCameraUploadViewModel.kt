package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.MonitorCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.MonitorEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.ResetEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.SetCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.SetEnableCameraUploadBannerDismissedTimestampUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.photos.model.CameraUploadsStatus
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature_flags.AppFeatures
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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
) : ViewModel() {

    // Due to time constraint, this approach will be updated to the lazy approach in phase 2.
    private val _uiState = MutableStateFlow(MediaCameraUploadUiState())
    internal val uiState = _uiState
        .onStart { startMonitoring() }
        .asUiStateFlow(
            scope = viewModelScope,
            initialValue = MediaCameraUploadUiState()
        )

    private var isCameraUploadsFirstSyncTriggered = false
    private var isCameraUploadsUploading = false

    private fun startMonitoring() {
        monitorCameraUploadShownStatus()
        monitorCameraUploadsStatus()
        monitorEnableCUBannerVisibility()
        syncCameraUploadsStatus()
        checkCameraUploadsTransferScreenEnabled()
        checkCameraUploadsPausedWarningBannerEnabled()
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
        setCameraUploadsSyncFab(isVisible = true)
        setCameraUploadsPausedMenuIconVisibility(isVisible = false)
        setCameraUploadsCompleteMenu(isVisible = false)
        setCameraUploadsWarningMenu(false)
    }

    private fun setCameraUploadsSyncFab(isVisible: Boolean) {
        _uiState.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Sync.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
            )
        }
    }

    private fun setCameraUploadsPausedMenuIconVisibility(isVisible: Boolean) {
        _uiState.update { it.copy(showCameraUploadsPaused = isVisible) }
    }

    private fun setCameraUploadsCompleteMenu(isVisible: Boolean) {
        _uiState.update { it.copy(showCameraUploadsComplete = isVisible) }
    }

    private fun handleCameraUploadsProgressStatus(info: CameraUploadsStatusInfo.UploadProgress) {
        updateCameraUploadProgressIfNeeded(pending = info.totalToUpload - info.totalUploaded)
        setCameraUploadsUploadingFab(
            isVisible = true,
            progress = info.progress.floatValue,
        )
        setCameraUploadsTotalUploaded(info.totalToUpload)

        isCameraUploadsUploading = true
    }

    private fun updateCameraUploadProgressIfNeeded(pending: Int) {
        _uiState.update { it.copy(pending = pending) }
        Timber.d("CU Upload Progress: Pending: $pending")
    }

    private fun setCameraUploadsUploadingFab(isVisible: Boolean, progress: Float) {
        _uiState.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Uploading.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
                cameraUploadsProgress = progress,
            )
        }
    }

    private fun setCameraUploadsTotalUploaded(totalUploaded: Int) {
        _uiState.update { state -> state.copy(cameraUploadsTotalUploaded = totalUploaded) }
    }

    private suspend fun handleCameraUploadsFinishedStatus(info: CameraUploadsStatusInfo.Finished) {
        updateCameraUploadProgressIfNeeded(pending = 0)
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
                setCameraUploadsPausedMenuIconVisibility(isVisible = !_uiState.value.isCUPausedWarningBannerEnabled)
                setCameraUploadsCompleteMenu(isVisible = false)
                setCameraUploadsWarningMenu(isVisible = _uiState.value.isCUPausedWarningBannerEnabled)
                updateIsWarningBannerShown(true)
                hideCameraUploadsFab()
            }

            else -> {
                setCameraUploadsCompleteMenu(isVisible = false)
                if (shouldShowWarningBanner()) {
                    hideCameraUploadsFab()
                } else {
                    setCameraUploadsWarningFab(isVisible = true, progress = 0.5f)
                }
                setCameraUploadsWarningMenu(shouldShowWarningMenu())
                updateIsWarningBannerShown(shouldShowWarningBanner())
            }
        }
    }

    private fun setCameraUploadsFinishedReason(reason: CameraUploadsFinishedReason) {
        _uiState.update { it.copy(cameraUploadsFinishedReason = reason) }
    }

    private fun setCameraUploadsCompleteFab(isVisible: Boolean) {
        _uiState.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Complete.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
            )
        }
    }

    internal fun setCameraUploadsCompletedMessage(show: Boolean) {
        _uiState.update { it.copy(showCameraUploadsCompletedMessage = show) }
    }

    private fun hideCameraUploadsFab() {
        _uiState.update { it.copy(cameraUploadsStatus = CameraUploadsStatus.None) }
    }

    internal fun shouldShowWarningBanner(
        finishReason: CameraUploadsFinishedReason? = _uiState.value.cameraUploadsFinishedReason,
        isWarningBannerEnabled: Boolean = _uiState.value.isCUPausedWarningBannerEnabled,
    ): Boolean {
        if (!isWarningBannerEnabled) return false

        return finishReason?.let { reason ->
            reason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
            )
        } ?: false
    }

    private fun setCameraUploadsWarningFab(isVisible: Boolean, progress: Float) {
        _uiState.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Warning.takeIf {
                    isVisible
                } ?: CameraUploadsStatus.None,
                cameraUploadsProgress = progress,
            )
        }
    }

    internal fun shouldShowWarningMenu(
        finishReason: CameraUploadsFinishedReason? = _uiState.value.cameraUploadsFinishedReason,
        isWarningBannerEnabled: Boolean = _uiState.value.isCUPausedWarningBannerEnabled,
    ): Boolean {
        if (!isWarningBannerEnabled || finishReason == CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA)
            return false

        return finishReason?.let { reason ->
            reason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
            )
        } ?: false
    }

    private fun checkCameraUploadsTransferScreenEnabled() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.CameraUploadsTransferScreen)
            }.onSuccess { isEnabled ->
                _uiState.update { it.copy(isCameraUploadsTransferScreenEnabled = isEnabled) }
            }.onFailure { error ->
                Timber.e(error)
            }
        }
    }

    private fun checkCameraUploadsPausedWarningBannerEnabled() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.CameraUploadsPausedWarningBanner)
            }.onSuccess { isEnabled ->
                _uiState.update { currentState ->
                    val isLimitedAccess = _uiState.value.isCameraUploadsLimitedAccess
                    val showWarningMenu =
                        isLimitedAccess || shouldShowWarningMenu(isWarningBannerEnabled = true)
                    val showWarningBanner =
                        isLimitedAccess || shouldShowWarningBanner(isWarningBannerEnabled = true)
                    if (isEnabled && showWarningBanner) {
                        hideCameraUploadsFab()
                    }
                    if (isEnabled) {
                        currentState.copy(
                            isCUPausedWarningBannerEnabled = true,
                            isWarningBannerShown = showWarningBanner,
                            showCameraUploadsWarning = showWarningMenu,
                            showCameraUploadsPaused = false,
                        )
                    } else {
                        currentState.copy(
                            isCUPausedWarningBannerEnabled = false
                        )
                    }
                }
            }.onFailure { error ->
                Timber.e(error)
            }
        }
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

        setCameraUploadsLimitedAccess(isLimitedAccess = !hasPermissions)
        setCameraUploadsWarningMenu(isVisible = !hasPermissions || shouldShowWarningMenu())
        updateIsWarningBannerShown(!hasPermissions || shouldShowWarningMenu())
        setTriggerMediaPermissionsDeniedLogicState(shouldTrigger = !hasPermissions)
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

    internal fun setTriggerMediaPermissionsDeniedLogicState(shouldTrigger: Boolean) {
        _uiState.update { it.copy(shouldTriggerMediaPermissionsDeniedLogic = shouldTrigger) }
    }

    internal fun setInitialPreferences() {
        viewModelScope.launch {
            runCatching { setInitialCUPreferences() }
                .onFailure { Timber.e(it, "Unable to set the initial CU preferences") }
        }
    }

    internal fun resetCUButtonAndProgress() {
        viewModelScope.launch {
            if (isCameraUploadsEnabledUseCase()) {
                _uiState.update {
                    it.copy(
                        enableCameraUploadButtonShowing = false,
                        enableCameraUploadPageShowing = false,
                    )
                }
            } else {
                _uiState.update { it.copy(enableCameraUploadButtonShowing = true) }
                hideCameraUploadsFab()
            }
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

    internal fun showCameraUploadsChangePermissionsMessage(show: Boolean) {
        _uiState.update { it.copy(showCameraUploadsChangePermissionsMessage = show) }
    }

    internal fun updatePopBackFromCameraUploadsTransferScreenEvent(value: StateEvent) {
        _uiState.update { it.copy(popBackFromCameraUploadsTransferScreenEvent = value) }
    }

    internal fun shouldEnableCUPage(mediaSource: FilterMediaSource, show: Boolean) {
        val isShown = show && mediaSource != FilterMediaSource.CloudDrive
        _uiState.update { it.copy(enableCameraUploadPageShowing = isShown) }
    }

    internal suspend fun updateCUPageEnablementBasedOnDisplayedPhotos(photos: ImmutableList<PhotosNodeContentType>) {
        runCatching { isCameraUploadsEnabledUseCase() }
            .onSuccess { isCameraUploadsEnabled ->
                _uiState.update { it.copy(enableCameraUploadPageShowing = photos.isEmpty() && !isCameraUploadsEnabled) }
            }
    }

    private fun monitorEnableCUBannerVisibility() {
        viewModelScope.launch {
            monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow
                .catch { Timber.e(it, "Unable to monitor enable CU banner visibility") }
                .collectLatest { isVisible ->
                    val isCUEnabled = runCatching {
                        isCameraUploadsEnabledUseCase()
                    }.getOrDefault(defaultValue = false)
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
}
