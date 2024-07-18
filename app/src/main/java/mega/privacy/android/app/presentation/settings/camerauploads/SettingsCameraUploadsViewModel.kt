package mega.privacy.android.app.presentation.settings.camerauploads

import mega.privacy.android.shared.resources.R as SharedR
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.UploadOptionUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.VideoQualityUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.ClearCameraUploadsRecordUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsFolderPathExistingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsNewFolderNodeValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] for Settings Camera Uploads
 *
 * @property applicationScope Coroutine Scope used to run operations that should outlive the
 * ViewModel
 * @property areLocationTagsEnabledUseCase Checks if Location Tags are included for Photo uploads
 * @property areUploadFileNamesKeptUseCase Checks if the existing filenames should be used when
 * uploading content
 * @property checkEnableCameraUploadsStatusUseCase Checks the Camera Uploads status and determine if it
 * can be ran
 * @property clearCameraUploadsRecordUseCase Clears the Records of either the Camera Uploads or Media
 * Uploads Folder
 * @property deleteCameraUploadsTemporaryRootDirectoryUseCase Deletes the temporary Camera Uploads Cache Folder
 * @property disableMediaUploadsSettingsUseCase Disables Media Uploads
 * @property getPrimaryFolderNodeUseCase Gets the Camera Uploads Primary Folder Node
 * @property getPrimaryFolderPathUseCase Gets the Camera Uploads Primary Folder Path
 * @property getSecondaryFolderNodeUseCase Gets the Media Uploads Secondary Folder Node
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
 * @property isChargingRequiredToUploadContentUseCase Checks whether or not the Device must be
 * charged for the active Camera Uploads to start uploading content
 * @property isConnectedToInternetUseCase Checks if the User is connected to the Internet or not
 * @property isFolderPathExistingUseCase Checks if the specific Local Folder exists or not
 * @property isMediaUploadsEnabledUseCase Checks if Media Uploads (the Secondary Folder) is enabled or not
 * @property isNewFolderNodeValidUseCase Checks whether the new Cloud Drive Folder Node selected by
 * Camera Uploads is valid or not
 * @property isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase Checks if the specific Camera
 * Uploads Primary Folder Path is not the same Folder or a parent Folder or a sub Folder from the
 * current Local Secondary Folder
 * @property isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase Checks if the specific Camera
 * Uploads Secondary Folder Path is not the same Folder or a parent Folder or a sub Folder from the
 * current Local Primary Folder
 * @property isSecondaryFolderPathValidUseCase Checks if the Media Uploads Secondary Folder Path is valid or not
 * @property listenToNewMediaUseCase Listens to new Photos and Videos captured by the Device
 * @property monitorCameraUploadsFolderDestinationUseCase Listens for any destination changes in the Camera
 * / Media Uploads Folder Nodes
 * @property monitorCameraUploadsSettingsActionsUseCase Monitors any changes to Settings Camera
 * Uploads
 * @property monitorCameraUploadsStatusInfoUseCase Monitors the Camera Uploads status
 * @property preparePrimaryFolderPathUseCase Prepares the Primary Folder path
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setChargingRequiredForVideoCompressionUseCase Sets whether or not the Device should be
 * charged when compressing Videos
 * @property setChargingRequiredToUploadContentUseCase Sets whether or not the Device must be
 * charged for the active Camera Uploads to start uploading content
 * @property setLocationTagsEnabledUseCase Sets whether or not Location Tags are added in Photo uploads
 * @property setPrimaryFolderPathUseCase Set the new Camera Uploads Primary Folder Path
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
 * @property setupPrimaryFolderUseCase Sets the new Camera Uploads Folder Node
 * @property setupSecondaryFolderUseCase Sets the new Media Uploads Folder Node
 * @property startCameraUploadUseCase Starts the Camera Uploads operation
 * @property stopCameraUploadsUseCase Stops the Camera Uploads operation
 * @property uploadOptionUiItemMapper UI Mapper that maps the Upload Option into [UploadOptionUiItem]
 * @property videoQualityUiItemMapper UI Mapper that maps the Video Quality into [VideoQualityUiItem]
 */
@HiltViewModel
internal class SettingsCameraUploadsViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val clearCameraUploadsRecordUseCase: ClearCameraUploadsRecordUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val disableMediaUploadsSettingsUseCase: DisableMediaUploadsSettingsUseCase,
    private val getPrimaryFolderNodeUseCase: GetPrimaryFolderNodeUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderNodeUseCase: GetSecondaryFolderNodeUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
    private val isChargingRequiredToUploadContentUseCase: IsChargingRequiredToUploadContentUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val isNewFolderNodeValidUseCase: IsNewFolderNodeValidUseCase,
    private val isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase: IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase,
    private val isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase: IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase,
    private val isSecondaryFolderPathValidUseCase: IsSecondaryFolderPathValidUseCase,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val monitorCameraUploadsFolderDestinationUseCase: MonitorCameraUploadsFolderDestinationUseCase,
    private val monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase,
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val preparePrimaryFolderPathUseCase: PreparePrimaryFolderPathUseCase,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setChargingRequiredToUploadContentUseCase: SetChargingRequiredToUploadContentUseCase,
    private val setLocationTagsEnabledUseCase: SetLocationTagsEnabledUseCase,
    private val setPrimaryFolderPathUseCase: SetPrimaryFolderPathUseCase,
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase,
    private val setUploadFileNamesKeptUseCase: SetUploadFileNamesKeptUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
    private val setupDefaultSecondaryFolderUseCase: SetupDefaultSecondaryFolderUseCase,
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase,
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
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
        monitorCameraUploadsFolderDestination()
        monitorCameraUploadsSettingsActions()
        monitorCameraUploadsStatusInfo()
    }

    /**
     * Configures all settings upon opening Settings Camera Uploads
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            runCatching {
                preparePrimaryFolderPathUseCase()

                val isCameraUploadsEnabled = async { isCameraUploadsEnabledUseCase() }
                val isMediaUploadsEnabled = async { isMediaUploadsEnabledUseCase() }
                val maximumNonChargingVideoCompressionSize =
                    async { getVideoCompressionSizeLimitUseCase() }
                val primaryFolderNode = async { getPrimaryFolderNodeUseCase() }
                val primaryFolderPath = async { getPrimaryFolderPathUseCase() }
                val requireChargingDuringVideoCompression =
                    async { isChargingRequiredForVideoCompressionUseCase() }
                val requireChargingWhenUploadingContent =
                    async { isChargingRequiredToUploadContentUseCase() }
                val secondaryFolderNode = async { getSecondaryFolderNodeUseCase() }
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
                        primaryFolderName = primaryFolderNode.await()?.name,
                        primaryFolderPath = primaryFolderPath.await(),
                        requireChargingDuringVideoCompression = requireChargingDuringVideoCompression.await(),
                        requireChargingWhenUploadingContent = requireChargingWhenUploadingContent.await(),
                        secondaryFolderName = secondaryFolderNode.await()?.name,
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
     * Observes any destination changes of the Camera / Media Uploads Folder nodes
     */
    private fun monitorCameraUploadsFolderDestination() {
        viewModelScope.launch {
            monitorCameraUploadsFolderDestinationUseCase()
                .catch { exception ->
                    Timber.e(
                        "An exception occurred when listening for the new Camera / Media Uploads Folder Node destinations",
                        exception,
                    )
                }.collect { cameraUploadsFolderDestinationUpdate ->
                    when (cameraUploadsFolderDestinationUpdate.cameraUploadFolderType) {
                        CameraUploadFolderType.Primary -> setPrimaryFolderName(
                            primaryFolderNodeId = NodeId(cameraUploadsFolderDestinationUpdate.nodeHandle),
                        )

                        CameraUploadFolderType.Secondary -> setSecondaryFolderName(
                            secondaryFolderNodeId = NodeId(cameraUploadsFolderDestinationUpdate.nodeHandle),
                        )
                    }
                }
        }
    }

    /**
     * Observes any behavioral changes to the Settings Camera Uploads
     */
    private fun monitorCameraUploadsSettingsActions() {
        viewModelScope.launch {
            monitorCameraUploadsSettingsActionsUseCase()
                .catch { exception ->
                    Timber.e(
                        "An exception occurred when listening for Settings Camera Uploads changes",
                        exception,
                    )
                }.collect { cameraUploadsSettingsAction ->
                    when (cameraUploadsSettingsAction) {
                        CameraUploadsSettingsAction.DisableCameraUploads -> {
                            onCameraUploadsStateChanged(enabled = false)
                        }

                        CameraUploadsSettingsAction.DisableMediaUploads -> {
                            onMediaUploadsStateChanged(enabled = false)
                        }
                    }
                }
        }
    }

    /**
     * Observes any Camera Uploads status changes
     */
    private fun monitorCameraUploadsStatusInfo() {
        viewModelScope.launch {
            monitorCameraUploadsStatusInfoUseCase()
                .catch { exception ->
                    Timber.e(
                        "An exception occurred when listening for Camera Uploads status changes",
                        exception,
                    )
                }.collect { cameraUploadsStatusInfo ->
                    if (cameraUploadsStatusInfo is CameraUploadsStatusInfo.Finished &&
                        cameraUploadsStatusInfo.reason == CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET
                    ) {
                        showSnackbar(SharedR.string.camera_uploads_phone_not_charging_message)
                    }
                }
        }
    }

    /**
     * When receiving a destination update of the Camera Uploads Primary Folder Node, update the
     * Primary Folder Node name in the UI State
     *
     * @param primaryFolderNodeId The Camera Uploads Primary Folder [NodeId]
     */
    private fun setPrimaryFolderName(primaryFolderNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                getPrimaryFolderNodeUseCase(primaryFolderNodeId)
            }.onSuccess { primaryFolderNode ->
                _uiState.update { it.copy(primaryFolderName = primaryFolderNode?.name) }
            }.onFailure { exception ->
                Timber.e(
                    "An exception occurred when updating the Camera Uploads Folder Node Name",
                    exception,
                )
            }
        }
    }

    /**
     * When receiving a destination update of the Media Uploads Secondary Folder Node, update the
     * Secondary Folder Node name in the UI State
     *
     * @param secondaryFolderNodeId The Media Uploads Secondary Folder [NodeId]
     */
    private fun setSecondaryFolderName(secondaryFolderNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                getSecondaryFolderNodeUseCase(secondaryFolderNodeId)
            }.onSuccess { secondaryFolderNode ->
                _uiState.update { it.copy(secondaryFolderName = secondaryFolderNode?.name) }
            }.onFailure { exception ->
                Timber.e(
                    "An exception occurred when updating the Media Uploads Folder Node Name",
                    exception,
                )
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
     * @param enabled true if Camera Uploads should be enabled
     */
    fun onCameraUploadsStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (isConnectedToInternetUseCase()) {
                    if (enabled) {
                        // Check if the Media Permissions have been granted before continuing the
                        // process of enabling Camera Uploads
                        _uiState.update { it.copy(requestMediaPermissions = triggered) }
                    } else {
                        // Disable Camera Uploads
                        setCameraUploadsEnabled(false)
                        stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
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
            }.onSuccess {
                showSnackbar(R.string.settings_camera_notif_initializing_title)
            }.onFailure { exception ->
                Timber.e("An error occurred when enabling Camera Uploads", exception)
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Starts the Camera Uploads process. The [ApplicationScope] is used to ensure that the process
     * is started when leaving the Settings Camera Uploads screen
     */
    fun onCameraUploadsProcessStarted() {
        applicationScope.launch {
            runCatching {
                startCameraUploadUseCase()
                listenToNewMediaUseCase(forceEnqueue = false)
            }.onFailure { exception ->
                Timber.e("An error occurred when starting Camera Uploads", exception)
            }
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsUiState.requestLocationPermission]
     *
     * @param newState The new State Event. If triggered, this will perform a request to grant the
     * Location Permission
     */
    fun onRequestLocationPermissionStateChanged(newState: StateEvent) {
        _uiState.update { it.copy(requestLocationPermission = newState) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsUiState.requestMediaPermissions]
     *
     * @param newState The new State Event. If triggered, this will perform a request to grant
     * Media Permissions
     */
    fun onRequestMediaPermissionsStateChanged(newState: StateEvent) {
        _uiState.update { it.copy(requestMediaPermissions = newState) }
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
     * Configures whether or not the Device should be charged in order for the active Camera Uploads
     * to begin uploading content
     *
     * @param newState The new Device charging state when uploading content
     */
    fun onChargingWhenUploadingContentStateChanged(newState: Boolean) {
        viewModelScope.launch {
            runCatching {
                setChargingRequiredToUploadContentUseCase(newState)
                _uiState.update { it.copy(requireChargingWhenUploadingContent = newState) }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Device charging state to upload content",
                    exception,
                )
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
     * When the User has granted the Location Permission, enable the "Include location tags" Option
     */
    fun onLocationPermissionGranted() {
        includeLocationTags(true)
    }

    /**
     * Configures whether Location Tags should be added / removed when uploading Photos. Doing this
     * stops the ongoing Camera Uploads process
     *
     * @param isEnabled true if the feature should be enabled
     */
    private fun includeLocationTags(isEnabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                setLocationTagsEnabledUseCase(isEnabled)
                stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                _uiState.update { it.copy(shouldIncludeLocationTags = isEnabled) }
            }.onFailure {
                Timber.e("An exception occurred when changing the Include Location Tags state to $isEnabled")
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Performs certain actions when the "Include location tags" State changes
     *
     * @param newState The new Include Location Tags state
     */
    fun onIncludeLocationTagsStateChanged(newState: Boolean) {
        if (newState) {
            // Check if the Location Permission has been granted before continuing the process of
            // enabling the Option
            _uiState.update { it.copy(requestLocationPermission = triggered) }
        } else {
            includeLocationTags(false)
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
     * Sets the new Local Camera Uploads Folder after selecting from the Device File Explorer. Doing
     * this removes the Camera Uploads temporary root directory, clears the Primary Folder Records
     * and stops Camera Uploads
     *
     * @param newPrimaryFolderPath The new Primary Folder path, which may be nullable
     */
    fun onLocalPrimaryFolderSelected(newPrimaryFolderPath: String?) {
        viewModelScope.launch {
            runCatching {
                newPrimaryFolderPath?.let { primaryFolderPath ->
                    if (isFolderPathExistingUseCase(primaryFolderPath)) {
                        if (isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(primaryFolderPath)) {
                            setPrimaryFolderPathUseCase(primaryFolderPath)
                            deleteCameraUploadsTemporaryRootDirectoryUseCase()
                            clearCameraUploadsRecordUseCase(listOf(CameraUploadFolderType.Primary))
                            stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)

                            _uiState.update { it.copy(primaryFolderPath = primaryFolderPath) }
                        } else {
                            Timber.d("The new Camera Uploads Local Folder is related to the Media Uploads Folder")
                            _uiState.update { it.copy(showRelatedNewLocalFolderWarning = true) }
                        }
                    } else {
                        Timber.d("The new Camera Uploads Local Folder does not exist")
                        showInvalidFolderSnackbar()
                    }
                } ?: run {
                    Timber.d("The new Camera Uploads Local Folder is null")
                    showInvalidFolderSnackbar()
                }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Camera Uploads Local Folder",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Sets the new Camera Uploads Folder Node after selecting from Cloud Drive. Doing this stops the
     * ongoing Camera Uploads process
     *
     * @param newPrimaryFolderNodeId The new Primary Folder [NodeId]
     */
    fun onPrimaryFolderNodeSelected(newPrimaryFolderNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                if (isNewFolderNodeValidUseCase(newPrimaryFolderNodeId.longValue)) {
                    setupPrimaryFolderUseCase(newPrimaryFolderNodeId.longValue)
                    stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                } else {
                    Timber.d("The new Camera Uploads Folder Node is invalid")
                    showInvalidFolderSnackbar()
                }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Camera Uploads Folder Node",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Sets the new Local Media Uploads Folder after selecting from the Device File Explorer. Doing
     * this clears the Secondary Folder Records and stops the ongoing Camera Uploads process
     *
     * @param newSecondaryFolderPath The new Secondary Folder path, which may be nullable
     */
    fun onLocalSecondaryFolderSelected(newSecondaryFolderPath: String?) {
        viewModelScope.launch {
            runCatching {
                newSecondaryFolderPath?.let { secondaryFolderPath ->
                    if (isFolderPathExistingUseCase(secondaryFolderPath)) {
                        if (isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(secondaryFolderPath)) {
                            setSecondaryFolderLocalPathUseCase(secondaryFolderPath)
                            clearCameraUploadsRecordUseCase(listOf(CameraUploadFolderType.Secondary))
                            stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)

                            _uiState.update { it.copy(secondaryFolderPath = secondaryFolderPath) }
                        } else {
                            Timber.d("The new Media Uploads Local Folder is related to the Camera Uploads Folder")
                            _uiState.update { it.copy(showRelatedNewLocalFolderWarning = true) }
                        }
                    } else {
                        Timber.d("The new Media Uploads Local Folder does not exist")
                        showInvalidFolderSnackbar()
                    }
                } ?: run {
                    Timber.d("The new Media Uploads Local Folder is null")
                    showInvalidFolderSnackbar()
                }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Media Uploads Local Folder",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Sets the new Media Uploads Folder Node after selecting from Cloud Drive. Doing this stops the
     * ongoing Camera Uploads process
     *
     * @param newSecondaryFolderNodeId The new Primary Folder [NodeId]
     */
    fun onSecondaryFolderNodeSelected(newSecondaryFolderNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                if (isNewFolderNodeValidUseCase(newSecondaryFolderNodeId.longValue)) {
                    setupSecondaryFolderUseCase(newSecondaryFolderNodeId.longValue)
                    stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
                } else {
                    Timber.d("The new Media Uploads Folder Node is invalid")
                    showInvalidFolderSnackbar()
                }
            }.onFailure { exception ->
                Timber.e(
                    "An error occurred when changing the Media Uploads Folder Node",
                    exception,
                )
                showGenericErrorSnackbar()
            }
        }
    }

    /**
     * Updates the UI State to hide the related new Local Primary / Secondary Folder warning
     */
    fun onRelatedNewLocalFolderWarningDismissed() =
        _uiState.update { it.copy(showRelatedNewLocalFolderWarning = false) }

    /**
     * Notifies the UI State that the Snackbar has been displayed with the specific message
     */
    fun onSnackbarMessageConsumed() = _uiState.update { it.copy(snackbarMessage = consumed()) }

    /**
     * Updates the UI State to display a Snackbar with a generic Error Message
     */
    private fun showGenericErrorSnackbar() = showSnackbar(R.string.general_error)

    /**
     * Updates the UI State to display a Snackbar with an Invalid Folder Error Message
     */
    private fun showInvalidFolderSnackbar() = showSnackbar(R.string.error_invalid_folder_selected)

    /**
     * Updates the UI State to display a Snackbar with a specific message
     *
     * @param messageRes The String Resource to be displayed in the Snackbar
     */
    private fun showSnackbar(@StringRes messageRes: Int) {
        _uiState.update { it.copy(snackbarMessage = triggered(messageRes)) }
    }
}