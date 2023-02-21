package mega.privacy.android.app.presentation.settings.camerauploads

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import javax.inject.Inject

/**
 * [ViewModel] class for SettingsCameraUploadsFragment
 *
 * @property checkEnableCameraUploadsStatus Check the Camera Uploads status before enabling
 * @property clearCacheDirectory Clear all the contents of the internal cache directory
 * @property disableCameraUploadsInDatabase Disable Camera Uploads by manipulating values in the database
 * @property disableMediaUploadSettings Disable Media Uploads by manipulating a certain value in the database
 * @property monitorConnectivity Monitor the device online status
 * @property resetCameraUploadTimeStamps Reset the Primary and Secondary Timestamps
 * @property resetMediaUploadTimeStamps Reset the Secondary Timestamps
 * @property restorePrimaryTimestamps Restore the Primary Timestamps
 * @property restoreSecondaryTimestamps Restore the Secondary Timestamps
 */
@HiltViewModel
class SettingsCameraUploadsViewModel @Inject constructor(
    private val checkEnableCameraUploadsStatus: CheckEnableCameraUploadsStatus,
    private val clearCacheDirectory: ClearCacheDirectory,
    private val disableCameraUploadsInDatabase: DisableCameraUploadsInDatabase,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
    private val monitorConnectivity: MonitorConnectivity,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val restorePrimaryTimestamps: RestorePrimaryTimestamps,
    private val restoreSecondaryTimestamps: RestoreSecondaryTimestamps,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsCameraUploadsState())

    /**
     * State of Settings Camera Uploads
     */
    val state: StateFlow<SettingsCameraUploadsState> = _state.asStateFlow()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Handle specific behavior when permissions are granted / denied
     *
     * @param permissions A [Map] of permissions that were requested
     */
    fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        if (areMediaPermissionsGranted(permissions)) {
            handleEnableCameraUploads()
        } else {
            setMediaPermissionsRationaleState(shouldShow = true)
        }
        if (!isNotificationPermissionGranted(permissions)) {
            setNotificationPermissionRationaleState(shouldShow = true)
        }
    }

    /**
     * Checks whether the Media Permissions have been granted. The checking would vary depending
     * on the user's Android OS
     *
     * @param permissions A [Map] of permissions that were requested
     *
     * @return Boolean value
     */
    private fun areMediaPermissionsGranted(permissions: Map<String, Boolean>) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[READ_MEDIA_IMAGES] == true && permissions[READ_MEDIA_VIDEO] == true
        } else {
            permissions[READ_EXTERNAL_STORAGE] == true
        }

    /**
     * Checks whether the Notification Permission been granted. For Devices running Android 12
     * and below, this is automatically granted.
     *
     * @param permissions A [Map] of permissions that were requested
     *
     * @return Boolean value
     */
    private fun isNotificationPermissionGranted(permissions: Map<String, Boolean>) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[POST_NOTIFICATIONS] == true
        } else {
            true
        }

    /**
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatus]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        when (checkEnableCameraUploadsStatus.invoke()) {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> {
                _state.update { it.copy(shouldTriggerCameraUploads = true) }
            }
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                _state.update { it.copy(shouldShowBusinessAccountPrompt = true) }
            }
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT -> {
                _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = true) }
            }
        }
    }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountPrompt] to False
     */
    fun resetBusinessAccountPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountPrompt = false) }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountSuspendedPrompt] to False
     */
    fun resetBusinessAccountSuspendedPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = false) }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldTriggerCameraUploads]
     * @param updatedValue the updated Boolean value of the parameter
     */
    fun setTriggerCameraUploadsState(updatedValue: Boolean) =
        _state.update { it.copy(shouldTriggerCameraUploads = updatedValue) }

    /**
     * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    fun restorePrimaryTimestampsAndSyncRecordProcess() {
        viewModelScope.launch {
            restorePrimaryTimestamps()
        }
    }

    /**
     * If the handle matches the previous secondary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    fun restoreSecondaryTimestampsAndSyncRecordProcess() {
        viewModelScope.launch {
            restoreSecondaryTimestamps()
        }
    }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldShowMediaPermissionsRationale]
     * @param shouldShow The new state value
     */
    fun setMediaPermissionsRationaleState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowMediaPermissionsRationale = shouldShow) }
    }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldShowNotificationPermissionRationale]
     * @param shouldShow The new state value
     */
    fun setNotificationPermissionRationaleState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowNotificationPermissionRationale = shouldShow) }
    }

    /**
     * Resets all Timestamps and cleans the Cache Directory
     */
    fun resetTimestampsAndCacheDirectory() = viewModelScope.launch {
        resetCameraUploadTimeStamps(clearCamSyncRecords = true)
        clearCacheDirectory()
    }

    /**
     * Call [disableCameraUploadsInDatabase] to disable Camera Uploads by manipulating
     * values in the database
     */
    fun disableCameraUploadsInDB() = viewModelScope.launch {
        disableCameraUploadsInDatabase()
    }

    /**
     * Call several Use Cases to disable Media Uploads
     */
    fun disableMediaUploads() = viewModelScope.launch {
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
    }
}
