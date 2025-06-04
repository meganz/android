package mega.privacy.android.app.presentation.permissions

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.data.extensions.filterAllowedPermissions
import mega.privacy.android.app.data.extensions.toPermissionScreen
import mega.privacy.android.app.data.extensions.toPermissionType
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.app.presentation.permissions.model.PermissionType
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getThemeModeUseCase: GetThemeMode,
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
    private val showInitialSetupScreen: MutableLiveData<Boolean> = MutableLiveData()
    private val currentPermission: MutableLiveData<PermissionScreen?> = MutableLiveData()
    private val askPermissionType = SingleLiveEvent<PermissionType>()
    private val isOnboardingRevampEnabled = MutableStateFlow<Boolean?>(null)

    init {
        getThemeMode()
        setOnboardingRevampFlag()
    }

    private fun getThemeMode() {
        viewModelScope.launch {
            getThemeModeUseCase()
                .catch { Timber.e(it) }
                .collect { themeMode ->
                    uiState.update { it.copy(themeMode = themeMode) }
                }
        }
    }

    private fun setOnboardingRevampFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp)
            }.onSuccess { isEnabled ->
                // Update the feature flag value in the state flow
                isOnboardingRevampEnabled.update { isEnabled }

                uiState.update {
                    it.copy(isOnboardingRevampEnabled = isEnabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Checks if should show "Allow access" screen.
     */
    fun shouldShowInitialSetupScreen(): LiveData<Boolean> = showInitialSetupScreen

    /**
     * Notifies about the current [PermissionScreen].
     */
    fun getCurrentPermission(): LiveData<PermissionScreen?> = currentPermission

    /**
     * Notifies about asking for some permissions.
     */
    fun onAskPermission(): SingleLiveEvent<PermissionType> = askPermissionType

    /**
     * Sets initial data for requesting missing permissions.
     */
    fun setData(permissions: List<Pair<Permission, Boolean>>) {
        viewModelScope.launch {
            missingPermissions = permissions.filterAllowedPermissions()
            // Suspend until feature flag value is set
            val isFlagEnabled = isOnboardingRevampEnabled.first { it != null } == true

            if (isFlagEnabled) {
                missingPermissions
                    // Filter out permissions that are not needed for the onboarding revamp. On the new
                    // onboarding flow, we only need Media (Read and Write) and Notifications permissions.
                    .filter { it == Permission.CameraBackup || it == Permission.Notifications }
                    .apply { permissionScreens = toPermissionScreen() }
                    .also { updateCurrentPermissionRevamp() }
            } else {
                // CameraBackup permission is not needed for the old onboarding flow, so we filter it out.
                // We want to preserve legacy code for the old onboarding flow to avoid regression bugs,
                // this will all be remove once we cleanup the old onboarding flow.
                missingPermissions
                    .filterNot { it == Permission.CameraBackup }
                    .apply {
                        permissionScreens = toPermissionScreen()
                    }

                if (permissionScreens.isNotEmpty()) {
                    showInitialSetupScreen.value = true
                }
            }
        }
    }

    private fun updateCurrentPermission() {
        if (permissionScreens.isNotEmpty()) {
            currentPermission.value = permissionScreens[0]
        } else {
            currentPermission.value = null
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
     * Sets the showInitialSetupScreen value to false and updates the current permission to show.
     */
    fun grantAskForPermissions() {
        showInitialSetupScreen.value = false
        updateCurrentPermission()
    }

    /**
     * Sets next permission to show as the current one.
     */
    fun nextPermission() {
        if (permissionScreens.isNotEmpty()) permissionScreens.removeAt(0)

        if (isOnboardingRevampEnabled.value == true) {
            updateCurrentPermissionRevamp()
        } else {
            updateCurrentPermission()
        }
    }

    /**
     * Asks for required permission.
     */
    fun askPermission() {
        askPermissionType.value = currentPermission.value?.toPermissionType(missingPermissions)
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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