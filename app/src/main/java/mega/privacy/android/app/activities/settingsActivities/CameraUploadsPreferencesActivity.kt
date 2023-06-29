package mega.privacy.android.app.activities.settingsActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_UI_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_REENABLE_CU_PREFERENCE
import mega.privacy.android.app.constants.BroadcastConstants.KEY_REENABLE_WHICH_PREFERENCE
import mega.privacy.android.app.fragments.settingsFragments.SettingsCameraUploadsFragment
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED
import mega.privacy.android.data.facade.BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE
import mega.privacy.android.data.facade.BROADCAST_ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING
import mega.privacy.android.data.facade.INTENT_EXTRA_CU_DESTINATION_HANDLE_TO_CHANGE
import mega.privacy.android.data.facade.INTENT_EXTRA_IS_CU_DESTINATION_SECONDARY
import mega.privacy.android.data.facade.INTENT_EXTRA_IS_CU_SECONDARY_FOLDER
import mega.privacy.android.data.facade.INTENT_EXTRA_NODE_HANDLE
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * Settings Activity class for Camera Uploads that holds [SettingsCameraUploadsFragment]
 */
class CameraUploadsPreferencesActivity : PreferencesBaseActivity() {

    private var settingsFragment: SettingsCameraUploadsFragment? = null

    private val updateCameraUploadsSettingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null || settingsFragment == null) {
                return
            }

            settingsFragment?.let {
                when (intent.action) {
                    ACTION_REFRESH_CAMERA_UPLOADS_SETTING -> {
                        Timber.d("Refresh Camera Uploads Settings Event Received")
                        it.refreshCameraUploadsSettings()
                    }
                    ACTION_DISABLE_MEDIA_UPLOADS_SETTING -> {
                        Timber.d("Disable Media Uploads UI Event Received")
                        it.disableMediaUploadUIProcess()
                    }
                    else -> Unit
                }
            }
        }
    }

    private val enableDisableCameraUploadsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null || settingsFragment == null) {
                return
            }

            settingsFragment?.let {
                when (intent.action) {
                    ACTION_UPDATE_DISABLE_CU_SETTING -> {
                        Timber.d("Disable Camera Uploads Event Received")
                        it.disableCameraUploads()
                    }
                    ACTION_UPDATE_DISABLE_CU_UI_SETTING -> {
                        Timber.d("Disable Camera Uploads UI Event Received")
                        it.disableCameraUploadUIProcess()
                    }
                    else -> Unit
                }
            }
        }
    }

    private val cameraUploadsDestinationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null || settingsFragment == null) {
                return
            }

            if (intent.action.equals(BROADCAST_ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING)) {
                Timber.d("Update Camera Uploads Destination Folder Setting Event Received")
                val isSecondaryFolder = intent.getBooleanExtra(
                    INTENT_EXTRA_IS_CU_DESTINATION_SECONDARY, false
                )
                val handleToChange = intent.getLongExtra(
                    INTENT_EXTRA_CU_DESTINATION_HANDLE_TO_CHANGE,
                    INVALID_HANDLE
                )
                settingsFragment?.setCUDestinationFolder(isSecondaryFolder, handleToChange)
            }
        }
    }

    private val receiverCameraUploadsAttrChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null || settingsFragment == null) {
                return
            }

            Timber.d("Receiver Camera Uploads Attribute Changed Event Received")
            setCUDestinationFolderSynchronized(intent)
        }
    }

    /**
     * Sets the Camera Uploads destination folder when the receiver Camera Uploads
     * attribute has changed in a synchronized manner
     *
     * @param intent The Intent
     */
    @Synchronized
    private fun setCUDestinationFolderSynchronized(intent: Intent) {
        val handleInUserAttr = intent.getLongExtra(INTENT_EXTRA_NODE_HANDLE, INVALID_HANDLE)
        val isSecondaryFolder = intent.getBooleanExtra(INTENT_EXTRA_IS_CU_SECONDARY_FOLDER, false)
        settingsFragment?.setCUDestinationFolder(isSecondaryFolder, handleInUserAttr)
    }

    private val reEnableCameraUploadsPreferenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action.equals(BROADCAST_ACTION_REENABLE_CU_PREFERENCE) &&
                settingsFragment != null
            ) {
                Timber.d("Re-Enable Camera Uploads Preference Event Received")
                settingsFragment?.reEnableCameraUploadsPreference(
                    intent.getIntExtra(
                        KEY_REENABLE_WHICH_PREFERENCE, 0
                    )
                )
            }
        }
    }

    /**
     * Set up the [BroadcastReceiver]s on Activity creation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK(true)) return

        setTitle(R.string.section_photo_sync)
        settingsFragment = SettingsCameraUploadsFragment().also {
            replaceFragment(it)
        }

        registerReceiver(
            cameraUploadsDestinationReceiver,
            IntentFilter(BROADCAST_ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING)
        )
        registerReceiver(
            receiverCameraUploadsAttrChanged,
            IntentFilter(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE)
        )
        registerReceiver(
            reEnableCameraUploadsPreferenceReceiver, IntentFilter(
                BROADCAST_ACTION_REENABLE_CU_PREFERENCE
            )
        )

        val filterCUMUSettings = IntentFilter(ACTION_UPDATE_DISABLE_CU_SETTING)
        filterCUMUSettings.addAction(ACTION_UPDATE_DISABLE_CU_UI_SETTING)
        registerReceiver(enableDisableCameraUploadsReceiver, filterCUMUSettings)

        val filterUpdateCUSettings = IntentFilter(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED)
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CAMERA_UPLOADS_SETTING)
        filterUpdateCUSettings.addAction(ACTION_DISABLE_MEDIA_UPLOADS_SETTING)
        registerReceiver(updateCameraUploadsSettingsReceiver, filterUpdateCUSettings)
    }

    /**
     * Unregister all [BroadcastReceiver]s when the Activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(cameraUploadsDestinationReceiver)
        unregisterReceiver(enableDisableCameraUploadsReceiver)
        unregisterReceiver(updateCameraUploadsSettingsReceiver)
        unregisterReceiver(receiverCameraUploadsAttrChanged)
        unregisterReceiver(reEnableCameraUploadsPreferenceReceiver)
    }
}
