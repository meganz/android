package mega.privacy.android.app.fragments.settingsFragments

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.TwoLineCheckPreference
import mega.privacy.android.app.constants.SettingsConstants.CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS
import mega.privacy.android.app.constants.SettingsConstants.CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS
import mega.privacy.android.app.constants.SettingsConstants.CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS
import mega.privacy.android.app.constants.SettingsConstants.CAMERA_UPLOAD_WIFI
import mega.privacy.android.app.constants.SettingsConstants.CAMERA_UPLOAD_WIFI_OR_DATA_PLAN
import mega.privacy.android.app.constants.SettingsConstants.COMPRESSION_QUEUE_SIZE_MAX
import mega.privacy.android.app.constants.SettingsConstants.COMPRESSION_QUEUE_SIZE_MIN
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_CAMERA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_CHARGING
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_HOW_TO
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_INCLUDE_GPS
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_MEGA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_ON_OFF
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_VIDEO_QUALITY
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE
import mega.privacy.android.app.constants.SettingsConstants.KEY_CAMERA_UPLOAD_WHAT_TO
import mega.privacy.android.app.constants.SettingsConstants.KEY_KEEP_FILE_NAMES
import mega.privacy.android.app.constants.SettingsConstants.KEY_LOCAL_SECONDARY_MEDIA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.KEY_MEGA_SECONDARY_MEDIA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.KEY_SECONDARY_MEDIA_FOLDER_ON
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_PRIMARY_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_SECONDARY_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_PRIMARY_UPLOAD_NODE
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_SECONDARY_UPLOAD_NODE
import mega.privacy.android.app.constants.SettingsConstants.SELECTED_MEGA_FOLDER
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getPartialMediaPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasAccessMediaLocationPermission
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * [SettingsBaseFragment] that enables or disables Camera Uploads in Settings
 */
@AndroidEntryPoint
class SettingsCameraUploadsFragment : SettingsBaseFragment(),
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {
    private var cameraUploadOnOff: SwitchPreferenceCompat? = null
    private var optionHowToUpload: ListPreference? = null
    private var optionFileUpload: ListPreference? = null
    private var optionIncludeLocationTags: SwitchPreferenceCompat? = null
    private var optionVideoQuality: ListPreference? = null
    private var optionChargingOnVideoCompression: SwitchPreferenceCompat? = null
    private var optionVideoCompressionSize: Preference? = null
    private var optionKeepUploadFileNames: TwoLineCheckPreference? = null
    private var optionLocalCameraFolder: Preference? = null
    private var optionMegaCameraFolder: Preference? = null
    private var secondaryMediaFolderOn: Preference? = null
    private var localSecondaryFolder: Preference? = null
    private var megaSecondaryFolder: Preference? = null
    private var businessCameraUploadsAlertDialog: AlertDialog? = null
    private var newVideoCompressionSizeDialog: AlertDialog? = null
    private var newVideoCompressionSizeInput: EditText? = null
    private var mediaPermissionsDialog: AlertDialog? = null

    private val permissionsList by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                getNotificationsPermission(),
                getImagePermissionByVersion(),
                getVideoPermissionByVersion(),
                getPartialMediaPermission(),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                getNotificationsPermission(),
                getImagePermissionByVersion(),
                getVideoPermissionByVersion()
            )
        } else {
            arrayOf(
                getImagePermissionByVersion(),
                getVideoPermissionByVersion()
            )
        }
    }

    /**
     * Flag that control the start of the Camera Uploads when the user leaves the screen [onPause]
     * to avoid unwanted trigger when the user goes to the folder picker for choosing primary and secondary local folder
     */
    private var canStartCameraUploads = true

    private val viewModel by viewModels<SettingsCameraUploadsViewModel>()

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * Register the permissions callback when the user attempts to enable Camera Uploads
     */
    private val enableCameraUploadsPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) {
            viewModel.handlePermissionsResult()
        }

    /**
     * Registers the Access Media Location permission callback when the user
     * enables the "Include location tags" option
     */
    private val includeLocationTagsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (hasAccessMediaLocationPermission(context)) {
                includeLocationTags(true)
            } else {
                viewModel.showAccessMediaLocationRationale()
            }
        }

    /**
     * onCreateView Implementation
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        val lv = v.findViewById<ListView>(android.R.id.list)
        lv?.setPadding(0, 0, 0, 0)
        setOnlineOptions(viewModel.isConnected && megaApi != null && megaApi.rootNode != null)
        return v
    }

    /**
     * onViewCreated Behavior
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    /**
     * Re-enable the flag that allows the start of the Camera Uploads when leaving the screen
     */
    override fun onResume() {
        super.onResume()
        canStartCameraUploads = true
    }

    /**
     * In onPause(), the Worker to start Camera Uploads will be fired if the user enables
     * the feature
     */
    override fun onPause() {
        Timber.d("CameraUpload enabled through Settings - fireCameraUploadJob()")
        if (canStartCameraUploads)
            viewModel.startCameraUploads()
        super.onPause()
    }

    /**
     * onCreatePreferences implementation that establishes the Camera Uploads controls from Preferences
     *
     * @param savedInstanceState the Saved Instance State
     * @param rootKey the Root Key
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cu)
        cameraUploadOnOff = findPreference(KEY_CAMERA_UPLOAD_ON_OFF)
        cameraUploadOnOff?.let {
            it.isEnabled = true
            it.setOnPreferenceChangeListener { _, _ ->
                if (viewModel.isConnected) {
                    viewModel.toggleCameraUploadsSettings()
                }
                false
            }
        }

        optionHowToUpload = findPreference(KEY_CAMERA_UPLOAD_HOW_TO)
        optionHowToUpload?.onPreferenceChangeListener = this

        optionFileUpload = findPreference(KEY_CAMERA_UPLOAD_WHAT_TO)
        optionFileUpload?.onPreferenceChangeListener = this

        optionIncludeLocationTags = findPreference(KEY_CAMERA_UPLOAD_INCLUDE_GPS)
        optionIncludeLocationTags?.onPreferenceClickListener = this

        optionVideoQuality = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUALITY)
        optionVideoQuality?.onPreferenceChangeListener = this

        optionChargingOnVideoCompression = findPreference(KEY_CAMERA_UPLOAD_CHARGING)
        optionChargingOnVideoCompression?.onPreferenceClickListener = this

        optionVideoCompressionSize = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE)
        optionVideoCompressionSize?.onPreferenceClickListener = this

        optionKeepUploadFileNames = findPreference(KEY_KEEP_FILE_NAMES)
        optionKeepUploadFileNames?.onPreferenceClickListener = this

        optionLocalCameraFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER)
        optionLocalCameraFolder?.onPreferenceClickListener = this

        optionMegaCameraFolder = findPreference(KEY_CAMERA_UPLOAD_MEGA_FOLDER)
        optionMegaCameraFolder?.onPreferenceClickListener = this

        secondaryMediaFolderOn = findPreference(KEY_SECONDARY_MEDIA_FOLDER_ON)
        secondaryMediaFolderOn?.let {
            it.isEnabled = true
            it.onPreferenceClickListener = this
        }

        localSecondaryFolder = findPreference(KEY_LOCAL_SECONDARY_MEDIA_FOLDER)
        localSecondaryFolder?.onPreferenceClickListener = this

        megaSecondaryFolder = findPreference(KEY_MEGA_SECONDARY_MEDIA_FOLDER)
        megaSecondaryFolder?.onPreferenceClickListener = this
    }

    /**
     * onPreferenceClick implementation
     *
     * @param preference The [Preference] object
     */
    @Suppress("DEPRECATION")
    override fun onPreferenceClick(preference: Preference): Boolean {
        megaChatApi.signalPresenceActivity()
        val intent: Intent
        when (preference.key) {
            KEY_CAMERA_UPLOAD_INCLUDE_GPS -> {
                val isChecked = optionIncludeLocationTags?.isChecked ?: false
                if (isChecked) {
                    if (hasAccessMediaLocationPermission(context)) {
                        includeLocationTags(true)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            includeLocationTagsLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                        }
                    }
                } else {
                    includeLocationTags(false)
                }
            }

            KEY_CAMERA_UPLOAD_CHARGING -> {
                val chargingRequired = optionChargingOnVideoCompression?.isChecked ?: false
                viewModel.changeChargingRequiredForVideoCompression(chargingRequired)
            }

            KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE -> viewModel.showNewVideoCompressionSizeDialog(true)
            KEY_KEEP_FILE_NAMES -> {
                val keepFileNames = optionKeepUploadFileNames?.isChecked ?: false
                viewModel.keepUploadFileNames(keepFileNames)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_keep_device_name),
                    Toast.LENGTH_SHORT
                ).show()
            }

            KEY_CAMERA_UPLOAD_CAMERA_FOLDER -> {
                canStartCameraUploads = false
                intent = Intent(context, FileStorageActivity::class.java).apply {
                    action = FileStorageActivity.Mode.PICK_FOLDER.action
                    putExtra(
                        FileStorageActivity.PICK_FOLDER_TYPE,
                        FileStorageActivity.PickFolderType.CU_FOLDER.folderType
                    )
                }
                startActivityForResult(intent, REQUEST_PRIMARY_FOLDER)
            }

            KEY_CAMERA_UPLOAD_MEGA_FOLDER -> {
                if (viewModel.isConnected.not()) return false
                canStartCameraUploads = false
                intent = Intent(context, FileExplorerActivity::class.java).apply {
                    action = FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC
                }
                startActivityForResult(intent, REQUEST_PRIMARY_UPLOAD_NODE)
            }

            KEY_SECONDARY_MEDIA_FOLDER_ON -> {
                viewModel.toggleMediaUploads()
            }

            KEY_LOCAL_SECONDARY_MEDIA_FOLDER -> {
                canStartCameraUploads = false
                intent = Intent(context, FileStorageActivity::class.java).apply {
                    action = FileStorageActivity.Mode.PICK_FOLDER.action
                    putExtra(
                        FileStorageActivity.PICK_FOLDER_TYPE,
                        FileStorageActivity.PickFolderType.MU_FOLDER.folderType
                    )
                }
                startActivityForResult(intent, REQUEST_SECONDARY_FOLDER)
            }

            KEY_MEGA_SECONDARY_MEDIA_FOLDER -> {
                if (viewModel.isConnected.not()) return false
                canStartCameraUploads = false
                intent = Intent(context, FileExplorerActivity::class.java).apply {
                    action = FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC
                }
                startActivityForResult(intent, REQUEST_SECONDARY_UPLOAD_NODE)
            }
        }
        return true
    }

    /**
     * Handles specific actions when a [Preference] is enabled
     *
     * @param preference The [Preference]
     * @param newValue Arbitrary value depending on what [Preference] is enabled
     */
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val value: Int = (newValue as String).toInt()
        when (preference.key) {
            KEY_CAMERA_UPLOAD_HOW_TO -> {
                when (value) {
                    CAMERA_UPLOAD_WIFI -> {
                        viewModel.changeUploadConnectionType(wifiOnly = true)
                    }

                    CAMERA_UPLOAD_WIFI_OR_DATA_PLAN -> {
                        viewModel.changeUploadConnectionType(wifiOnly = false)
                    }
                }
            }

            KEY_CAMERA_UPLOAD_WHAT_TO -> {
                when (value) {
                    CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS -> {
                        viewModel.changeUploadOption(UploadOption.PHOTOS)
                    }

                    CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS -> {
                        viewModel.changeUploadOption(UploadOption.VIDEOS)
                    }

                    CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS -> {
                        viewModel.changeUploadOption(UploadOption.PHOTOS_AND_VIDEOS)
                    }
                }
            }

            KEY_CAMERA_UPLOAD_VIDEO_QUALITY -> {
                viewModel.changeUploadVideoQuality(value)
            }
        }
        return true
    }

    /**
     * onActivityResult Behavior Declarations
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_OK || intent == null) return
        Timber.d("REQUEST CODE: %d___RESULT CODE: %d", requestCode, resultCode)
        when (requestCode) {
            REQUEST_PRIMARY_FOLDER -> {
                val newPrimaryFolderPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH)
                viewModel.setPrimaryFolder(newPrimaryFolderPath)
            }

            REQUEST_PRIMARY_UPLOAD_NODE -> {
                val newMegaPrimaryFolderPath = intent.getLongExtra(SELECTED_MEGA_FOLDER, -1L)
                viewModel.setPrimaryUploadNode(newMegaPrimaryFolderPath)
            }

            REQUEST_SECONDARY_FOLDER -> {
                val secondaryPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH)
                viewModel.setSecondaryFolder(secondaryPath)
            }

            REQUEST_SECONDARY_UPLOAD_NODE -> {
                val secondaryHandle =
                    intent.getLongExtra(SELECTED_MEGA_FOLDER, MegaApiJava.INVALID_HANDLE)
                if (!viewModel.isNewSettingValid(secondaryHandle = secondaryHandle)) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_invalid_folder_selected),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                if (secondaryHandle != MegaApiJava.INVALID_HANDLE) {
                    Timber.d("Set Camera Uploads Secondary Attribute: %s", secondaryHandle)
                    viewModel.setSecondaryUploadNode(secondaryHandle)
                } else {
                    Timber.e("Error choosing the Mega folder for Secondary Uploads")
                }
            }
        }
    }

    /**
     * Setup [SettingsCameraUploadsViewModel] Observers
     */
    private fun setupObservers() {
        viewLifecycleOwner.run {
            collectFlow(viewModel.monitorConnectivityEvent) { isConnected ->
                setOnlineOptions(isConnected)
            }
            collectFlow(viewModel.state) {
                Timber.d("SettingsCameraUploadsState $it")
                handleIsCameraUploadsEnabled(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled
                )
                checkSecondaryMediaFolder(
                    isMediaUploadsEnabled = it.isMediaUploadsEnabled,
                    secondaryFolderName = it.secondaryFolderName,
                    secondaryFolderPath = it.secondaryFolderPath
                )
                handleFileUpload(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    uploadOption = it.uploadOption,
                )
                handleHowToUpload(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    uploadConnectionType = it.uploadConnectionType,
                )
                handleIncludeLocationTags(
                    areLocationTagsIncluded = it.areLocationTagsIncluded,
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    uploadOption = it.uploadOption,
                )
                handleUploadVideoQuality(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    uploadOption = it.uploadOption,
                    videoQuality = it.videoQuality,
                )
                handleChargingOnVideoCompression(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    isChargingRequiredForVideoCompression = it.isChargingRequiredForVideoCompression,
                    uploadOption = it.uploadOption,
                    videoCompressionSizeLimit = it.videoCompressionSizeLimit,
                    videoQuality = it.videoQuality,
                )
                handleVideosToCompressSize(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    isChargingRequiredForVideoCompression = it.isChargingRequiredForVideoCompression,
                    uploadOption = it.uploadOption,
                    videoCompressionSizeLimit = it.videoCompressionSizeLimit,
                    videoQuality = it.videoQuality,
                )
                handleNewVideoCompressionSizeChange(it.showNewVideoCompressionSizePrompt)
                handleClearNewVideoCompressionSizeInputEvent(it.clearNewVideoCompressionSizeInput)
                handleKeepUploadFileNames(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    areUploadFileNamesKept = it.areUploadFileNamesKept,
                )
                handlePrimaryFolderPath(
                    isCameraUploadsEnabled = it.isCameraUploadsEnabled,
                    primaryFolderPath = it.primaryFolderPath,
                )
                handleBusinessAccountPrompt(it.shouldShowBusinessAccountPrompt)
                if (it.isCameraUploadsEnabled) {
                    handlePrimaryFolderName(it.primaryFolderName)
                }
                handleMediaPermissionsRationale(it.shouldShowMediaPermissionsRationale)
                handleShouldTriggerPermissionDialog(it.shouldTriggerPermissionDialog)
            }
            collectFlow(viewModel.monitorCameraUploadsSettingsActions) {
                when (it) {
                    CameraUploadsSettingsAction.DisableMediaUploads -> viewModel.toggleMediaUploads()
                    CameraUploadsSettingsAction.RefreshSettings -> viewModel.toggleCameraUploadsSettings()
                }
            }
            collectFlow(viewModel.monitorBackupInfoType) {
                reEnableCameraUploadsPreference(it)
            }
            collectFlow(viewModel.monitorCameraUploadsFolderDestination) {
                setCameraUploadsDestinationFolder(it)
            }
        }
    }

    /**
     * Display primary upload folder's name
     */
    private fun handlePrimaryFolderName(primaryFolderName: String) {
        optionMegaCameraFolder?.let { preference ->
            preference.summary = primaryFolderName.takeIf { it.isNotEmpty() }
                ?: getString(R.string.section_photo_sync)
            preferenceScreen.addPreference(preference)
        }
    }

    /**
     * Handles the Camera Uploads enable option
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     */
    private fun handleIsCameraUploadsEnabled(isCameraUploadsEnabled: Boolean) {
        if (isCameraUploadsEnabled) {
            Timber.d("Camera Uploads ON")
            cameraUploadOnOff?.isEnabled = true
            cameraUploadOnOff?.isChecked = true
            secondaryMediaFolderOn?.let { preferenceScreen.addPreference(it) }
        } else {
            Timber.d("Camera Uploads Off")
            cameraUploadOnOff?.isChecked = false
            secondaryMediaFolderOn?.let { preferenceScreen.removePreference(it) }

            optionMegaCameraFolder?.let {
                it.summary = ""
                preferenceScreen.removePreference(it)
            }
            localSecondaryFolder?.let {
                it.summary = ""
                preferenceScreen.removePreference(it)
            }
            megaSecondaryFolder?.let {
                it.summary = ""
                preferenceScreen.removePreference(it)
            }
        }
    }

    /**
     * Handles the "File upload" option visibility and content when a UI State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param uploadOption the specific [UploadOption] which can be nullable
     */
    private fun handleFileUpload(
        isCameraUploadsEnabled: Boolean,
        uploadOption: UploadOption?,
    ) {
        optionFileUpload?.run {
            summary = if (isCameraUploadsEnabled && uploadOption != null) {
                preferenceScreen.addPreference(this)
                setValueIndex(uploadOption.position)
                when (uploadOption) {
                    UploadOption.PHOTOS -> getString(R.string.settings_camera_upload_only_photos)
                    UploadOption.VIDEOS -> getString(R.string.settings_camera_upload_only_videos)
                    UploadOption.PHOTOS_AND_VIDEOS -> getString(R.string.settings_camera_upload_photos_and_videos)
                }
            } else {
                preferenceScreen.removePreference(this)
                ""
            }
        }
    }

    /**
     * Handles the "How to Upload" option visibility and content when a UI State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param uploadConnectionType the specific [UploadConnectionType] which can be nullable
     */
    private fun handleHowToUpload(
        isCameraUploadsEnabled: Boolean,
        uploadConnectionType: UploadConnectionType?,
    ) {
        optionHowToUpload?.run {
            if (isCameraUploadsEnabled && uploadConnectionType != null) {
                preferenceScreen.addPreference(this)
                summary = getString(uploadConnectionType.textRes)
                setValueIndex(uploadConnectionType.position)
            } else {
                preferenceScreen.removePreference(this)
                summary = ""
            }
        }
    }

    /**
     * Handles the "Include location tags" option visibility and checked state when a UI State change happens
     *
     * @param areLocationTagsIncluded Whether Location Tags are embedded when uploading Photos or not
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     */
    private fun handleIncludeLocationTags(
        areLocationTagsIncluded: Boolean,
        isCameraUploadsEnabled: Boolean,
        uploadOption: UploadOption?,
    ) {
        val allowedOptions = listOf(UploadOption.PHOTOS, UploadOption.PHOTOS_AND_VIDEOS)

        optionIncludeLocationTags?.run {
            if (isCameraUploadsEnabled && allowedOptions.contains(uploadOption)) {
                preferenceScreen.addPreference(this)
                this.isChecked = areLocationTagsIncluded
            } else {
                preferenceScreen.removePreference(this)
                this.isChecked = false
            }
        }
    }

    /**
     * Handles the "Video quality" option visibility and content when a UI State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     * @param videoQuality The specific [VideoQuality] which can be nullable
     */
    private fun handleUploadVideoQuality(
        isCameraUploadsEnabled: Boolean,
        uploadOption: UploadOption?,
        videoQuality: VideoQuality?,
    ) {
        optionVideoQuality?.run {
            summary = if (isCameraUploadsEnabled && videoQuality != null && uploadOption in listOf(
                    UploadOption.VIDEOS,
                    UploadOption.PHOTOS_AND_VIDEOS
                )
            ) {
                preferenceScreen.addPreference(this)
                setValueIndex(videoQuality.value)
                this.entry
            } else {
                preferenceScreen.removePreference(this)
                ""
            }
        }
    }

    /**
     * Handles the "Require me to actively charge my device" option visibility, content and checked
     * state when a UI State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param isChargingRequiredForVideoCompression Whether compressing videos require the device
     * to be charged or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     * @param videoCompressionSizeLimit The maximum video file size that can be compressed
     * @param videoQuality The specific [VideoQuality] which can be nullable
     */
    private fun handleChargingOnVideoCompression(
        isCameraUploadsEnabled: Boolean,
        isChargingRequiredForVideoCompression: Boolean,
        uploadOption: UploadOption?,
        videoCompressionSizeLimit: Int,
        videoQuality: VideoQuality?,
    ) {
        optionChargingOnVideoCompression?.run {
            summary =
                if (isCameraUploadsEnabled && uploadOption in listOf(
                        UploadOption.VIDEOS,
                        UploadOption.PHOTOS_AND_VIDEOS,
                    ) && videoQuality != null && videoQuality != VideoQuality.ORIGINAL
                ) {
                    preferenceScreen.addPreference(this)
                    isChecked = isChargingRequiredForVideoCompression
                    getString(
                        R.string.settings_camera_upload_charging_helper_label,
                        getString(
                            R.string.label_file_size_mega_byte,
                            videoCompressionSizeLimit.toString()
                        )
                    )
                } else {
                    preferenceScreen.removePreference(this)
                    isChecked = false
                    ""
                }
        }
    }

    /**
     * Handles the "If videos to compress are larger than" option visibility and content when a UI
     * State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param isChargingRequiredForVideoCompression Whether compressing videos require the device
     * to be charged or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     * @param videoCompressionSizeLimit The maximum video file size that can be compressed
     * @param videoQuality The specific [VideoQuality] which can be nullable
     */
    private fun handleVideosToCompressSize(
        isCameraUploadsEnabled: Boolean,
        isChargingRequiredForVideoCompression: Boolean,
        uploadOption: UploadOption?,
        videoCompressionSizeLimit: Int,
        videoQuality: VideoQuality?,
    ) {
        optionVideoCompressionSize?.run {
            summary =
                if (isCameraUploadsEnabled && uploadOption in listOf(
                        UploadOption.VIDEOS,
                        UploadOption.PHOTOS_AND_VIDEOS,
                    ) && videoQuality != null && videoQuality != VideoQuality.ORIGINAL && isChargingRequiredForVideoCompression
                ) {
                    preferenceScreen.addPreference(this)
                    getString(
                        R.string.label_file_size_mega_byte,
                        videoCompressionSizeLimit.toString()
                    )
                } else {
                    preferenceScreen.removePreference(this)
                    ""
                }
        }
    }

    /**
     * Handles the "Notify me when size is larger than" Dialog visibility and content when a UI State
     * change happens
     *
     * @param showNewVideoCompressionSizePrompt If true, the Dialog is shown
     */
    private fun handleNewVideoCompressionSizeChange(showNewVideoCompressionSizePrompt: Boolean) {
        if (showNewVideoCompressionSizePrompt) {
            if (newVideoCompressionSizeDialog?.isShowing == true) {
                return
            }
            // Create and show the New Video Compression Size Input
            newVideoCompressionSizeDialog = buildNewVideoCompressionSizeDialog().show()
        } else {
            newVideoCompressionSizeDialog?.dismiss()
        }
    }

    /**
     * Clears the inputted New Video Compression Size upon receiving a prompt
     */
    private fun handleClearNewVideoCompressionSizeInputEvent(clearNewVideoCompressionSizeInput: Boolean) {
        if (clearNewVideoCompressionSizeInput) {
            newVideoCompressionSizeInput?.let { editText ->
                with(editText) {
                    setText("")
                    requestFocus()
                }
            }
            viewModel.onClearNewVideoCompressionSizeInputConsumed()
        }
    }

    /**
     * Builds the new Video Compression Size Dialog
     *
     * @return a [MaterialAlertDialogBuilder] used to create the Dialog
     */
    @Suppress("DEPRECATION")
    private fun buildNewVideoCompressionSizeDialog(): MaterialAlertDialogBuilder {
        val display = requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display?.getMetrics(outMetrics)
        val margin = 20
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        var params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            Util.dp2px(margin.toFloat(), outMetrics),
            Util.dp2px(margin.toFloat(), outMetrics),
            Util.dp2px(margin.toFloat(), outMetrics),
            0
        )

        newVideoCompressionSizeInput = EditText(context)
        newVideoCompressionSizeInput?.let { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            layout.addView(editText, params)
            editText.setSingleLine()
            editText.setTextColor(
                getThemeColor(
                    requireContext(),
                    android.R.attr.textColorSecondary
                )
            )
            editText.hint = getString(R.string.label_mega_byte)
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.setNewVideoCompressionSize(textView.text.toString())
                    return@setOnEditorActionListener true
                }
                false
            }
            editText.setImeActionLabel(
                getString(R.string.general_create),
                EditorInfo.IME_ACTION_DONE
            )
            editText.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) Util.showKeyboardDelayed(view)
            }
        }

        params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            Util.dp2px((margin + 5).toFloat(), outMetrics),
            Util.dp2px(0f, outMetrics),
            Util.dp2px(margin.toFloat(), outMetrics),
            0
        )
        val textView = TextView(context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        textView.text = getString(
            R.string.settings_compression_queue_subtitle,
            getString(
                R.string.label_file_size_mega_byte,
                COMPRESSION_QUEUE_SIZE_MIN.toString()
            ),
            getString(
                R.string.label_file_size_mega_byte,
                COMPRESSION_QUEUE_SIZE_MAX.toString()
            )
        )
        layout.addView(textView, params)

        return MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog,
        )
            .setView(layout)
            .setTitle(getString(R.string.settings_video_compression_queue_size_popup_title))
            .setPositiveButton(R.string.general_ok) { _, _ ->
                newVideoCompressionSizeInput?.let { editText ->
                    viewModel.setNewVideoCompressionSize(editText.text.toString())
                }
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                viewModel.showNewVideoCompressionSizeDialog(false)
            }
    }

    /**
     * Handles the "Keep file names as in the device" option visibility and content when a UI State
     * change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param areUploadFileNamesKept Whether the File Names are kept or not when uploading content
     */
    private fun handleKeepUploadFileNames(
        isCameraUploadsEnabled: Boolean,
        areUploadFileNamesKept: Boolean,
    ) {
        optionKeepUploadFileNames?.run {
            isChecked = if (isCameraUploadsEnabled) {
                preferenceScreen.addPreference(this)
                areUploadFileNamesKept
            } else {
                preferenceScreen.removePreference(this)
                false
            }
        }
    }

    /**
     * Handles the "Local Camera folder" option visibility and content when a UI State change happens
     *
     * @param isCameraUploadsEnabled Whether Camera Uploads is enabled or not
     * @param primaryFolderPath The Primary Folder path
     */
    private fun handlePrimaryFolderPath(
        isCameraUploadsEnabled: Boolean,
        primaryFolderPath: String,
    ) {
        optionLocalCameraFolder?.run {
            summary = if (isCameraUploadsEnabled) {
                preferenceScreen.addPreference(this)
                primaryFolderPath
            } else {
                preferenceScreen.removePreference(this)
                ""
            }
        }
    }

    /**
     * Handle the Business Account prompt when a UI State change happens
     *
     * @param showPrompt If true, display a Dialog explaining that the Business Account
     * Administrator can access the user's Camera Uploads
     */
    private fun handleBusinessAccountPrompt(showPrompt: Boolean) {
        if (showPrompt) {
            if (businessCameraUploadsAlertDialog != null && (businessCameraUploadsAlertDialog
                    ?: return).isShowing
            ) {
                return
            }
            val builder = MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
            builder.setTitle(R.string.section_photo_sync)
                .setMessage(R.string.camera_uploads_business_alert)
                .setNegativeButton(R.string.general_cancel) { _, _ -> }
                // Clicking this Button will enable Camera Uploads
                .setPositiveButton(R.string.general_enable) { _, _ ->
                    viewModel.onCameraUploadsEnabled()
                }
                .setCancelable(false)
                // Reset the Business Account Prompt State when the Dialog is dismissed
                .setOnDismissListener { viewModel.resetBusinessAccountPromptState() }
            businessCameraUploadsAlertDialog = builder.create()
            businessCameraUploadsAlertDialog?.show()
        }
    }

    /**
     * Handle the display of the Media Permissions rationale when a UI State change happens
     *
     * @param showRationale If true, display the Media Permissions rationale to inform the user
     * that Media Permissions should be granted in order to enable Camera Uploads
     */
    private fun handleMediaPermissionsRationale(showRationale: Boolean) {
        if (showRationale) {
            if (mediaPermissionsDialog?.isShowing == true) mediaPermissionsDialog?.dismiss()

            mediaPermissionsDialog = MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
                .setMessage(R.string.settings_camera_uploads_grant_media_permissions_body)
                .setPositiveButton(R.string.settings_camera_uploads_grant_media_permissions_positive_button) { dialog, _ ->
                    if (shouldShowMediaPermissionsRationale()) {
                        enableCameraUploadsPermissionLauncher.launch(permissionsList)
                    } else {
                        // User has selected "Never Ask Again". Starting Android 11, selecting "Deny"
                        // more than once is equivalent to selecting "Never Ask Again"
                        requireContext().navigateToAppSettings()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.settings_camera_uploads_grant_media_permissions_negative_button) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    // Once the Dialog is dismissed, reset the Media Permissions Rationale state
                    viewModel.setMediaPermissionsRationaleState(shouldShow = false)
                }
                .show()
        }
    }

    private fun handleShouldTriggerPermissionDialog(shouldTriggerPermissionDialog: Boolean) {
        if (shouldTriggerPermissionDialog) {
            enableCameraUploadsPermissionLauncher.launch(permissionsList)
            viewModel.onConsumeTriggerPermissionDialog()
        }
    }

    /**
     * Includes or excludes adding Location tags to Photo uploads. This also stops
     * Camera Uploads
     *
     * @param include true if Location data should be added to Photos, and false if otherwise
     */
    private fun includeLocationTags(include: Boolean) {
        viewModel.includeLocationTags(include)
    }

    /**
     * Checks whether a rationale is needed for Media Permissions
     *
     * @return Boolean value
     */
    private fun shouldShowMediaPermissionsRationale() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            ) || shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            )
        } else {
            shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)
        }

    /**
     * Checks the Secondary Folder
     */
    private fun checkSecondaryMediaFolder(
        isMediaUploadsEnabled: Boolean,
        secondaryFolderName: String,
        secondaryFolderPath: String,
    ) {
        if (isMediaUploadsEnabled) {
            secondaryMediaFolderOn?.title = getString(R.string.settings_secondary_upload_off)
            megaSecondaryFolder?.let { preference ->
                preference.summary =
                    secondaryFolderName.takeIf { it.isNotEmpty() }
                        ?: getString(R.string.section_secondary_media_uploads)
                preferenceScreen.addPreference(preference)
            }
            localSecondaryFolder?.let { preference ->
                preference.summary =
                    secondaryFolderPath.takeIf { it.isNotEmpty() }
                        ?: getString(R.string.settings_empty_folder)
                preferenceScreen.addPreference(preference)
            }
        } else {
            disableMediaUploadUIProcess()
        }
    }

    /**
     * Setup the Destination Folder of either Primary or Secondary Uploads
     *
     * @param destination CameraUploadsFolderDestinationUpdate
     */
    private fun setCameraUploadsDestinationFolder(destination: CameraUploadsFolderDestinationUpdate) {
        when (destination.cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> {
                viewModel.updatePrimaryUploadNode(destination.nodeHandle)
            }

            CameraUploadFolderType.Secondary -> {
                viewModel.updateSecondaryUploadNode(destination.nodeHandle)
            }
        }
    }

    /**
     * Enables or disables the Camera Uploads switch
     *
     * @param isOnline Set "true" to enable the Camera Uploads switch and "false" to disable it
     */
    private fun setOnlineOptions(isOnline: Boolean) {
        cameraUploadOnOff?.isEnabled = isOnline
    }

    /**
     * Disable Media Uploads UI-related process
     */
    private fun disableMediaUploadUIProcess() {
        Timber.d("Changes applied to Secondary Folder Only")
        secondaryMediaFolderOn?.title =
            getString(R.string.settings_secondary_upload_on)

        localSecondaryFolder?.let { preferenceScreen.removePreference(it) }
        megaSecondaryFolder?.let { preferenceScreen.removePreference(it) }
    }

    /**
     * Re-enables the Preferences
     *
     * @param which [MegaApiJava] that specifies which part of Camera Uploads is enabled
     */
    private fun reEnableCameraUploadsPreference(which: BackupInfoType) {
        when (which) {
            BackupInfoType.CAMERA_UPLOADS -> {
                Timber.d("${cameraUploadOnOff?.isEnabled}")
                cameraUploadOnOff?.isEnabled = true
                Timber.d("${optionLocalCameraFolder?.isEnabled}")
                optionLocalCameraFolder?.isEnabled = true
                Timber.d("${optionMegaCameraFolder?.isEnabled}")
                optionMegaCameraFolder?.isEnabled = true
                Timber.d("${secondaryMediaFolderOn?.isEnabled}")
                secondaryMediaFolderOn?.isEnabled = true
                Timber.d("${localSecondaryFolder?.isEnabled}")
                localSecondaryFolder?.isEnabled = true
                Timber.d("${megaSecondaryFolder?.isEnabled}")
                megaSecondaryFolder?.isEnabled = true
            }

            BackupInfoType.MEDIA_UPLOADS -> {
                Timber.d("${secondaryMediaFolderOn?.isEnabled}")
                secondaryMediaFolderOn?.isEnabled = true
                Timber.d("${localSecondaryFolder?.isEnabled}")
                localSecondaryFolder?.isEnabled = true
                Timber.d("${megaSecondaryFolder?.isEnabled}")
                megaSecondaryFolder?.isEnabled = true
            }

            else -> Unit
        }
    }
}
