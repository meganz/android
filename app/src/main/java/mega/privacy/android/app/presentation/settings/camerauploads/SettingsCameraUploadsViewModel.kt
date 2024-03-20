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
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] for Settings Camera Uploads
 *
 * @property checkEnableCameraUploadsStatusUseCase Checks the Camera Uploads status and determine if it
 * can be ran
 * @property deleteCameraUploadsTemporaryRootDirectoryUseCase Deletes the temporary Camera Uploads Cache Folder
 * @property getUploadOptionUseCase Gets the type of content being uploaded by Camera Uploads
 * @property getUploadVideoQualityUseCase Gets the Video Quality of Videos being uploaded by Camera Uploads
 * @property isCameraUploadsByWifiUseCase Checks whether Camera Uploads can only be run on Wi-Fi / Wi-Fi or Mobile Data
 * @property isCameraUploadsEnabledUseCase Checks if Camera Uploads (the Primary Folder) is enabled
 * or not
 * @property isConnectedToInternetUseCase Checks if the User is connected to the Internet or not
 * @property isSecondaryFolderEnabled Checks if Media Uploads (the Secondary Folder) is enabled or not
 * @property listenToNewMediaUseCase Listens to new Photos and Videos captured by the Device
 * @property preparePrimaryFolderPathUseCase Prepares the Primary Folder path
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setUploadOptionUseCase Sets the new type of content being uploaded by Camera Uploads
 * @property setUploadVideoQualityUseCase Sets the new Video Quality of Videos being uploaded by Camera Uploads
 * @property setupCameraUploadsSettingUseCase If true, this enables Camera Uploads. Otherwise, the
 * feature is disabled
 * @property snackBarHandler Handler to display a Snackbar
 * @property startCameraUploadUseCase Starts the Camera Uploads operation
 * @property stopCameraUploadsUseCase Stops the Camera Uploads operation
 * @property uploadOptionUiItemMapper UI Mapper that maps the Upload Option into [UploadOptionUiItem]
 * @property videoQualityUiItemMapper UI Mapper that maps the Video Quality into [VideoQualityUiItem]
 */
@HiltViewModel
internal class SettingsCameraUploadsViewModel @Inject constructor(
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val preparePrimaryFolderPathUseCase: PreparePrimaryFolderPathUseCase,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
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
                val uploadOption = async { getUploadOptionUseCase() }
                val uploadConnectionType = async { getUploadConnectionType() }
                val videoQuality = async { getUploadVideoQualityUseCase() }

                _uiState.update {
                    it.copy(
                        isCameraUploadsEnabled = isCameraUploadsEnabled.await(),
                        isMediaUploadsEnabled = isMediaUploadsEnabled.await(),
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
     * Uses [SnackBarHandler] to display a generic Error Message
     */
    private fun showGenericErrorSnackbar() {
        snackBarHandler.postSnackbarMessage(
            resId = R.string.general_error,
            snackbarDuration = MegaSnackbarDuration.Long,
        )
    }
}