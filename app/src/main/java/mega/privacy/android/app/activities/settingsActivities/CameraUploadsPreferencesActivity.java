package mega.privacy.android.app.activities.settingsActivities;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_UI_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_REENABLE_CU_PREFERENCE;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_IS_CU_SECONDARY_FOLDER;
import static mega.privacy.android.app.constants.BroadcastConstants.KEY_REENABLE_WHICH_PREFERENCE;
import static mega.privacy.android.app.constants.BroadcastConstants.PRIMARY_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.SECONDARY_FOLDER;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED;
import static mega.privacy.android.app.utils.Constants.EXTRA_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.GO_OFFLINE;
import static mega.privacy.android.app.utils.Constants.GO_ONLINE;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.Constants.REQUEST_ACCESS_MEDIA_LOCATION;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_UPLOAD;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsCameraUploadsFragment;
import mega.privacy.android.app.utils.Util;

public class CameraUploadsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsCameraUploadsFragment settingsFragment;
    private AlertDialog businessCUAlert;

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received!");
            if (intent == null || intent.getAction() == null || settingsFragment == null)
                return;

            int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);

            if (actionType == GO_OFFLINE) {
                settingsFragment.setOnlineOptions(false);
            } else if (actionType == GO_ONLINE) {
                settingsFragment.setOnlineOptions(true);
            }
        }
    };

    private final BroadcastReceiver updateCUSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || settingsFragment == null)
                return;

            switch (intent.getAction()) {
                case ACTION_REFRESH_CAMERA_UPLOADS_SETTING:
                    settingsFragment.refreshCameraUploadsSettings();
                    break;

                case ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING:
                    settingsFragment.disableMediaUploadUIProcess();
                    break;
            }
        }
    };

    private final BroadcastReceiver enableDisableCameraUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || settingsFragment == null)
                return;

            switch (intent.getAction()) {
                case ACTION_UPDATE_DISABLE_CU_SETTING:
                    settingsFragment.disableCameraUpload();
                    break;

                case ACTION_UPDATE_DISABLE_CU_UI_SETTING:
                    settingsFragment.disableCameraUploadUIProcess();
                    break;
            }
        }
    };

    private final BroadcastReceiver cameraUploadDestinationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || settingsFragment == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING)) {
                boolean isSecondary = intent.getBooleanExtra(SECONDARY_FOLDER, false);
                long primaryHandle = intent.getLongExtra(PRIMARY_HANDLE, INVALID_HANDLE);
                settingsFragment.setCUDestinationFolder(isSecondary, primaryHandle);
            }
        }
    };

    private final BroadcastReceiver receiverCUAttrChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || settingsFragment == null)
                return;

            synchronized (this) {
                long handleInUserAttr = intent.getLongExtra(EXTRA_NODE_HANDLE, INVALID_HANDLE);
                boolean isSecondary = intent.getBooleanExtra(EXTRA_IS_CU_SECONDARY_FOLDER, false);
                settingsFragment.setCUDestinationFolder(isSecondary, handleInUserAttr);
            }
        }
    };

    private final BroadcastReceiver reEnableCameraUploadsPreferenceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && BROADCAST_ACTION_REENABLE_CU_PREFERENCE.equals(intent.getAction()) && settingsFragment != null) {
                settingsFragment.reEnableCameraUploadsPreference(intent.getIntExtra(KEY_REENABLE_WHICH_PREFERENCE, 0));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.section_photo_sync);
        settingsFragment = new SettingsCameraUploadsFragment();
        replaceFragment(settingsFragment);

        registerReceiver(networkReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));

        registerReceiver(cameraUploadDestinationReceiver,
                new IntentFilter(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING));

        IntentFilter filterCUMUSettings =
                new IntentFilter(ACTION_UPDATE_DISABLE_CU_SETTING);
        filterCUMUSettings.addAction(ACTION_UPDATE_DISABLE_CU_UI_SETTING);
        registerReceiver(enableDisableCameraUploadReceiver, filterCUMUSettings);

        IntentFilter filterUpdateCUSettings =
                new IntentFilter(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CAMERA_UPLOADS_SETTING);
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING);
        registerReceiver(updateCUSettingsReceiver, filterUpdateCUSettings);

        registerReceiver(receiverCUAttrChanged,
                new IntentFilter(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE));

        registerReceiver(reEnableCameraUploadsPreferenceReceiver, new IntentFilter(BROADCAST_ACTION_REENABLE_CU_PREFERENCE));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_UPLOAD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkIfShouldShowBusinessCUAlert();
            } else {
                Util.showSnackbar(this, getString(R.string.on_refuse_storage_permission));
            }
        } else if (requestCode == REQUEST_ACCESS_MEDIA_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableCameraUploadsWithLocation();
            } else {
                Util.showSnackbar(this, getString(R.string.on_refuse_storage_permission));
            }
        }
    }

    /**
     * Method for enabling Camera Uploads Location data.
     */
    private void enableCameraUploadsWithLocation() {
        if (settingsFragment != null) {
            settingsFragment.enableCameraUploadsWithLocation();
        }
    }

    /**
     * Method for enabling Camera Uploads.
     */
    private void enableCU() {
        if (settingsFragment != null) {
            settingsFragment.enableCameraUpload();
        }
    }

    /**
     * Method to check if Business alert needs to be displayed before enabling Camera Uploads.
     */
    public void checkIfShouldShowBusinessCUAlert() {
        if (megaApi.isBusinessAccount() && !megaApi.isMasterBusinessAccount()) {
            showBusinessCUAlert();
        } else {
            enableCU();
        }
    }

    /**
     * Method for displaying the Business alert.
     */
    private void showBusinessCUAlert() {
        if (businessCUAlert != null && businessCUAlert.isShowing()) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        builder.setTitle(R.string.section_photo_sync)
                .setMessage(R.string.camera_uploads_business_alert)
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .setPositiveButton(R.string.general_enable, (dialog, which) -> enableCU())
                .setCancelable(false);
        businessCUAlert = builder.create();
        businessCUAlert.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
        unregisterReceiver(cameraUploadDestinationReceiver);
        unregisterReceiver(enableDisableCameraUploadReceiver);
        unregisterReceiver(updateCUSettingsReceiver);
        unregisterReceiver(receiverCUAttrChanged);
        unregisterReceiver(reEnableCameraUploadsPreferenceReceiver);
    }
}