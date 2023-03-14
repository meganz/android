package mega.privacy.android.app.fragments.settingsFragments

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
import mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE
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
import mega.privacy.android.app.constants.SettingsConstants.KEY_SET_QUEUE_DIALOG
import mega.privacy.android.app.constants.SettingsConstants.KEY_SET_QUEUE_SIZE
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_CAMERA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_MEGA_CAMERA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.REQUEST_MEGA_SECONDARY_MEDIA_FOLDER
import mega.privacy.android.app.constants.SettingsConstants.SELECTED_MEGA_FOLDER
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updatePrimaryLocalFolder
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updateSecondaryLocalFolder
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.displayNotificationPermissionRationale
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasAccessMediaLocationPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File

/**
 * [SettingsBaseFragment] that enables or disables Camera Uploads in Settings
 */
@AndroidEntryPoint
class SettingsCameraUploadsFragment : SettingsBaseFragment() {
    private var cameraUploadOnOff: SwitchPreferenceCompat? = null
    private var optionHowToUpload: ListPreference? = null
    private var optionFileUpload: ListPreference? = null
    private var optionIncludeLocationTags: SwitchPreferenceCompat? = null
    private var optionVideoQuality: ListPreference? = null
    private var cameraUploadCharging: SwitchPreferenceCompat? = null
    private var cameraUploadVideoQueueSize: Preference? = null
    private var keepFileNames: TwoLineCheckPreference? = null
    private var localCameraUploadFolder: Preference? = null
    private var megaCameraFolder: Preference? = null
    private var secondaryMediaFolderOn: Preference? = null
    private var localSecondaryFolder: Preference? = null
    private var megaSecondaryFolder: Preference? = null
    private var businessCameraUploadsAlertDialog: AlertDialog? = null
    private var cameraUpload = false
    private var secondaryUpload = false
    private var charging = false
    private var fileNames = false
    private var camSyncLocalPath: String? = ""
    private var isExternalSDCardCU = false
    private var camSyncHandle: Long? = null
    private var camSyncMegaNode: MegaNode? = null
    private var camSyncMegaPath = ""
    private var compressionQueueSizeDialog: AlertDialog? = null
    private var queueSizeInput: EditText? = null
    private var mediaPermissionsDialog: AlertDialog? = null

    // Secondary Folder
    private var localSecondaryFolderPath: String? = ""
    private var handleSecondaryMediaFolder: Long? = null
    private var megaNodeSecondaryMediaFolder: MegaNode? = null
    private var megaPathSecMediaFolder = ""
    private var isExternalSDCardMU = false

    private val localDCIMFolderPath: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
    private val viewModel by viewModels<SettingsCameraUploadsViewModel>()

    /**
     * Register the permissions callback when the user attempts to enable Camera Uploads
     */
    private val enableCameraUploadsPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { permissions ->
            viewModel.handlePermissionsResult(permissions)
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
                viewModel.setAccessMediaLocationRationaleShown(true)
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
     * onSaveInstanceState Behavior
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (compressionQueueSizeDialog != null && (compressionQueueSizeDialog
                ?: return).isShowing
        ) {
            outState.putBoolean(KEY_SET_QUEUE_DIALOG, true)
            outState.putString(KEY_SET_QUEUE_SIZE, queueSizeInput?.text.toString())
        }
    }

    /**
     * onViewCreated Behavior
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    /**
     * In onPause(), the Worker to start Camera Uploads will be fired if the user enables
     * the feature
     */
    override fun onPause() {
        Timber.d("CameraUpload enabled through Settings - fireCameraUploadJob()")
        JobUtil.fireCameraUploadJob(context)

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
                    dbH.setCamSyncTimeStamp(0)
                    cameraUpload = !cameraUpload
                    refreshCameraUploadsSettings()
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

        cameraUploadCharging = findPreference(KEY_CAMERA_UPLOAD_CHARGING)
        cameraUploadCharging?.onPreferenceClickListener = this

        cameraUploadVideoQueueSize = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE)
        cameraUploadVideoQueueSize?.onPreferenceClickListener = this

        keepFileNames = findPreference(KEY_KEEP_FILE_NAMES)
        keepFileNames?.onPreferenceClickListener = this

        localCameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER)
        localCameraUploadFolder?.onPreferenceClickListener = this

        megaCameraFolder = findPreference(KEY_CAMERA_UPLOAD_MEGA_FOLDER)
        megaCameraFolder?.onPreferenceClickListener = this

        secondaryMediaFolderOn = findPreference(KEY_SECONDARY_MEDIA_FOLDER_ON)
        secondaryMediaFolderOn?.let {
            it.isEnabled = true
            it.onPreferenceClickListener = this
        }

        localSecondaryFolder = findPreference(KEY_LOCAL_SECONDARY_MEDIA_FOLDER)
        localSecondaryFolder?.onPreferenceClickListener = this

        megaSecondaryFolder = findPreference(KEY_MEGA_SECONDARY_MEDIA_FOLDER)
        megaSecondaryFolder?.onPreferenceClickListener = this

        if (prefs == null || prefs.camSyncEnabled == null) {
            if (prefs == null) {
                with(dbH) {
                    setFirstTime(false)
                    setSecondaryUploadEnabled(false)
                }
            }
            dbH.setCamSyncEnabled(false)
            cameraUpload = false
            charging = true
            fileNames = false
        } else {
            cameraUpload = prefs.camSyncEnabled.toBoolean()
            if (prefs.cameraFolderExternalSDCard != null) {
                isExternalSDCardCU = prefs.cameraFolderExternalSDCard.toBoolean()
            }
            val tempHandle = prefs.camSyncHandle
            if (tempHandle != null) {
                camSyncHandle = tempHandle.toLongOrNull()
                if (camSyncHandle != MegaApiJava.INVALID_HANDLE) {
                    camSyncMegaNode = camSyncHandle?.let { megaApi.getNodeByHandle(it) }
                    if (camSyncMegaNode != null) {
                        camSyncMegaPath = camSyncMegaNode?.name ?: ""
                    } else {
                        nodeForCameraSyncDoesNotExist()
                    }
                } else {
                    camSyncMegaPath = getString(R.string.section_photo_sync)
                }
            } else {
                nodeForCameraSyncDoesNotExist()
            }
            fileNames = if (prefs.keepFileNames == null) {
                dbH.setKeepFileNames(false)
                false
            } else {
                prefs.keepFileNames.toBoolean()
            }

            camSyncLocalPath = prefs.camSyncLocalPath
            if (camSyncLocalPath.isNullOrBlank() || !isExternalSDCardCU
                && !FileUtil.isFileAvailable(camSyncLocalPath?.let { File(it) })
                && Environment.getExternalStorageDirectory() != null
            ) {
                val cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                )
                with(dbH) {
                    setCamSyncLocalPath(cameraDownloadLocation.absolutePath)
                    setCameraFolderExternalSDCard(false)
                }
                camSyncLocalPath = cameraDownloadLocation.absolutePath
            } else if (isExternalSDCardCU) {
                val uri = Uri.parse(prefs.uriExternalSDCard)
                val pickedDirName = SDCardUtils.getSDCardDirName(uri)
                if (pickedDirName != null) {
                    camSyncLocalPath = pickedDirName
                    localCameraUploadFolder?.summary = pickedDirName
                } else {
                    Timber.w("The Directory Name is Null")
                }
            }
            if (prefs.secondaryMediaFolderEnabled == null) {
                dbH.setSecondaryUploadEnabled(false)
                secondaryUpload = false
            } else {
                secondaryUpload = prefs.secondaryMediaFolderEnabled.toBoolean()
                Timber.d("Secondary is: %s", secondaryUpload)
            }
            isExternalSDCardMU = dbH.mediaFolderExternalSdCard
        }
        if (cameraUpload) {
            Timber.d("Camera Uploads ON")
            cameraUploadOnOff?.isChecked = true
            megaCameraFolder?.summary = camSyncMegaPath
            megaSecondaryFolder?.summary = megaPathSecMediaFolder

            viewModel.setCameraUploadsRunning(true)

            localCameraUploadFolder?.let {
                it.summary = camSyncLocalPath
                preferenceScreen.addPreference(it)
            }
            keepFileNames?.let {
                it.isChecked = fileNames
                preferenceScreen.addPreference(it)
            }

            if (!charging) {
                disableVideoCompressionSizeSettings()
            }
            checkSecondaryMediaFolder()
        } else {
            Timber.d("Camera Uploads Off")
            cameraUploadOnOff?.isChecked = false
            keepFileNames?.let { preferenceScreen.removePreference(it) }
            secondaryMediaFolderOn?.let { preferenceScreen.removePreference(it) }

            viewModel.setCameraUploadsRunning(false)

            localCameraUploadFolder?.let {
                it.summary = ""
                preferenceScreen.removePreference(it)
            }
            megaCameraFolder?.let {
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

        val sizeInDB = prefs.chargingOnSize
        val size: String = if (sizeInDB == null) {
            dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE)
            DEFAULT_CONVENTION_QUEUE_SIZE.toString()
        } else {
            sizeInDB
        }

        val chargingHelper = getString(
            R.string.settings_camera_upload_charging_helper_label,
            getString(R.string.label_file_size_mega_byte, size)
        )
        cameraUploadCharging?.summary = chargingHelper
        if (savedInstanceState != null) {
            val isShowingQueueDialog = savedInstanceState.getBoolean(KEY_SET_QUEUE_DIALOG, false)
            if (isShowingQueueDialog) {
                showResetCompressionQueueSizeDialog()
                val input = savedInstanceState.getString(KEY_SET_QUEUE_SIZE, "")
                queueSizeInput?.let {
                    it.setText(input)
                    it.setSelection(input.length)
                }
            }
        }
    }

    /**
     * onPreferenceClick implementation
     *
     * @param preference The [Preference] object
     */
    @Suppress("DEPRECATION")
    override fun onPreferenceClick(preference: Preference): Boolean {
        if (megaChatApi.isSignalActivityRequired) megaChatApi.signalPresenceActivity()
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
                charging = cameraUploadCharging?.isChecked ?: false
                if (charging) {
                    enableVideoCompressionSizeSettingsAndRestartUpload()
                } else {
                    disableVideoCompressionSizeSettingsAndRestartUpload()
                }
                dbH.setConversionOnCharging(charging)
            }
            KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE -> showResetCompressionQueueSizeDialog()
            KEY_KEEP_FILE_NAMES -> {
                fileNames = keepFileNames?.isChecked ?: false
                dbH.setKeepFileNames(fileNames)
                Toast.makeText(
                    context,
                    getString(R.string.message_keep_device_name),
                    Toast.LENGTH_SHORT
                ).show()
            }
            KEY_CAMERA_UPLOAD_CAMERA_FOLDER -> {
                intent = Intent(context, FileStorageActivity::class.java).apply {
                    action = FileStorageActivity.Mode.PICK_FOLDER.action
                    putExtra(
                        FileStorageActivity.PICK_FOLDER_TYPE,
                        FileStorageActivity.PickFolderType.CU_FOLDER.folderType
                    )
                }
                startActivityForResult(intent, REQUEST_CAMERA_FOLDER)
            }
            KEY_CAMERA_UPLOAD_MEGA_FOLDER -> {
                if (viewModel.isConnected.not()) return false
                intent = Intent(context, FileExplorerActivity::class.java).apply {
                    action = FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC
                }
                startActivityForResult(intent, REQUEST_MEGA_CAMERA_FOLDER)
            }
            KEY_SECONDARY_MEDIA_FOLDER_ON -> {
                if (viewModel.isConnected.not()) return false
                secondaryUpload = !secondaryUpload
                if (secondaryUpload) {
                    Timber.d("Enable Media Uploads.")
                    // If there is any possible secondary folder, set it as the default one
                    viewModel.setupDefaultSecondaryCameraUploadFolder(getString(R.string.section_secondary_media_uploads))
                    viewModel.restoreSecondaryTimestampsAndSyncRecordProcess()
                    dbH.setSecondaryUploadEnabled(true)

                    // To prevent user switch on/off rapidly. After set backup, will be re-enabled.
                    secondaryMediaFolderOn?.isEnabled = false
                    localSecondaryFolder?.isEnabled = false
                    megaSecondaryFolder?.isEnabled = false
                    if (handleSecondaryMediaFolder == null || handleSecondaryMediaFolder == MegaApiJava.INVALID_HANDLE) {
                        megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads)
                    }
                    prefs = dbH.preferences
                    checkMediaUploadsPath()
                } else {
                    Timber.d("Disable Media Uploads.")
                    viewModel.disableMediaUploads()
                }
                checkIfSecondaryFolderExists()
                JobUtil.rescheduleCameraUpload(context)
            }
            KEY_LOCAL_SECONDARY_MEDIA_FOLDER -> {
                intent = Intent(context, FileStorageActivity::class.java).apply {
                    action = FileStorageActivity.Mode.PICK_FOLDER.action
                    putExtra(
                        FileStorageActivity.PICK_FOLDER_TYPE,
                        FileStorageActivity.PickFolderType.MU_FOLDER.folderType
                    )
                }
                startActivityForResult(intent, REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER)
            }
            KEY_MEGA_SECONDARY_MEDIA_FOLDER -> {
                if (viewModel.isConnected.not()) return false
                intent = Intent(context, FileExplorerActivity::class.java).apply {
                    action = FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC
                }
                startActivityForResult(intent, REQUEST_MEGA_SECONDARY_MEDIA_FOLDER)
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
        prefs = dbH.preferences
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
                JobUtil.rescheduleCameraUpload(context)
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
                viewModel.resetTimestampsAndCacheDirectory()
                JobUtil.rescheduleCameraUpload(context)
            }
            KEY_CAMERA_UPLOAD_VIDEO_QUALITY -> {
                viewModel.changeUploadVideoQuality(value)
                JobUtil.rescheduleCameraUpload(context)
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
        prefs = dbH.preferences
        Timber.d("REQUEST CODE: %d___RESULT CODE: %d", requestCode, resultCode)
        when (requestCode) {
            REQUEST_CAMERA_FOLDER -> {
                val cameraPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH)
                if (!isNewSettingValid(
                        cameraPath,
                        prefs.localPathSecondaryFolder,
                        prefs.camSyncHandle,
                        prefs.megaHandleSecondaryFolder
                    )
                ) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_invalid_folder_selected),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                isExternalSDCardCU = SDCardUtils.isLocalFolderOnSDCard(
                    context,
                    cameraPath
                ) && !FileUtil.isBasedOnFileStorage()
                dbH.setCameraFolderExternalSDCard(isExternalSDCardCU)
                camSyncLocalPath =
                    if (isExternalSDCardCU) SDCardUtils.getSDCardDirName(Uri.parse(prefs.uriExternalSDCard)) else cameraPath
                prefs.camSyncLocalPath = camSyncLocalPath
                camSyncLocalPath?.let {
                    dbH.setCamSyncLocalPath(it)
                    localCameraUploadFolder?.summary = it
                }
                viewModel.resetTimestampsAndCacheDirectory()
                JobUtil.rescheduleCameraUpload(context)

                // Update Sync when the Primary Local Folder has changed
                updatePrimaryLocalFolder(cameraPath)
            }
            REQUEST_MEGA_CAMERA_FOLDER -> {
                // Primary Folder to Sync
                val handle = intent.getLongExtra(SELECTED_MEGA_FOLDER, MegaApiJava.INVALID_HANDLE)
                if (!isNewSettingValid(
                        prefs.camSyncLocalPath,
                        prefs.localPathSecondaryFolder,
                        handle.toString(),
                        prefs.megaHandleSecondaryFolder
                    )
                ) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_invalid_folder_selected),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                if (handle != MegaApiJava.INVALID_HANDLE) {
                    // Set Primary Folder only
                    Timber.d("Set Camera Uploads Primary Attribute: %s", handle)
                    viewModel.setupPrimaryCameraUploadFolder(handle)
                } else {
                    Timber.e("Error choosing the Mega folder for Primary Uploads")
                }
            }
            REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER -> {
                // Secondary Folder to Sync
                val secondaryPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH)
                if (!isNewSettingValid(
                        prefs.camSyncLocalPath,
                        secondaryPath,
                        prefs.camSyncHandle,
                        prefs.megaHandleSecondaryFolder
                    )
                ) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_invalid_folder_selected),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                isExternalSDCardMU = SDCardUtils.isLocalFolderOnSDCard(
                    context,
                    secondaryPath
                ) && !FileUtil.isBasedOnFileStorage()
                dbH.mediaFolderExternalSdCard = isExternalSDCardMU
                localSecondaryFolderPath =
                    if (isExternalSDCardMU) SDCardUtils.getSDCardDirName(Uri.parse(dbH.uriMediaExternalSdCard)) else secondaryPath
                dbH.setSecondaryFolderPath(localSecondaryFolderPath.orEmpty())
                prefs.localPathSecondaryFolder = localSecondaryFolderPath.orEmpty()
                localSecondaryFolder?.summary = localSecondaryFolderPath.orEmpty()
                dbH.setSecSyncTimeStamp(0)
                dbH.setSecVideoSyncTimeStamp(0)
                JobUtil.rescheduleCameraUpload(context)

                // Update Sync when the Secondary Local Folder has changed
                updateSecondaryLocalFolder(secondaryPath)
            }
            REQUEST_MEGA_SECONDARY_MEDIA_FOLDER -> {
                // Secondary Folder to Sync
                val secondaryHandle =
                    intent.getLongExtra(SELECTED_MEGA_FOLDER, MegaApiJava.INVALID_HANDLE)
                if (!isNewSettingValid(
                        prefs.camSyncLocalPath,
                        prefs.localPathSecondaryFolder,
                        prefs.camSyncHandle,
                        secondaryHandle.toString()
                    )
                ) {
                    Toast.makeText(
                        context,
                        getString(R.string.error_invalid_folder_selected),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                if (secondaryHandle != MegaApiJava.INVALID_HANDLE) {
                    Timber.d("Set Camera Uploads Secondary Attribute: %s", secondaryHandle)
                    viewModel.setupSecondaryCameraUploadFolder(secondaryHandle)
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
                handleFileUpload(
                    isCameraUploadsRunning = it.isCameraUploadsRunning,
                    uploadOption = it.uploadOption,
                )
                handleHowToUpload(
                    isCameraUploadsRunning = it.isCameraUploadsRunning,
                    uploadConnectionType = it.uploadConnectionType,
                )
                handleIncludeLocationTags(
                    areLocationTagsIncluded = it.areLocationTagsIncluded,
                    isCameraUploadsRunning = it.isCameraUploadsRunning,
                    uploadOption = it.uploadOption,
                )
                handleUploadVideoQuality(
                    isCameraUploadsRunning = it.isCameraUploadsRunning,
                    uploadOption = it.uploadOption,
                    videoQuality = it.videoQuality,
                )
                handleChargingSettings(
                    isCameraUploadsRunning = it.isCameraUploadsRunning,
                    uploadOption = it.uploadOption,
                    videoQuality = it.videoQuality,
                )
                handleBusinessAccountPrompt(it.shouldShowBusinessAccountPrompt)
                handleBusinessAccountSuspendedPrompt(it.shouldShowBusinessAccountSuspendedPrompt)
                handleTriggerCameraUploads(it.shouldTriggerCameraUploads)
                handleMediaPermissionsRationale(it.shouldShowMediaPermissionsRationale)
                handleNotificationPermissionRationale(it.shouldShowNotificationPermissionRationale)
                handleAccessMediaLocationPermissionRationale(it.accessMediaLocationRationaleText)
            }
        }
    }

    /**
     * Handles the "File upload" option visibility and content when a UI State change happens
     *
     * @param isCameraUploadsRunning Whether Camera Uploads is running or not
     * @param uploadOption the specific [UploadOption] which can be nullable
     */
    private fun handleFileUpload(
        isCameraUploadsRunning: Boolean,
        uploadOption: UploadOption?,
    ) {
        optionFileUpload?.run {
            summary = if (isCameraUploadsRunning && uploadOption != null) {
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
     * @param isCameraUploadsRunning Whether Camera Uploads is running or not
     * @param uploadConnectionType the specific [UploadConnectionType] which can be nullable
     */
    private fun handleHowToUpload(
        isCameraUploadsRunning: Boolean,
        uploadConnectionType: UploadConnectionType?,
    ) {
        optionHowToUpload?.run {
            if (isCameraUploadsRunning && uploadConnectionType != null) {
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
     * @param isCameraUploadsRunning Whether Camera Uploads is running or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     */
    private fun handleIncludeLocationTags(
        areLocationTagsIncluded: Boolean,
        isCameraUploadsRunning: Boolean,
        uploadOption: UploadOption?,
    ) {
        val allowedOptions = listOf(UploadOption.PHOTOS, UploadOption.PHOTOS_AND_VIDEOS)

        optionIncludeLocationTags?.run {
            if (isCameraUploadsRunning && allowedOptions.contains(uploadOption)) {
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
     * @param isCameraUploadsRunning Whether Camera Uploads is running or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     * @param videoQuality The specific [VideoQuality] which can be nullable
     */
    private fun handleUploadVideoQuality(
        isCameraUploadsRunning: Boolean,
        uploadOption: UploadOption?,
        videoQuality: VideoQuality?,
    ) {
        optionVideoQuality?.run {
            summary = if (isCameraUploadsRunning && videoQuality != null && uploadOption in listOf(
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
     * Handles the display and content of the Charging Settings options when a UI State change happens
     *
     * @param isCameraUploadsRunning Whether Camera Uploads is running or not
     * @param uploadOption The specific [UploadOption] which can be nullable
     * @param videoQuality The specific [VideoQuality] which can be nullable
     */
    private fun handleChargingSettings(
        isCameraUploadsRunning: Boolean,
        uploadOption: UploadOption?,
        videoQuality: VideoQuality?,
    ) {
        if (isCameraUploadsRunning && uploadOption in listOf(
                UploadOption.VIDEOS,
                UploadOption.PHOTOS_AND_VIDEOS,
            ) && videoQuality != null && videoQuality != VideoQuality.ORIGINAL
        ) {
            enableChargingSettings()
        } else {
            disableChargingSettings()
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
                    viewModel.setTriggerCameraUploadsState(true)
                }
                .setCancelable(false)
                // Reset the Business Account Prompt State when the Dialog is dismissed
                .setOnDismissListener { viewModel.resetBusinessAccountPromptState() }
            businessCameraUploadsAlertDialog = builder.create()
            businessCameraUploadsAlertDialog?.show()
        }
    }

    /**
     * Handle the Business Account Suspended prompt when a UI State change happens
     *
     * @param showPrompt If true, send a broadcast to display the Dialog informing
     * that the current Business Account has expired
     */
    private fun handleBusinessAccountSuspendedPrompt(showPrompt: Boolean) {
        if (showPrompt) {
            requireContext().sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            // After sending the broadcast, reset the Business Account suspended prompt state
            viewModel.resetBusinessAccountSuspendedPromptState()
        }
    }

    /**
     * Handle the Camera Uploads trigger when a UI State change happens
     *
     * @param shouldTrigger If true, enable Camera Uploads
     */
    private fun handleTriggerCameraUploads(shouldTrigger: Boolean) {
        if (shouldTrigger) {
            enableCameraUploads()
            // After enabling Camera Uploads, reset the Trigger Camera Uploads state
            viewModel.setTriggerCameraUploadsState(false)
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
                        enableCameraUploadsPermissionLauncher.launch(
                            arrayOf(
                                getImagePermissionByVersion(),
                                getVideoPermissionByVersion(),
                            )
                        )
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

    /**
     * Handle the display of the Notification Permission rationale when a UI State change happens
     *
     * @param showRationale If true, display the Notification Permission rationale
     */
    private fun handleNotificationPermissionRationale(showRationale: Boolean) {
        if (showRationale) {
            if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                displayNotificationPermissionRationale(requireActivity())
            }
            // Once the rationale has been shown, reset the Notification Permission Rationale state
            viewModel.setNotificationPermissionRationaleState(shouldShow = false)
        }
    }

    /**
     * Handle the display of the Access Media Location Permission rationale when a UI State change happens
     *
     * @param accessMediaLocationRationaleText A [StringRes] message to be displayed, which can be nullable
     */
    private fun handleAccessMediaLocationPermissionRationale(@StringRes accessMediaLocationRationaleText: Int?) {
        if (accessMediaLocationRationaleText != null) {
            view?.let {
                Snackbar.make(
                    it,
                    getString(accessMediaLocationRationaleText),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            // Once the Rationale has been shown, notify the ViewModel
            viewModel.setAccessMediaLocationRationaleShown(false)
        }
    }

    /**
     * Includes or excludes adding Location tags to Photo uploads. This also reschedules
     * Camera Uploads
     *
     * @param include true if Location data should be added to Photos, and false if otherwise
     */
    private fun includeLocationTags(include: Boolean) {
        viewModel.includeLocationTags(include)
        JobUtil.rescheduleCameraUpload(context)
    }

    /**
     * Checks whether a rationale is needed for Media Permissions
     *
     * @return Boolean value
     */
    private fun shouldShowMediaPermissionsRationale() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            )
        } else {
            shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)
        }

    /**
     * Checks the Secondary Folder
     */
    private fun checkSecondaryMediaFolder() {
        if (secondaryUpload) {
            // Check if the node exists in MEGA
            checkIfNodeOfSecondaryFolderExistsInMega()

            // Check if the local secondary folder exists
            checkMediaUploadsPath()
        }
        checkIfSecondaryFolderExists()
    }

    /**
     * Check if the Secondary Folder exists
     */
    private fun checkIfSecondaryFolderExists() {
        if (secondaryUpload) {
            secondaryMediaFolderOn?.title = getString(R.string.settings_secondary_upload_off)
            megaSecondaryFolder?.let {
                it.summary = megaPathSecMediaFolder
                preferenceScreen.addPreference(it)
            }
            localSecondaryFolder?.let {
                it.summary = localSecondaryFolderPath.orEmpty()
                preferenceScreen.addPreference(it)
            }
        } else {
            secondaryMediaFolderOn?.title = getString(R.string.settings_secondary_upload_on)
            megaSecondaryFolder?.let { preferenceScreen.removePreference(it) }
            localSecondaryFolder?.let { preferenceScreen.removePreference(it) }
        }
    }

    /**
     * Method to control the changes needed when the node for CameraSync doesn't exist.
     */
    private fun nodeForCameraSyncDoesNotExist() {
        dbH.setCamSyncHandle(Constants.INVALID_NON_NULL_VALUE.toLong())
        camSyncHandle = Constants.INVALID_NON_NULL_VALUE.toLong()
        camSyncMegaPath = getString(R.string.section_photo_sync)
    }

    /**
     * Setup the Destination Folder of either Primary or Secondary Uploads
     *
     * @param isSecondary Set "true" if the Destination Folder is for Secondary Uploads, and
     * "false" if set for Primary Uploads
     */
    @Synchronized
    fun setCUDestinationFolder(isSecondary: Boolean, handle: Long) {
        val targetNode = megaApi.getNodeByHandle(handle) ?: return
        if (isSecondary) {
            // Reset Secondary Timeline
            handleSecondaryMediaFolder = handle
            megaNodeSecondaryMediaFolder = targetNode
            megaPathSecMediaFolder = megaNodeSecondaryMediaFolder?.name.orEmpty()
            megaSecondaryFolder?.summary = megaPathSecMediaFolder
        } else {
            // Reset Primary Timeline
            camSyncHandle = handle
            camSyncMegaNode = targetNode
            camSyncMegaPath = camSyncMegaNode?.name.orEmpty()
            megaCameraFolder?.summary = camSyncMegaPath
        }
    }

    /**
     * Setup the Primary Cloud Folder
     */
    private fun setupPrimaryCloudFolder() {
        if (camSyncHandle == null || camSyncHandle == MegaApiJava.INVALID_HANDLE) {
            camSyncMegaPath = getString(R.string.section_photo_sync)
        }
        megaCameraFolder?.summary = camSyncMegaPath
    }

    /**
     * Refresh the Camera Uploads service settings depending on the service status
     */
    fun refreshCameraUploadsSettings() {
        var cuEnabled = false
        prefs = dbH.preferences
        if (prefs != null) {
            cuEnabled = prefs.camSyncEnabled.toBoolean()
        }
        if (cuEnabled) {
            Timber.d("Disable CU.")
            disableCameraUpload()
        } else {
            // Check if the necessary permissions to enable Camera Uploads have been granted.
            // The permissions are adaptive depending on the Android SDK version
            Timber.d("Checking if the necessary permissions have been granted")
            val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            if (hasPermissions(context, *permissionsList)) {
                Timber.d("All necessary permissions have been granted")
                viewModel.handleEnableCameraUploads()
            } else {
                Timber.d("At least one permission was denied. Launching permission window")
                enableCameraUploadsPermissionLauncher.launch(permissionsList)
            }
        }
    }

    /**
     * This method is to do the setting process and UI related process.
     * It also cancels all CameraUpload and Heartbeat workers.
     */
    fun disableCameraUpload() {
        getContext()?.let {
            JobUtil.fireStopCameraUploadJob(it)
            JobUtil.stopCameraUploadSyncHeartbeatWorkers(it)
        }
        viewModel.disableCameraUploadsInDB()
        disableCameraUploadUIProcess()
    }

    /**
     * Checks the Media Uploads local path
     */
    private fun checkMediaUploadsPath() {
        localSecondaryFolderPath = prefs.localPathSecondaryFolder
        if (localSecondaryFolderPath.isNullOrBlank()
            || localSecondaryFolderPath == Constants.INVALID_NON_NULL_VALUE
            || (!isExternalSDCardMU && !FileUtil.isFileAvailable(File(localSecondaryFolderPath.orEmpty())))
        ) {
            Timber.w("Secondary ON: invalid localSecondaryFolderPath")
            localSecondaryFolderPath = getString(R.string.settings_empty_folder)
            Toast.makeText(
                context,
                getString(R.string.secondary_media_service_error_local_folder),
                Toast.LENGTH_SHORT
            ).show()
            if (!FileUtil.isFileAvailable(File(localSecondaryFolderPath.orEmpty()))) {
                dbH.setSecondaryFolderPath(Constants.INVALID_NON_NULL_VALUE)
            }
        } else if (isExternalSDCardMU) {
            val uri = Uri.parse(dbH.uriMediaExternalSdCard)
            val pickedDirName = SDCardUtils.getSDCardDirName(uri)
            if (pickedDirName != null) {
                localSecondaryFolderPath = pickedDirName
            } else {
                localSecondaryFolderPath = getString(R.string.settings_empty_folder)
                dbH.setSecondaryFolderPath(Constants.INVALID_NON_NULL_VALUE)
            }
        }
    }

    /**
     * Set Compression Queue Size
     *
     * @param value the Compression Value
     * @param input the [EditText]
     */
    private fun setCompressionQueueSize(value: String, input: EditText) {
        if (value.isEmpty()) {
            compressionQueueSizeDialog?.dismiss()
            return
        }
        try {
            val size = value.toInt()
            if (isQueueSizeValid(size)) {
                compressionQueueSizeDialog?.dismiss()
                cameraUploadVideoQueueSize?.summary =
                    getString(R.string.label_file_size_mega_byte, size.toString())
                val chargingHelper =
                    getString(
                        R.string.settings_camera_upload_charging_helper_label,
                        getString(R.string.label_file_size_mega_byte, size.toString())
                    )
                cameraUploadCharging?.summary = chargingHelper
                dbH.setChargingOnSize(size)
                prefs.chargingOnSize = size.toString() + ""
                JobUtil.rescheduleCameraUpload(context)
            } else {
                resetSizeInput(input)
            }
        } catch (e: Exception) {
            Timber.e(e)
            resetSizeInput(input)
        }
    }

    /**
     * Is Queue Size Valid
     *
     * @param size The Queue size
     */
    private fun isQueueSizeValid(size: Int): Boolean =
        size in COMPRESSION_QUEUE_SIZE_MIN..COMPRESSION_QUEUE_SIZE_MAX

    /**
     * Reset Size Input
     *
     * @param input the [EditText`]
     */
    private fun resetSizeInput(input: EditText) = with(input) {
        setText("")
        requestFocus()
    }

    /**
     * Show Reset Compression Queue Size Dialog
     */
    @Suppress("DEPRECATION")
    fun showResetCompressionQueueSizeDialog() {
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

        queueSizeInput = EditText(context)
        queueSizeInput?.let { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            layout.addView(editText, params)
            editText.setSingleLine()
            editText.setTextColor(getThemeColor(context, android.R.attr.textColorSecondary))
            editText.hint = getString(R.string.label_mega_byte)
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val value = textView.text.toString().trim { it <= ' ' }
                    setCompressionQueueSize(value, editText)
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

        val builder = AlertDialog.Builder(context)
        with(builder) {
            setTitle(getString(R.string.settings_video_compression_queue_size_popup_title))
            setPositiveButton(getString(R.string.general_ok), null)
            setNegativeButton(getString(android.R.string.cancel), null)
            setView(layout)
        }

        compressionQueueSizeDialog = builder.create()
        compressionQueueSizeDialog?.let { alertDialog ->
            alertDialog.show()
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                queueSizeInput?.let { editText ->
                    val value = editText.text.toString().trim { it <= ' ' }
                    setCompressionQueueSize(value, editText)
                }
            }
        }
    }

    /**
     * Disable Charging Settings
     */
    private fun disableChargingSettings() {
        charging = false
        dbH.setConversionOnCharging(false)

        cameraUploadCharging?.let {
            it.isChecked = charging
            preferenceScreen.removePreference(it)
        }

        disableVideoCompressionSizeSettings()
    }

    /**
     * Disable Video Compression Size Settings
     */
    private fun disableVideoCompressionSizeSettings() =
        cameraUploadVideoQueueSize?.let { preferenceScreen.removePreference(it) }

    /**
     * Disable Video Compression Size Settings and Restart the Camera Uploads process
     */
    private fun disableVideoCompressionSizeSettingsAndRestartUpload() {
        disableVideoCompressionSizeSettings()
        JobUtil.rescheduleCameraUpload(context)
    }

    /**
     * Enable Video Compression Size Settings and Restart the Camera Uploads process
     */
    private fun enableVideoCompressionSizeSettingsAndRestartUpload() {
        enableVideoCompressionSizeSettings()
        JobUtil.rescheduleCameraUpload(context)
    }

    /**
     * Enable Video Compression Size Settings
     */
    private fun enableVideoCompressionSizeSettings() {
        prefs = dbH.preferences
        val sizeInDB = prefs.chargingOnSize
        val size = if (sizeInDB == null) {
            dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE)
            DEFAULT_CONVENTION_QUEUE_SIZE
        } else {
            sizeInDB.toInt()
        }

        cameraUploadVideoQueueSize?.run {
            preferenceScreen.addPreference(this)
            summary = getString(R.string.label_file_size_mega_byte, size.toString())
        }
    }

    /**
     * Enable Charging Settings
     */
    private fun enableChargingSettings() {
        prefs = dbH.preferences
        charging = if (prefs.conversionOnCharging == null) {
            dbH.setConversionOnCharging(true)
            true
        } else {
            prefs.conversionOnCharging.toBoolean()
        }
        cameraUploadCharging?.run {
            preferenceScreen.addPreference(this)
            isChecked = charging
        }

        if (charging) {
            enableVideoCompressionSizeSettings()
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
     * Enables the Camera Uploads functionality
     */
    private fun enableCameraUploads() {
        Timber.d("Camera Uploads Enabled")

        cameraUpload = true
        prefs = dbH.preferences

        viewModel.setCameraUploadsRunning(true)

        // Local Primary Folder
        setupLocalPathForCameraUpload()
        viewModel.restorePrimaryTimestampsAndSyncRecordProcess()

        // Cloud Primary Folder
        setupPrimaryCloudFolder()

        // Secondary Uploads
        setupSecondaryUpload()

        // Set Camera Uploads as Enabled
        dbH.setCamSyncEnabled(true)

        cameraUploadOnOff?.isChecked = true
        keepFileNames?.let { preferenceScreen.addPreference(it) }
        megaCameraFolder?.let { preferenceScreen.addPreference(it) }
        secondaryMediaFolderOn?.let { preferenceScreen.addPreference(it) }

        // Configured to prevent Camera Uploads from rapidly being switched on/off.
        // After setting the Backup, it will be re-enabled
        cameraUploadOnOff?.isEnabled = false
        localCameraUploadFolder?.isEnabled = false
        megaCameraFolder?.isEnabled = false
    }

    /**
     * Disable Media Uploads UI-related process
     */
    fun disableMediaUploadUIProcess() {
        Timber.d("Changes applied to Secondary Folder Only")
        secondaryUpload = false
        secondaryMediaFolderOn?.title =
            getString(R.string.settings_secondary_upload_on)

        localSecondaryFolder?.let { preferenceScreen.removePreference(it) }
        megaSecondaryFolder?.let { preferenceScreen.removePreference(it) }
    }

    /**
     * Disable Camera Uploads UI-related process
     */
    fun disableCameraUploadUIProcess() {
        Timber.d("Camera Uploads Disabled")
        cameraUpload = false
        cameraUploadOnOff?.isChecked = false

        viewModel.setCameraUploadsRunning(false)

        localCameraUploadFolder?.let { preferenceScreen.removePreference(it) }
        keepFileNames?.let { preferenceScreen.removePreference(it) }
        megaCameraFolder?.let { preferenceScreen.removePreference(it) }
        secondaryMediaFolderOn?.let { preferenceScreen.removePreference(it) }

        disableMediaUploadUIProcess()
    }

    /**
     * Sets up the Local Path for Camera Uploads
     */
    private fun setupLocalPathForCameraUpload() {
        var cameraFolderLocation = prefs.camSyncLocalPath
        if (cameraFolderLocation.isNullOrBlank()) {
            cameraFolderLocation = localDCIMFolderPath
        }

        if (camSyncLocalPath != null) {
            if (isExternalSDCardCU) {
                val uri = Uri.parse(prefs.uriExternalSDCard)
                val pickedDirName = SDCardUtils.getSDCardDirName(uri)
                if (pickedDirName != null) {
                    camSyncLocalPath = pickedDirName
                } else {
                    Timber.e("pickedDirName is null")
                }
            } else {
                val checkFile = camSyncLocalPath?.let { File(it) }
                if (checkFile != null && checkFile.exists()) {
                    Timber.w("Local path does not exist. Use the default Camera Uploads folder path instead")
                    camSyncLocalPath = cameraFolderLocation
                }
            }
        } else {
            Timber.e("Local path is null")
            dbH.setCameraFolderExternalSDCard(false)
            isExternalSDCardCU = false
            camSyncLocalPath = cameraFolderLocation
        }

        dbH.setCamSyncLocalPath(cameraFolderLocation)
        localCameraUploadFolder?.let {
            it.summary = camSyncLocalPath
            preferenceScreen.addPreference(it)
        }
    }

    /**
     * Checks if the Secondary Folder Node exists in MEGA
     */
    private fun checkIfNodeOfSecondaryFolderExistsInMega() {
        val secHandle = prefs.megaHandleSecondaryFolder
        megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads)
        if (secHandle != null) {
            if (!secHandle.isNullOrBlank()) {
                handleSecondaryMediaFolder = secHandle.toLong()

                handleSecondaryMediaFolder?.let { longValue ->
                    if (handleSecondaryMediaFolder != MegaApiJava.INVALID_HANDLE) {
                        megaNodeSecondaryMediaFolder = megaApi.getNodeByHandle(longValue)
                        megaPathSecMediaFolder = if (megaNodeSecondaryMediaFolder != null) {
                            megaNodeSecondaryMediaFolder?.name.orEmpty()
                        } else {
                            getString(R.string.section_secondary_media_uploads)
                        }
                    }
                }
            }
        } else {
            dbH.setSecondaryFolderHandle(MegaApiJava.INVALID_HANDLE)
            handleSecondaryMediaFolder = MegaApiJava.INVALID_HANDLE
        }
    }

    /**
     * Setup Secondary Uploads
     */
    private fun setupSecondaryUpload() {
        secondaryUpload = if (prefs.secondaryMediaFolderEnabled == null) {
            dbH.setSecondaryUploadEnabled(false)
            false
        } else {
            prefs.secondaryMediaFolderEnabled.toBoolean()
        }
        checkSecondaryMediaFolder()
    }

    /**
     * Is New Settings Valid
     *
     * @param primaryPath Defines the Primary Folder path
     * @param secondaryPath Defines the Secondary Folder path
     * @param primaryHandle Defines the Primary Folder handle
     * @param secondaryHandle Defines the Secondary Folder handle
     */
    private fun isNewSettingValid(
        primaryPath: String?,
        secondaryPath: String?,
        primaryHandle: String?,
        secondaryHandle: String?,
    ): Boolean {
        return if (!secondaryUpload || primaryPath == null || primaryHandle == null
            || secondaryPath == null || secondaryHandle == null
        ) {
            true
        } else {
            primaryHandle != secondaryHandle || !primaryPath.contains(secondaryPath)
                    && !secondaryPath.contains(primaryPath)
        }
    }

    /**
     * Re-enables the Preferences
     *
     * @param which [MegaApiJava] that specifies which part of Camera Uploads is enabled
     */
    fun reEnableCameraUploadsPreference(which: Int) {
        when (which) {
            MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS -> {
                cameraUploadOnOff?.isEnabled = true
                localCameraUploadFolder?.isEnabled = true
                megaCameraFolder?.isEnabled = true
                secondaryMediaFolderOn?.isEnabled = true
                localSecondaryFolder?.isEnabled = true
                megaSecondaryFolder?.isEnabled = true
            }
            MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS -> {
                secondaryMediaFolderOn?.isEnabled = true
                localSecondaryFolder?.isEnabled = true
                megaSecondaryFolder?.isEnabled = true
            }
        }
    }
}
