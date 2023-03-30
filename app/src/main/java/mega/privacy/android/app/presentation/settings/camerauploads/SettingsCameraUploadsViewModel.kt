package mega.privacy.android.app.presentation.settings.camerauploads

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.SetupDefaultSecondaryFolder
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatus
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimit
import javax.inject.Inject

/**
 * [ViewModel] class for SettingsCameraUploadsFragment
 *
 * @property areLocationTagsEnabledUseCase When uploading Photos, this checks whether Location Tags should be embedded in each Photo or not
 * @property checkEnableCameraUploadsStatus Check the Camera Uploads status before enabling
 * @property clearCacheDirectory Clear all the contents of the internal cache directory
 * @property disableCameraUploadsInDatabase Disable Camera Uploads by manipulating values in the database
 * @property disableMediaUploadSettings Disable Media Uploads by manipulating a certain value in the database
 * @property getUploadOptionUseCase Retrieves the upload option of Camera Uploads
 * @property getUploadVideoQuality Retrieves the Video Quality of Videos to be uploaded
 * @property getVideoCompressionSizeLimit Retrieve the maximum video file size that can be compressed
 * @property isCameraUploadsByWifiUseCase Checks whether Camera Uploads can only be run on Wi-Fi / Wi-Fi or Mobile Data
 * @property isChargingRequiredForVideoCompression Checks whether compressing videos require the device to be charged or not
 * @property monitorConnectivityUseCase Monitor the device online status
 * @property resetCameraUploadTimeStamps Reset the Primary and Secondary Timestamps
 * @property resetMediaUploadTimeStamps Reset the Secondary Timestamps
 * @property restorePrimaryTimestamps Restore the Primary Timestamps
 * @property restoreSecondaryTimestamps Restore the Secondary Timestamps
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setChargingRequiredForVideoCompression Sets whether compressing videos require the device to be charged or not
 * @property setLocationTagsEnabledUseCase Sets whether Location Tags should be embedded in each Photo to be uploaded or not
 * @property setUploadOptionUseCase Sets the new upload option of Camera Uploads
 * @property setUploadVideoQuality Sets the new Video Quality of Videos to be uploaded
 * @property setUploadVideoSyncStatus Sets the new Sync Status of Videos to be uploaded
 * @property setVideoCompressionSizeLimit Sets the maximum video file size that can be compressed
 * @property setupDefaultSecondaryFolder Sets up a default Secondary Folder of Camera Uploads
 * @property setupPrimaryFolder Sets up the Primary Folder of Camera Uploads
 * @property setupSecondaryFolder Sets up the Secondary Folder of Camera Uploads
 */
@HiltViewModel
class SettingsCameraUploadsViewModel @Inject constructor(
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val checkEnableCameraUploadsStatus: CheckEnableCameraUploadsStatus,
    private val clearCacheDirectory: ClearCacheDirectory,
    private val disableCameraUploadsInDatabase: DisableCameraUploadsInDatabase,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQuality: GetUploadVideoQuality,
    private val getVideoCompressionSizeLimit: GetVideoCompressionSizeLimit,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isChargingRequiredForVideoCompression: IsChargingRequiredForVideoCompression,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val restorePrimaryTimestamps: RestorePrimaryTimestamps,
    private val restoreSecondaryTimestamps: RestoreSecondaryTimestamps,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompression: SetChargingRequiredForVideoCompression,
    private val setLocationTagsEnabledUseCase: SetLocationTagsEnabledUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQuality: SetUploadVideoQuality,
    private val setUploadVideoSyncStatus: SetUploadVideoSyncStatus,
    private val setVideoCompressionSizeLimit: SetVideoCompressionSizeLimit,
    private val setupDefaultSecondaryFolder: SetupDefaultSecondaryFolder,
    private val setupPrimaryFolder: SetupPrimaryFolder,
    private val setupSecondaryFolder: SetupSecondaryFolder,
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
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    init {
        initializeSettings()
    }

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
     * Sets up a Secondary Folder with a Media Uploads folder name
     */
    fun setupDefaultSecondaryCameraUploadFolder(secondaryFolderName: String) {
        viewModelScope.launch {
            setupDefaultSecondaryFolder(secondaryFolderName)
        }
    }

    /**
     * Sets up the Primary Folder with a given folder handle
     */
    fun setupPrimaryCameraUploadFolder(primaryHandle: Long) {
        viewModelScope.launch {
            setupPrimaryFolder(primaryHandle)
        }
    }

    /**
     * Sets up the Secondary Folder with a given folder handle
     */
    fun setupSecondaryCameraUploadFolder(secondaryHandle: Long) {
        viewModelScope.launch {
            setupSecondaryFolder(secondaryHandle)
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
     * Shows / hides the Access Media Location Permission rationale by updating the
     * value of [SettingsCameraUploadsState.accessMediaLocationRationaleText]
     *
     * @param showRationale true if the rationale should be shown, and false if otherwise
     */
    fun setAccessMediaLocationRationaleShown(showRationale: Boolean) =
        _state.update {
            it.copy(accessMediaLocationRationaleText = if (showRationale) R.string.on_refuse_storage_permission else null)
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

    /**
     * Sets whether Camera Uploads is running or not
     *
     * @param isRunning True if Camera Uploads is running, and false if otherwise
     */
    fun setCameraUploadsRunning(isRunning: Boolean) =
        _state.update { it.copy(isCameraUploadsRunning = isRunning) }

    /**
     * Change the Upload Connection Type for Camera Uploads
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    fun changeUploadConnectionType(wifiOnly: Boolean) = viewModelScope.launch {
        setCameraUploadsByWifiUseCase(wifiOnly)
        refreshUploadConnectionType()
    }

    /**
     * Change the Upload Option of Camera Uploads
     *
     * @param uploadOption The new [UploadOption]
     */
    fun changeUploadOption(uploadOption: UploadOption) = viewModelScope.launch {
        setUploadOptionUseCase(uploadOption)
        refreshUploadOption()
    }

    /**
     * Sets whether to include Location Tags in Photos or not
     *
     * @param include If true, Location Tags will be included for every Photo upload
     * If false, no Location Tags will be included when uploading Photos
     */
    fun includeLocationTags(include: Boolean) = viewModelScope.launch {
        setLocationTagsEnabledUseCase(include)
        refreshLocationTags()
    }

    /**
     * Change the Video Quality for videos to be uploaded. The Video Sync Status will also
     * be updated depending on the new Video Quality selected
     *
     * @param value The new Video Quality, represented as an Integer from the list
     */
    fun changeUploadVideoQuality(value: Int) = viewModelScope.launch {
        VideoQuality.values().find { it.value == value }?.let { videoQuality ->
            setUploadVideoQuality(videoQuality)
            setUploadVideoSyncStatus(
                if (videoQuality == VideoQuality.ORIGINAL) {
                    SyncStatus.STATUS_PENDING
                } else {
                    SyncStatus.STATUS_TO_COMPRESS
                }
            )
            refreshUploadVideoQuality()
        }
    }

    /**
     * Sets whether charging is required for video compression or not
     *
     * @param chargingRequired True if charging is required for video compression, and false
     * if otherwise
     */
    fun changeChargingRequiredForVideoCompression(chargingRequired: Boolean) =
        viewModelScope.launch {
            setChargingRequiredForVideoCompression(chargingRequired)
            refreshChargingRequiredForVideoCompression()
        }

    /**
     * Sets the new video compression size limit
     *
     * @param size The new video compression size limit
     */
    fun changeVideoCompressionSizeLimit(size: Int) = viewModelScope.launch {
        setVideoCompressionSizeLimit(size)
        refreshVideoCompressionSizeLimit()
    }

    /**
     * When [SettingsCameraUploadsViewModel] is instantiated, initialize the UI Elements
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            val areLocationTagsIncluded = async { areLocationTagsEnabledUseCase() }
            val isChargingRequiredForVideoCompression =
                async { isChargingRequiredForVideoCompression() }
            val uploadConnectionType = async { getUploadConnectionType() }
            val getUploadOption = async { getUploadOptionUseCase() }
            val videoCompressionSizeLimit = async { getVideoCompressionSizeLimit() }
            val videoQuality = async { getUploadVideoQuality() }
            _state.update {
                it.copy(
                    areLocationTagsIncluded = areLocationTagsIncluded.await(),
                    isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression.await(),
                    uploadConnectionType = uploadConnectionType.await(),
                    uploadOption = getUploadOption.await(),
                    videoCompressionSizeLimit = videoCompressionSizeLimit.await(),
                    videoQuality = videoQuality.await(),
                )
            }
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.uploadConnectionType] whenever a new Upload
     * Connection type is set
     */
    private suspend fun refreshUploadConnectionType() {
        val uploadConnectionType = getUploadConnectionType()
        _state.update { it.copy(uploadConnectionType = uploadConnectionType) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.uploadOption] whenever a new
     * Upload Connection type is set
     */
    private suspend fun refreshUploadOption() {
        val uploadOption = getUploadOptionUseCase()
        _state.update { it.copy(uploadOption = uploadOption) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.areLocationTagsIncluded] whenever changes
     * to include / exclude Location Tags for Photo uploads are found
     */
    private suspend fun refreshLocationTags() {
        val areLocationTagsIncluded = areLocationTagsEnabledUseCase()
        _state.update { it.copy(areLocationTagsIncluded = areLocationTagsIncluded) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.videoQuality] whenever a new upload
     * Video Quality has been set
     */
    private suspend fun refreshUploadVideoQuality() {
        val videoQuality = getUploadVideoQuality()
        _state.update { it.copy(videoQuality = videoQuality) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.isChargingRequiredForVideoCompression] whenever
     * a change to require charging for video compression is found
     */
    private suspend fun refreshChargingRequiredForVideoCompression() {
        val isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression()
        _state.update {
            it.copy(isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression)
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.videoCompressionSizeLimit] whenever the
     * maximum video compression size limit changes
     */
    private suspend fun refreshVideoCompressionSizeLimit() {
        val videoCompressionSizeLimit = getVideoCompressionSizeLimit()
        _state.update { it.copy(videoCompressionSizeLimit = videoCompressionSizeLimit) }
    }

    /**
     * Retrieves the current Upload Connection Type
     *
     * @return [UploadConnectionType.WIFI] if Camera Uploads will only upload content over Wi-Fi
     * [UploadConnectionType.WIFI_OR_MOBILE_DATA] if Camera Uploads can upload content either on
     * Wi-Fi or Mobile Data
     */
    private suspend fun getUploadConnectionType() =
        if (isCameraUploadsByWifiUseCase()) UploadConnectionType.WIFI else UploadConnectionType.WIFI_OR_MOBILE_DATA
}
