package mega.privacy.android.app.presentation.permissions

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.data.extensions.filterAllowedPermissions
import mega.privacy.android.app.data.extensions.toPermissionScreen
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.notifications.SetNotificationPermissionShownUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.mobile.analytics.event.CameraUploadsEnabledEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for managing [PermissionsFragment] data.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val defaultAccountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val setNotificationPermissionShownUseCase: SetNotificationPermissionShownUseCase,
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val enableCameraUploadsInPhotosUseCase: EnableCameraUploadsInPhotosUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    internal val uiState: StateFlow<PermissionsUIState>
        field = MutableStateFlow(PermissionsUIState())

    private lateinit var missingPermissions: List<Permission>
    private lateinit var permissionScreens: MutableList<PermissionScreen>

    init {
        getThemeMode()
    }

    private fun getThemeMode() {
        viewModelScope.launch {
            monitorThemeModeUseCase()
                .catch { Timber.e(it) }
                .collect { themeMode ->
                    uiState.update { it.copy(themeMode = themeMode) }
                }
        }
    }

    /**
     * Sets initial data for requesting missing permissions.
     */
    fun setData(permissions: List<Pair<Permission, Boolean>>) {
        viewModelScope.launch {
            missingPermissions = permissions.filterAllowedPermissions()
            missingPermissions
                // Filter out permissions that are not needed for the onboarding revamp. On the new
                // onboarding flow, we only need Media (Read and Write) and Notifications permissions.
                .filter { it == Permission.CameraBackup || it == Permission.Notifications }
                .apply { permissionScreens = toPermissionScreen() }
                .also { updateCurrentPermissionRevamp() }
        }
    }

    private fun updateCurrentPermissionRevamp() {
        val visiblePermission = permissionScreens
            .firstOrNull()
            ?.toNewPermissionScreen()
            ?: run {
                Timber.d("No more permissions to show")
                uiState.update { it.copy(finishEvent = triggered) }
                return
            }

        Timber.d("Showing permission: $visiblePermission")
        uiState.update {
            it.copy(visiblePermission = visiblePermission)
        }
    }

    private fun PermissionScreen.toNewPermissionScreen() =
        when (this) {
            PermissionScreen.Notifications -> NewPermissionScreen.Notification
            PermissionScreen.CameraBackup -> NewPermissionScreen.CameraBackup
            else -> NewPermissionScreen.Loading
        }

    /**
     * Sets next permission to show as the current one.
     */
    fun nextPermission() {
        if (permissionScreens.isNotEmpty()) permissionScreens.removeAt(0)

        updateCurrentPermissionRevamp()
    }

    /**
     * Sets first time value in preference as false
     * First time value is an indication of first launch of application
     */
    fun updateFirstTimeLoginStatus() {
        viewModelScope.launch(ioDispatcher) {
            defaultAccountRepository.setUserHasLoggedIn()
        }
    }

    fun setPermissionPageShown() {
        viewModelScope.launch {
            runCatching {
                setNotificationPermissionShownUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * When the User has granted all Media Permissions, enable the camera uploads feature
     * based on the Camera Uploads status
     */
    fun onMediaPermissionsGranted() {
        applicationScope.launch {
            val result = runCatching { checkEnableCameraUploadsStatusUseCase() }

            if (result.isFailure) {
                Timber.e("PermissionsViewModel::onMediaPermissionsGranted - Failed to check Camera Uploads status")
                nextPermission()
                return@launch
            }

            when (result.getOrNull()) {
                EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> enableCameraUploads()
                else -> {
                    Timber.e("PermissionsViewModel::onMediaPermissionsGranted - Cannot enable Camera Uploads")
                    nextPermission()
                }
            }
        }
    }

    private fun enableCameraUploads() {
        applicationScope.launch {
            val result = runCatching {
                enableCameraUploadsInPhotosUseCase(
                    shouldSyncVideos = false,
                    shouldUseWiFiOnly = false,
                    videoCompressionSizeLimit = VIDEO_COMPRESSION_SIZE_LIMIT,
                    videoUploadQuality = VideoQuality.ORIGINAL
                )
            }

            if (result.isFailure) {
                Timber.e(
                    result.exceptionOrNull(),
                    "PermissionsViewModel::enableCameraUploads - An error occurred when enabling Camera Uploads"
                )
            } else {
                Analytics.tracker.trackEvent(CameraUploadsEnabledEvent)
                uiState.update { it.copy(isCameraUploadsEnabled = true) }
                Timber.d("PermissionsViewModel::enableCameraUploads - Camera Uploads enabled")
            }

            nextPermission()
        }
    }

    /**
     * Starts the Camera Uploads process. The [ApplicationScope] is used to ensure that the process
     * is started when leaving the Settings Camera Uploads screen
     */
    fun startCameraUploadIfGranted() {
        applicationScope.launch {
            runCatching {
                startCameraUploadUseCase()
            }.onFailure { exception ->
                Timber.e(exception, "An error occurred when starting Camera Uploads")
            }
        }
    }

    internal fun resetFinishEvent() {
        uiState.update { it.copy(finishEvent = consumed) }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val VIDEO_COMPRESSION_SIZE_LIMIT = 200
    }
}