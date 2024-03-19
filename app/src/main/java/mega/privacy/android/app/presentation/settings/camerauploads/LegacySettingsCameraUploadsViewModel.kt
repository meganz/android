package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.LegacySettingsCameraUploadsState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.backup.MonitorBackupInfoTypeUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.ClearCameraUploadsRecordUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderNodeValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderNodeValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class for SettingsCameraUploadsFragment
 *
 * @property isCameraUploadsEnabledUseCase Retrieves the enable status of Camera Uploads
 * @property areLocationTagsEnabledUseCase When uploading Photos, this checks whether Location Tags should be embedded in each Photo or not
 * @property areUploadFileNamesKeptUseCase Checks whether the File Names are kept or not when uploading content
 * @property checkEnableCameraUploadsStatusUseCase Checks the Camera Uploads status before enabling
 * @property deleteCameraUploadsTemporaryRootDirectoryUseCase Deletes the temporary Camera Uploads Cache Folder
 * @property disableMediaUploadsSettingsUseCase Disables Media Uploads by manipulating a certain value in the database
 * @property getPrimaryFolderPathUseCase Retrieves the Primary Folder path
 * @property getUploadOptionUseCase Retrieves the upload option of Camera Uploads
 * @property getUploadVideoQualityUseCase Retrieves the Video Quality of Videos to be uploaded
 * @property getVideoCompressionSizeLimitUseCase Retrieves the maximum video file size that can be compressed
 * @property isCameraUploadsByWifiUseCase Checks whether Camera Uploads can only be run on Wi-Fi / Wi-Fi or Mobile Data
 * @property isChargingRequiredForVideoCompressionUseCase Checks whether compressing videos require the device to be charged or not
 * @property isPrimaryFolderNodeValidUseCase Checks whether the Primary Folder node is valid or not
 * @property isPrimaryFolderPathValidUseCase Checks whether the Primary Folder path is valid or not
 * @property monitorConnectivityUseCase Monitors the device online status
 * @property preparePrimaryFolderPathUseCase Prepares the Primary Folder path
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setChargingRequiredForVideoCompressionUseCase Sets whether compressing videos require the device to be charged or not
 * @property setDefaultPrimaryFolderPathUseCase Sets the default Primary Folder path
 * @property setLocationTagsEnabledUseCase Sets whether Location Tags should be embedded in each Photo to be uploaded or not
 * @property setPrimaryFolderPathUseCase Sets the new Primary Folder path
 * @property setUploadFileNamesKeptUseCase Sets whether the File Names of files to be uploaded will be kept or not
 * @property setUploadOptionUseCase Sets the new upload option of Camera Uploads
 * @property setUploadVideoQualityUseCase Sets the new Video Quality of Videos to be uploaded
 * @property setVideoCompressionSizeLimitUseCase Sets the maximum video file size that can be compressed
 * @property setupDefaultSecondaryFolderUseCase Sets up a default Secondary Folder of Camera Uploads
 * @property setupPrimaryFolderUseCase Sets up the Primary Folder of Camera Uploads
 * @property setupSecondaryFolderUseCase Sets up the Secondary Folder of Camera Uploads
 * @property startCameraUploadUseCase Start the camera upload
 * @property stopCameraUploadsUseCase Stop the camera upload
 * @property broadcastBusinessAccountExpiredUseCase broadcast business account expired
 * @property isSecondaryFolderNodeValidUseCase Checks whether the Secondary Folder node is valid or not
 * @property snackBarHandler Handler used to display a Snackbar
 */
@HiltViewModel
@Deprecated(message = "This is a legacy class that will be replaced by a ViewModel for [SettingsCameraUploadsComposeActivity] once the migration to Jetpack Compose has been finished")
class LegacySettingsCameraUploadsViewModel @Inject constructor(
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val disableMediaUploadsSettingsUseCase: DisableMediaUploadsSettingsUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
    private val isPrimaryFolderNodeValidUseCase: IsPrimaryFolderNodeValidUseCase,
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val preparePrimaryFolderPathUseCase: PreparePrimaryFolderPathUseCase,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setDefaultPrimaryFolderPathUseCase: SetDefaultPrimaryFolderPathUseCase,
    private val setLocationTagsEnabledUseCase: SetLocationTagsEnabledUseCase,
    private val setPrimaryFolderPathUseCase: SetPrimaryFolderPathUseCase,
    private val setUploadFileNamesKeptUseCase: SetUploadFileNamesKeptUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
    private val setupDefaultSecondaryFolderUseCase: SetupDefaultSecondaryFolderUseCase,
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase,
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase,
    monitorBackupInfoTypeUseCase: MonitorBackupInfoTypeUseCase,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    monitorCameraUploadsFolderDestinationUseCase: MonitorCameraUploadsFolderDestinationUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val isSecondaryFolderEnabledUseCase: IsSecondaryFolderEnabled,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val setupMediaUploadsSyncHandleUseCase: SetupMediaUploadsSyncHandleUseCase,
    private val isSecondaryFolderNodeValidUseCase: IsSecondaryFolderNodeValidUseCase,
    private val isSecondaryFolderPathValidUseCase: IsSecondaryFolderPathValidUseCase,
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase,
    private val clearCameraUploadsRecordUseCase: ClearCameraUploadsRecordUseCase,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    private val _state = MutableStateFlow(LegacySettingsCameraUploadsState())

    /**
     * State of Settings Camera Uploads
     */
    val state: StateFlow<LegacySettingsCameraUploadsState> = _state.asStateFlow()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Monitor Camera Upload Settings Actions
     */
    val monitorCameraUploadsSettingsActions = monitorCameraUploadsSettingsActionsUseCase()

    /**
     * Monitor Backup Info Type
     */
    val monitorBackupInfoType = monitorBackupInfoTypeUseCase()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    /**
     * Monitor Camera Uploads Folder Destination
     */
    val monitorCameraUploadsFolderDestination = monitorCameraUploadsFolderDestinationUseCase()

    init {
        initializeSettings()
    }

    /**
     * Handle specific behavior when permissions are granted / denied
     */
    fun handlePermissionsResult() {
        if (hasMediaPermissionUseCase()) {
            handleEnableCameraUploads()
        } else {
            setMediaPermissionsRationaleState(shouldShow = true)
        }
    }

    /**
     *
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatusUseCase]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        Timber.d("checkEnableCameraUploadsStatusUseCase")
        runCatching { checkEnableCameraUploadsStatusUseCase() }.onSuccess { status ->
            when (status) {
                EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> {
                    onCameraUploadsEnabled()
                }

                EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                    _state.update { it.copy(shouldShowBusinessAccountPrompt = true) }
                }

                EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
                EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
                -> {
                    broadcastBusinessAccountExpiredUseCase()
                }
            }
        }.onFailure { Timber.w("Exception checking CU status: $it") }
    }

    /**
     * Resets the value of [LegacySettingsCameraUploadsState.shouldShowBusinessAccountPrompt] to False
     */
    fun resetBusinessAccountPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountPrompt = false) }

    /**
     * on Enable MediaUpload
     */
    private suspend fun enableMediaUploads() {
        runCatching {
            val isCurrentSecondaryFolderPathValid =
                isSecondaryFolderPathValidUseCase(_state.value.secondaryFolderPath)
            if (!isCurrentSecondaryFolderPathValid) {
                setSecondaryFolderLocalPathUseCase("")
            }

            // Sets up a Secondary Folder with a Media Uploads folder name
            setupDefaultSecondaryFolderUseCase()
            setupMediaUploadsSettingUseCase(isEnabled = true)
            stopCameraUploads()

            _state.update {
                it.copy(
                    isMediaUploadsEnabled = !it.isMediaUploadsEnabled,
                    secondaryFolderPath = if (!isCurrentSecondaryFolderPathValid) "" else it.secondaryFolderPath,
                )
            }
        }.onFailure {
            Timber.e(it)
            snackBarHandler.postSnackbarMessage(
                resId = R.string.general_error,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }
    }

    private suspend fun disableMediaUploads() {
        runCatching {
            resetAndDisableMediaUploads()
            stopCameraUploads()
            _state.update {
                it.copy(isMediaUploadsEnabled = !it.isMediaUploadsEnabled)
            }
        }.onFailure {
            Timber.e(it)
            snackBarHandler.postSnackbarMessage(
                resId = R.string.general_error,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }
    }

    /**
     * Sets the new Secondary Folder from Cloud Drive
     *
     * @param newHandle The handle of the new Secondary Folder from Cloud Drive
     */
    fun setSecondaryUploadNode(newHandle: Long) {
        viewModelScope.launch {
            runCatching {
                if (isSecondaryFolderNodeValidUseCase(newHandle)) {
                    setupSecondaryFolderUseCase(newHandle)
                } else {
                    Timber.e("The new Cloud Drive Folder for Secondary Uploads is invalid")
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.error_invalid_folder_selected,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
            }.onFailure {
                Timber.e("An Exception occurred when setting the new Cloud Drive Folder for Secondary Uploads:\n$it")
                snackBarHandler.postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
        }
    }

    /**
     * Sets the value of [LegacySettingsCameraUploadsState.shouldShowMediaPermissionsRationale]
     * @param shouldShow The new state value
     */
    fun setMediaPermissionsRationaleState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowMediaPermissionsRationale = shouldShow) }
    }

    /**
     * Shows the Access Media Location Permission rationale by displaying a Snackbar with a specific
     * message
     */
    fun showAccessMediaLocationRationale() {
        snackBarHandler.postSnackbarMessage(R.string.on_refuse_storage_permission)
    }

    private suspend fun resetAndDisableMediaUploads() {
        disableMediaUploadsSettingsUseCase()
    }

    /**
     * onCameraUploadsEnabled
     */
    fun onCameraUploadsEnabled() {
        viewModelScope.launch {
            runCatching {
                setupCameraUploadsSettingUseCase(isEnabled = true)
                setCameraUploadsEnabled(true)
            }.onFailure {
                Timber.e(it)
                snackBarHandler.postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
        }
    }

    /**
     * Sets whether Camera Uploads is enabled or not
     *
     * @param isEnabled True if Camera Uploads is enabled, and false if otherwise
     */
    fun setCameraUploadsEnabled(isEnabled: Boolean) =
        _state.update {
            it.copy(
                isCameraUploadsEnabled = isEnabled,
                isMediaUploadsEnabled = if (isEnabled) it.isMediaUploadsEnabled else false
            )
        }

    /**
     * Change the Upload Connection Type for Camera Uploads
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    fun changeUploadConnectionType(wifiOnly: Boolean) {
        viewModelScope.launch {
            runCatching {
                setCameraUploadsByWifiUseCase(wifiOnly)
                refreshUploadConnectionType()
                stopCameraUploads()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Change the Upload Option of Camera Uploads
     *
     * @param uploadOption The new [UploadOption]
     */
    fun changeUploadOption(uploadOption: UploadOption) {
        viewModelScope.launch {
            runCatching {
                setUploadOptionUseCase(uploadOption)
                refreshUploadOption()
                deleteCameraUploadsTemporaryRootDirectoryUseCase()
                stopCameraUploads()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Sets whether to include Location Tags in Photos or not
     *
     * @param include If true, Location Tags will be included for every Photo upload
     * If false, no Location Tags will be included when uploading Photos
     */
    fun includeLocationTags(include: Boolean) {
        viewModelScope.launch {
            runCatching {
                setLocationTagsEnabledUseCase(include)
                refreshLocationTags()
                stopCameraUploads()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Change the Video Quality for videos to be uploaded. The Video Sync Status will also
     * be updated depending on the new Video Quality selected
     *
     * @param value The new Video Quality, represented as an Integer from the list
     */
    fun changeUploadVideoQuality(value: Int) {
        viewModelScope.launch {
            runCatching {
                VideoQuality.entries.find { it.value == value }?.let { videoQuality ->
                    setUploadVideoQualityUseCase(videoQuality)
                    refreshUploadVideoQuality()
                    stopCameraUploads()
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Sets whether charging is required for video compression or not
     *
     * @param chargingRequired True if charging is required for video compression, and false
     * if otherwise
     */
    fun changeChargingRequiredForVideoCompression(chargingRequired: Boolean) {
        viewModelScope.launch {
            runCatching {
                setChargingRequiredForVideoCompressionUseCase(chargingRequired)
                refreshChargingRequiredForVideoCompression()
                stopCameraUploads()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Sets whether the File Names of files to be uploaded will be kept or not
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    fun keepUploadFileNames(keepFileNames: Boolean) {
        viewModelScope.launch {
            runCatching {
                setUploadFileNamesKeptUseCase(keepFileNames)
                refreshUploadFilesNamesKept()
                stopCameraUploads()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Sets the new Primary Folder after selecting a Folder from File Explorer
     *
     * @param newPath The new Primary Folder path, which may be nullable
     */
    fun setPrimaryFolder(newPath: String?) {
        viewModelScope.launch {
            runCatching {
                if (isPrimaryFolderPathValidUseCase(newPath)) {
                    newPath?.let { setPrimaryFolderPathUseCase(it) }
                    deleteCameraUploadsTemporaryRootDirectoryUseCase()
                    clearCameraUploadsRecordUseCase(listOf(CameraUploadFolderType.Primary))
                    stopCameraUploads()
                    _state.update { it.copy(primaryFolderPath = newPath.orEmpty()) }
                } else {
                    Timber.e("The new Folder for Primary Uploads is invalid")
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.error_invalid_folder_selected,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
            }.onFailure {
                Timber.e("An exception occurred when setting the new Folder for Primary Uploads:\n$it")
                snackBarHandler.postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
        }
    }

    /**
     * Sets the new Primary Folder from Cloud Drive
     *
     * @param newHandle The handle of the new Primary Folder from Cloud Drive
     */
    fun setPrimaryUploadNode(newHandle: Long) {
        viewModelScope.launch {
            runCatching {
                if (isPrimaryFolderNodeValidUseCase(newHandle)) {
                    setupPrimaryFolderUseCase(newHandle)
                    stopCameraUploads()
                } else {
                    Timber.e("The new Cloud Drive Folder for Primary Uploads is invalid")
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.error_invalid_folder_selected,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
            }.onFailure {
                Timber.e("An exception occurred when setting the new Cloud Drive Folder for Primary Uploads:\n$it")
                snackBarHandler.postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
        }
    }

    /**
     * When [LegacySettingsCameraUploadsViewModel] is instantiated, initialize the UI Elements
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            preparePrimaryFolderPathUseCase()

            val isCameraUploadsEnabled = async { isCameraUploadsEnabledUseCase() }
            val areLocationTagsIncluded = async { areLocationTagsEnabledUseCase() }
            val areUploadFileNamesKept = async { areUploadFileNamesKeptUseCase() }
            val isChargingRequiredForVideoCompression =
                async { isChargingRequiredForVideoCompressionUseCase() }
            val uploadConnectionType = async { getUploadConnectionType() }
            val getUploadOption = async { getUploadOptionUseCase() }
            val primaryFolderPath = async { getPrimaryFolderPathUseCase() }
            val videoCompressionSizeLimit = async { getVideoCompressionSizeLimitUseCase() }
            val videoQuality = async { getUploadVideoQualityUseCase() }
            val primaryUploadNode = async { getPrimaryFolderNode() }
            val isMediaUploadEnabled = async { isSecondaryFolderEnabledUseCase() }
            val secondaryUploadNode = async { getSecondaryFolderNode() }
            val secondaryFolderPath = async { getSecondaryFolderPathUseCase() }
            _state.update {
                it.copy(
                    isCameraUploadsEnabled = isCameraUploadsEnabled.await(),
                    isMediaUploadsEnabled = isMediaUploadEnabled.await(),
                    areLocationTagsIncluded = areLocationTagsIncluded.await(),
                    areUploadFileNamesKept = areUploadFileNamesKept.await(),
                    isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression.await(),
                    primaryFolderPath = primaryFolderPath.await(),
                    uploadConnectionType = uploadConnectionType.await(),
                    uploadOption = getUploadOption.await(),
                    videoCompressionSizeLimit = videoCompressionSizeLimit.await(),
                    videoQuality = videoQuality.await(),
                    primaryFolderName = primaryUploadNode.await()?.name ?: "",
                    secondaryFolderName = secondaryUploadNode.await()?.name ?: "",
                    secondaryFolderPath = secondaryFolderPath.await(),
                )
            }
        }
    }

    private suspend fun getPrimaryFolderNode(nodeHandle: Long? = null): TypedNode? =
        runCatching {
            val id = nodeHandle ?: getPrimarySyncHandleUseCase()
            if (id == -1L) {
                return@runCatching null
            }
            return@runCatching getNodeByIdUseCase(NodeId(id)).also {
                if (it == null) {
                    setInvalidCameraUploadsHandle()
                }
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()

    private suspend fun getSecondaryFolderNode(nodeHandle: Long? = null): TypedNode? =
        runCatching {
            val id = nodeHandle ?: getSecondarySyncHandleUseCase()
            if (id == -1L) {
                return@runCatching null
            }
            return@runCatching getNodeByIdUseCase(NodeId(id)).also {
                if (it == null) {
                    setInvalidMediaUploadsHandle()
                }
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()


    /**
     * Updates the [LegacySettingsCameraUploadsState.primaryFolderName]
     */
    fun updatePrimaryUploadNode(nodeHandle: Long) {
        viewModelScope.launch {
            val node = getPrimaryFolderNode(nodeHandle)
            _state.update {
                it.copy(primaryFolderName = node?.name ?: "")
            }
        }
    }

    /**
     * Updates the [LegacySettingsCameraUploadsState.secondaryFolderName]
     */
    fun updateSecondaryUploadNode(nodeHandle: Long) {
        viewModelScope.launch {
            val node = getSecondaryFolderNode(nodeHandle)
            _state.update {
                it.copy(secondaryFolderName = node?.name ?: "")
            }
        }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.uploadConnectionType] whenever a new Upload
     * Connection type is set
     */
    private suspend fun refreshUploadConnectionType() {
        val uploadConnectionType = getUploadConnectionType()
        _state.update { it.copy(uploadConnectionType = uploadConnectionType) }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.uploadOption] whenever a new
     * Upload Connection type is set
     */
    private suspend fun refreshUploadOption() {
        val uploadOption = getUploadOptionUseCase()
        _state.update { it.copy(uploadOption = uploadOption) }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.areLocationTagsIncluded] whenever changes
     * to include / exclude Location Tags for Photo uploads are found
     */
    private suspend fun refreshLocationTags() {
        val areLocationTagsIncluded = areLocationTagsEnabledUseCase()
        _state.update { it.copy(areLocationTagsIncluded = areLocationTagsIncluded) }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.videoQuality] whenever a new upload
     * Video Quality has been set
     */
    private suspend fun refreshUploadVideoQuality() {
        val videoQuality = getUploadVideoQualityUseCase()
        _state.update { it.copy(videoQuality = videoQuality) }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.isChargingRequiredForVideoCompression] whenever
     * a change to require charging for video compression is found
     */
    private suspend fun refreshChargingRequiredForVideoCompression() {
        val isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompressionUseCase()
        _state.update {
            it.copy(isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression)
        }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.videoCompressionSizeLimit] whenever the
     * maximum video compression size limit changes
     */
    private suspend fun refreshVideoCompressionSizeLimit() {
        val videoCompressionSizeLimit = getVideoCompressionSizeLimitUseCase()
        _state.update { it.copy(videoCompressionSizeLimit = videoCompressionSizeLimit) }
    }

    /**
     * Updates the value of [LegacySettingsCameraUploadsState.areUploadFileNamesKept] whenever File Name
     * changes for uploads are found
     */
    private suspend fun refreshUploadFilesNamesKept() {
        val areUploadFileNamesKept = areUploadFileNamesKeptUseCase()
        _state.update { it.copy(areUploadFileNamesKept = areUploadFileNamesKept) }
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
     *  Start camera uploads
     */
    fun startCameraUploads() = viewModelScope.launch {
        runCatching {
            startCameraUploadUseCase()
            listenToNewMediaUseCase(forceEnqueue = false)
        }.onFailure { Timber.e(it) }
    }

    /**
     * Stop camera uploads
     */
    private suspend fun stopCameraUploads() {
        stopCameraUploadsUseCase(CameraUploadsRestartMode.Stop)
    }

    /**
     * Stop and disable camera uploads
     * Cancel camera upload and heartbeat workers
     */
    private suspend fun stopAndDisableCameraUploads() {
        setCameraUploadsEnabled(isEnabled = false)
        stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
    }

    /**
     * Set Invalid Camera Uploads Sync Handle
     */
    private suspend fun setInvalidCameraUploadsHandle() {
        setupCameraUploadsSyncHandleUseCase(handle = -1L)
    }

    /**
     * Set Invalid Camera Uploads Sync Handle
     */
    private suspend fun setInvalidMediaUploadsHandle() {
        setupMediaUploadsSyncHandleUseCase(handle = -1L)
    }

    /**
     * toggle MediaUploads
     * on Enable or Disable MediaUpload
     */
    fun toggleMediaUploads() {
        viewModelScope.launch {
            if (isConnected.not()) return@launch
            if (_state.value.isMediaUploadsEnabled) {
                // we need to disable media upload
                disableMediaUploads()
            } else {
                enableMediaUploads()
            }
        }
    }

    /**
     * Sets the new Secondary Folder after selecting a Folder from File Explorer
     *
     * @param newPath The new Secondary Folder path, which may be nullable
     */
    fun setSecondaryFolder(newPath: String?) {
        viewModelScope.launch {
            runCatching {
                if (isSecondaryFolderPathValidUseCase(newPath)) {
                    newPath?.let { setSecondaryFolderLocalPathUseCase(it) }
                    clearCameraUploadsRecordUseCase(listOf(CameraUploadFolderType.Secondary))
                    stopCameraUploads()
                    _state.update { it.copy(secondaryFolderPath = newPath.orEmpty()) }
                } else {
                    Timber.e("The new Folder for Secondary Uploads is invalid")
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.error_invalid_folder_selected,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
            }.onFailure {
                Timber.e("An exception occurred when setting the new Folder for Secondary Uploads:\n$it")
                snackBarHandler.postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
        }
    }

    /**
     * toggle CameraUploads Settings
     */
    fun toggleCameraUploadsSettings() {
        viewModelScope.launch {
            runCatching {
                if (isCameraUploadsEnabledUseCase()) {
                    stopAndDisableCameraUploads()
                } else {
                    if (hasMediaPermissionUseCase()) {
                        handleEnableCameraUploads()
                    } else {
                        _state.update {
                            it.copy(shouldTriggerPermissionDialog = true)
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * on Consume Trigger Permission Dialog
     */
    fun onConsumeTriggerPermissionDialog() {
        viewModelScope.launch {
            _state.update {
                it.copy(shouldTriggerPermissionDialog = false)
            }
        }
    }

    /**
     * Updates [LegacySettingsCameraUploadsState.showNewVideoCompressionSizePrompt] to determine whether
     * to show the New Video Compression Size Dialog or not
     *
     * @param showDialog true if the New Video Compression Size Dialog should be shown
     */
    fun showNewVideoCompressionSizeDialog(showDialog: Boolean) {
        _state.update { it.copy(showNewVideoCompressionSizePrompt = showDialog) }
    }

    /**
     * Updates the established Video Compression Size
     */
    fun setNewVideoCompressionSize(newVideoCompressionSizeString: String) {
        viewModelScope.launch {
            if (newVideoCompressionSizeString.trim().isBlank()) {
                // Dismiss the Prompt if an empty input is provided
                showNewVideoCompressionSizeDialog(false)
            } else {
                val convertedVideoCompressionSize = try {
                    newVideoCompressionSizeString.toInt()
                } catch (e: NumberFormatException) {
                    Timber.e("The new Video Compression Size is invalid")
                    0
                }
                if (convertedVideoCompressionSize in MINIMUM_NEW_VIDEO_COMPRESSION_SIZE
                    ..MAXIMUM_NEW_VIDEO_COMPRESSION_SIZE
                ) {
                    // Dismiss the Prompt and set the New Video Compression Size
                    showNewVideoCompressionSizeDialog(false)
                    runCatching {
                        setVideoCompressionSizeLimitUseCase(convertedVideoCompressionSize)
                        refreshVideoCompressionSizeLimit()
                        stopCameraUploads()
                    }.onFailure {
                        Timber.e(it)
                    }
                } else {
                    // Clear the Prompt input if the new Video Compression Size does not fall
                    // within the range
                    clearNewVideoCompressionSizeInput()
                }
            }
        }
    }

    /**
     * Notifies the View that the inputted New Video Compression Size should be cleared
     */
    private fun clearNewVideoCompressionSizeInput() {
        _state.update { it.copy(clearNewVideoCompressionSizeInput = true) }
    }

    /**
     * Notifies [LegacySettingsCameraUploadsState.clearNewVideoCompressionSizeInput] that the Event has
     * been performed
     */
    fun onClearNewVideoCompressionSizeInputConsumed() {
        _state.update { it.copy(clearNewVideoCompressionSizeInput = false) }
    }

    companion object {
        /**
         * The minimum Video Compression Size that can be set by the User, represented as MB
         */
        private const val MINIMUM_NEW_VIDEO_COMPRESSION_SIZE = 100

        /**
         * The maximum Video Compression Size that can be set by the User, represented as MB
         */
        private const val MAXIMUM_NEW_VIDEO_COMPRESSION_SIZE = 1000
    }
}
