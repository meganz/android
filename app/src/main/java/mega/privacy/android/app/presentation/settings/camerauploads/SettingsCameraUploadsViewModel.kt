package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.triggered
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.UploadOptionUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.VideoQualityUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] for Settings Camera Uploads
 *
 * @property areLocationTagsEnabledUseCase Checks if Location Tags are included for Photo uploads
 * @property areUploadFileNamesKeptUseCase Checks if the existing filenames should be used when
 * uploading content
 * @property checkEnableCameraUploadsStatusUseCase Checks the Camera Uploads status and determine if it
 * can be ran
 * @property deleteCameraUploadsTemporaryRootDirectoryUseCase Deletes the temporary Camera Uploads Cache Folder
 * @property disableMediaUploadsSettingsUseCase Disables Media Uploads
 * @property getSecondaryFolderPathUseCase Gets the Media Uploads Secondary Folder Path
 * @property getUploadOptionUseCase Gets the type of content being uploaded by Camera Uploads
 * @property getUploadVideoQualityUseCase Gets the Video Quality of Videos being uploaded by Camera Uploads
 * @property getVideoCompressionSizeLimitUseCase Gets the maximum aggregated Video Size that can be
 * compressed without having to charge the Device
 * @property isCameraUploadsByWifiUseCase Checks whether Camera Uploads can only be run on Wi-Fi / Wi-Fi or Mobile Data
 * @property isCameraUploadsEnabledUseCase Checks if Camera Uploads (the Primary Folder) is enabled
 * or not
 * @property isChargingRequiredForVideoCompressionUseCase Checks whether or not the Device should be
 * charged when compressing Videos
 * @property isConnectedToInternetUseCase Checks if the User is connected to the Internet or not
 * @property isSecondaryFolderEnabled Checks if Media Uploads (the Secondary Folder) is enabled or not
 * @property isSecondaryFolderPathValidUseCase Checks if the Media Uploads Secondary Folder Path is valid or not
 * @property listenToNewMediaUseCase Listens to new Photos and Videos captured by the Device
 * @property preparePrimaryFolderPathUseCase Prepares the Primary Folder path
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setChargingRequiredForVideoCompressionUseCase Sets whether or not the Device should be
 * charged when compressing Videos
 * @property setLocationTagsEnabledUseCase Sets whether or not Location Tags are added in Photo uploads
 * @property setSecondaryFolderLocalPathUseCase Sets the new Media Uploads Secondary Folder Path
 * @property setUploadFileNamesKeptUseCase Sets whether or not existing filenames should be used
 * when uploading content
 * @property setUploadOptionUseCase Sets the new type of content being uploaded by Camera Uploads
 * @property setUploadVideoQualityUseCase Sets the new Video Quality of Videos being uploaded by Camera Uploads
 * @property setVideoCompressionSizeLimitUseCase Sets the new maximum aggregate Video Size that can
 * be compressed without having to charge the Device
 * @property setupCameraUploadsSettingUseCase If true, this enables Camera Uploads. Otherwise, the
 * feature is disabled
 * @property setupDefaultSecondaryFolderUseCase Establishes a default Media Uploads Secondary Folder
 * @property setupMediaUploadsSettingUseCase Sets up Media Uploads and its Backup Folder
 * @property snackBarHandler Handler to display a Snackbar
 * @property startCameraUploadUseCase Starts the Camera Uploads operation
 * @property stopCameraUploadsUseCase Stops the Camera Uploads operation
 * @property uploadOptionUiItemMapper UI Mapper that maps the Upload Option into [UploadOptionUiItem]
 * @property videoQualityUiItemMapper UI Mapper that maps the Video Quality into [VideoQualityUiItem]
 */
@HiltViewModel
internal class SettingsCameraUploadsViewModel @Inject constructor(
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val disableMediaUploadsSettingsUseCase: DisableMediaUploadsSettingsUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isSecondaryFolderPathValidUseCase: IsSecondaryFolderPathValidUseCase,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val preparePrimaryFolderPathUseCase: PreparePrimaryFolderPathUseCase,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setLocationTagsEnabledUseCase: SetLocationTagsEnabledUseCase,
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase,
    private val setUploadFileNamesKeptUseCase: SetUploadFileNamesKeptUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
    private val setupDefaultSecondaryFolderUseCase: SetupDefaultSecondaryFolderUseCase,
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase,
    private val snackBarHandler: SnackBarHandler,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val uploadOptionUiItemMapper: UploadOptionUiItemMapper,
    private val videoQualityUiItemMapper: VideoQualityUiItemMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsCameraUploadsUiState())

    /**
     * The Settings Camera Uploads UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        initializeSettings()
    }

    /**
     * Configures all settings upon opening Settings Camera Uploads
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            runCatching {
                preparePrimaryFolderPathUseCase()

                val isCameraUploadsEnabled = async { isCameraUploadsEnabledUseCase() }
                val isMediaUploadsEnabled = async { isSecondaryFolderEnabled() }
                val maximumNonChargingVideoCompressionSize =
                    async { getVideoCompressionSizeLimitUseCase() }
                val requireChargingDuringVideoCompression =
                    async { isChargingRequiredForVideoCompressionUseCase() }
                val secondaryFolderPath = async { getSecondaryFolderPathUseCase() }
                val shouldIncludeLocationTags = async { areLocationTagsEnabledUseCase() }
                val shouldKeepUploadFileNames = async { areUploadFileNamesKeptUseCase() }
                val uploadOption = async { getUploadOptionUseCase() }
                val uploadConnectionType = async { getUploadConnectionType() }
                val videoQuality = async { getUploadVideoQualityUseCase() }

                _uiState.update {
                    it.copy(
                        isCameraUploadsEnabled = isCameraUploadsEnabled.await(),
                        isMediaUploadsEnabled = isMediaUploadsEnabled.await(),
                        maximumNonChargingVideoCompressionSize = maximumNonChargingVideoCompressionSize.await(),
                        requireChargingDuringVideoCompression = requireChargingDuringVideoCompression.await(),
                        secondaryFolderPath = secondaryFolderPath.await(),
                        shouldIncludeLocationTags = shouldIncludeLocationTags.await(),
                        shouldKeepUploadFileNames = shouldKeepUploadFileNames.await(),
                        uploadOptionUiItem = uploadOptionUiItemMapper(uploadOption.await()),
                        uploadConnectionType = uploadConnectionType.await(),
                        videoQualityUiItem = videoQualityUiItemMapper(videoQuality.await()),
                    )
                }
            }.onFailure { exception ->
                Timber.e("An error occurred when initializing Settings Camera Uploads", exception)
            }
        }
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

    /**
     * Performs specific actions when the Camera Uploads state changes
     *
     * @param newState The new Camera Uploads state
     */
    fun onCameraUploadsStateChanged(newState: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (isConnectedToInternetUseCase()) {
                    Timber.d("Is Camera Uploads enabled: $newState")
                    if (isCameraUploadsEnabledUseCase()) {
                        // Camera Uploads is currently enabled. Disable the feature
                        setCameraUploadsEnabled(false)
                        stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
                    } else {
                        // Camera Uploads is currently disabled. Check if the Media Permissions have
                        // been granted before continuing the process
                        _uiState.update { it.copy(requestPermissions = triggered) }
                    }
                } else {
                    Timber.d("User must be connected to the Internet to update the Camera Uploads state")
                    showGenericErrorSnackbar()
                }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Camera Uploads state", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Sets whether Camera Uploads is enabled or not
     *
     * @param isEnabled true if Camera Uploads is enabled
     */
    private fun setCameraUploadsEnabled(isEnabled: Boolean) {
        _uiState.update {
            it.copy(
                isCameraUploadsEnabled = isEnabled,
                isMediaUploadsEnabled = if (isEnabled) it.isMediaUploadsEnabled else false,
            )
        }
    }

    /**
     * When the Business Account Sub-User acknowledges the prompt informing that the Business Account
     * Administrator can access the content in Camera Uploads, dismiss the prompt and enable Camera
     * Uploads
     */
    fun onRegularBusinessAccountSubUserPromptAcknowledged() {
        _uiState.update { it.copy(businessAccountPromptType = null) }
        enableCameraUploads()
    }

    /**
     * Reset the value of [SettingsCameraUploadsUiState.businessAccountPromptType]
     */
    fun onBusinessAccountPromptDismissed() {
        _uiState.update { it.copy(businessAccountPromptType = null) }
    }

    /**
     * When the User has granted all Media Permissions, perform specific actions based on the
     * Camera Uploads status
     */
    fun onMediaPermissionsGranted() {
        viewModelScope.launch {
            runCatching {
                checkEnableCameraUploadsStatusUseCase()
            }.onSuccess { cameraUploadsStatus ->
                if (cameraUploadsStatus == EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS) {
                    enableCameraUploads()
                } else {
                    _uiState.update { it.copy(businessAccountPromptType = cameraUploadsStatus) }
                }
            }.onFailure { exception ->
                Timber.e("An error occurred when checking the Camera Uploads status", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Enables Camera Uploads after granting the Media Permissions and the User can access the
     * feature normally
     */
    private fun enableCameraUploads() {
        viewModelScope.launch {
            runCatching {
                setupCameraUploadsSettingUseCase(isEnabled = true)
                setCameraUploadsEnabled(true)
            }.onFailure { exception ->
                Timber.e("An error occurred when enabling Camera Uploads", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * When the User triggers onPause() in the Settings screen, start Camera Uploads if the
     * functionality is enabled
     */
    fun onSettingsScreenPaused() {
        if (_uiState.value.isCameraUploadsEnabled) {
            viewModelScope.launch {
                runCatching {
                    startCameraUploadUseCase()
                    listenToNewMediaUseCase(forceEnqueue = false)
                }.onFailure { exception ->
                    Timber.e("An error occurred when starting Camera Uploads", exception)
                }
            }
        } else {
            Timber.d("Camera Uploads is not started because it is disabled")
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsUiState.requestPermissions]
     *
     * @param newState The new State Event. If triggered, this will perform a Camera Uploads
     * permissions request
     */
    fun onRequestPermissionsStateChanged(newState: StateEvent) {
        _uiState.update { it.copy(requestPermissions = newState) }
    }

    /**
     * Configures the new [UploadConnectionType] when uploading Camera Uploads content. Doing this
     * stops the ongoing Camera Uploads process
     *
     * @param uploadConnectionType The new [UploadConnectionType]
     */
    fun onHowToUploadPromptOptionSelected(uploadConnectionType: UploadConnectionType) {
        viewModelScope.launch {
            runCatching {
                setCameraUploadsByWifiUseCase(uploadConnectionType == UploadConnectionType.WIFI)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update {
                    it.copy(uploadConnectionType = uploadConnectionType)
                }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Upload Connection Type", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Configures the new type of content being uploaded by Camera Uploads. Doing this stops the
     * ongoing Camera Uploads process and clears the internal Cache
     *
     * @param uploadOptionUiItem The new [UploadOptionUiItem]
     */
    fun onUploadOptionUiItemSelected(uploadOptionUiItem: UploadOptionUiItem) {
        viewModelScope.launch {
            runCatching {
                setUploadOptionUseCase(uploadOptionUiItem.uploadOption)
                deleteCameraUploadsTemporaryRootDirectoryUseCase()
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(uploadOptionUiItem = uploadOptionUiItem) }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Upload Option", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Configures the new Video Quality for Videos being uploaded by Camera Uploads. Doing this stops
     * the ongoing Camera Uploads process
     *
     * @param videoQualityUiItem The new [VideoQualityUiItem]
     */
    fun onVideoQualityUiItemSelected(videoQualityUiItem: VideoQualityUiItem) {
        viewModelScope.launch {
            runCatching {
                setUploadVideoQualityUseCase(videoQualityUiItem.videoQuality)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(videoQualityUiItem = videoQualityUiItem) }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Video Quality", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Configures whether or not the existing filenames should be used when uploading content. Doing
     * this stops the ongoing Camera Uploads process
     *
     * @param newState The new Keep File Names state
     */
    fun onKeepFileNamesStateChanged(newState: Boolean) {
        viewModelScope.launch {
            runCatching {
                setUploadFileNamesKeptUseCase(newState)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(shouldKeepUploadFileNames = newState) }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Keep File Names state", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Configures whether Location Tags should be added / removed when uploading Photos. Doing this
     * stops the ongoing Camera Uploads process
     *
     * @param newState The new Include Location Tags state
     */
    fun onIncludeLocationTagsStateChanged(newState: Boolean) {
        viewModelScope.launch {
            runCatching {
                setLocationTagsEnabledUseCase(newState)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(shouldIncludeLocationTags = newState) }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Include Location Tags state",
                    exception
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Configures whether or not the Device should be charged when compressing Videos. Doing this
     * stops the ongoing Camera Uploads process
     *
     * @param newState The new Device charging state when compressing Videos
     */
    fun onChargingDuringVideoCompressionStateChanged(newState: Boolean) {
        viewModelScope.launch {
            runCatching {
                setChargingRequiredForVideoCompressionUseCase(newState)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(requireChargingDuringVideoCompression = newState) }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Video Compression Charging State",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Sets the new maximum aggregate Video Size that can be compressed without having to charge the
     * Device. Doing this stops the ongoing Camera Uploads process
     *
     * @param newVideoCompressionSize The new maximum Video Size
     */
    fun onNewVideoCompressionSizeLimitProvided(newVideoCompressionSize: Int) {
        viewModelScope.launch {
            runCatching {
                setVideoCompressionSizeLimitUseCase(newVideoCompressionSize)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(maximumNonChargingVideoCompressionSize = newVideoCompressionSize) }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the new maximum Video Compression Size Limit",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Performs specific actions when the Media Uploads state changes
     *
     * @param enabled true if Media Uploads should be enabled
     */
    fun onMediaUploadsStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (isConnectedToInternetUseCase()) {
                    if (enabled) {
                        // Enable Media Uploads
                        val isCurrentSecondaryFolderPathValid =
                            isSecondaryFolderPathValidUseCase(_uiState.value.secondaryFolderPath)
                        if (!isCurrentSecondaryFolderPathValid) {
                            setSecondaryFolderLocalPathUseCase("")
                        }

                        // Sets up a Secondary Folder with a Media Uploads folder name
                        setupDefaultSecondaryFolderUseCase()
                        setupMediaUploadsSettingUseCase(isEnabled = true)
                        stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)

                        _uiState.update {
                            it.copy(
                                isMediaUploadsEnabled = true,
                                secondaryFolderPath = if (!isCurrentSecondaryFolderPathValid) "" else it.secondaryFolderPath,
                            )
                        }
                    } else {
                        // Disable Media Uploads
                        disableMediaUploadsSettingsUseCase()
                        stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                        _uiState.update { it.copy(isMediaUploadsEnabled = false) }
                    }
                } else {
                    Timber.d("User must be connected to the Internet to update the Media Uploads state")
                    showGenericErrorSnackbar()
                }
            }.onFailure { exception ->
                Timber.e("An error occurred when changing the Media Uploads state", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Uses [SnackBarHandler] to display a generic Error Message
     */
    private fun showGenericErrorSnackbar() {
        snackBarHandler.postSnackbarMessage(
            resId = R.string.general_error,
            snackbarDuration = MegaSnackbarDuration.Long,
        )
    }
}