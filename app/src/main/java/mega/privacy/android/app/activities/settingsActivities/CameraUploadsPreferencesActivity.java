package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsCameraUploadsFragment;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class CameraUploadsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsCameraUploadsFragment sttCameraUploads;
    private AlertDialog businessCUAlert;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received!");
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);

            if (actionType == GO_OFFLINE) {
                sttCameraUploads.setOnlineOptions(false);
            } else if (actionType == GO_ONLINE) {
                sttCameraUploads.setOnlineOptions(true);
            }
        }
    };

    private BroadcastReceiver updateCUSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            switch (intent.getAction()) {
                case ACTION_REFRESH_CAMERA_UPLOADS_SETTING:
                    sttCameraUploads.refreshCameraUploadsSettings();
                    break;

                case ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING:
                    sttCameraUploads.disableMediaUploadUIProcess();
                    break;
            }
        }
    };

    private BroadcastReceiver enableDisableCameraUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            switch (intent.getAction()){
                case ACTION_UPDATE_ENABLE_CU_SETTING:
                    sttCameraUploads.enableCameraUpload();
                    break;

                case ACTION_UPDATE_DISABLE_CU_SETTING:
                    sttCameraUploads.disableCameraUpload();
                    break;

                case ACTION_UPDATE_DISABLE_CU_UI_SETTING:
                    sttCameraUploads.disableCameraUploadUIProcess();
                    break;
            }
        }
    };

    private BroadcastReceiver cameraUploadDestinationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING)) {
                boolean isSecondary = intent.getBooleanExtra(SECONDARY_FOLDER, false);
                long primaryHandle = intent.getLongExtra(PRIMARY_HANDLE, INVALID_HANDLE);
                sttCameraUploads.setCUDestinationFolder(isSecondary, primaryHandle);
            }
        }
    };

    private BroadcastReceiver receiverCUAttrChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            synchronized (this) {
                long handleInUserAttr = intent.getLongExtra(EXTRA_NODE_HANDLE, INVALID_HANDLE);
                boolean isSecondary = intent.getBooleanExtra(EXTRA_IS_CU_SECONDARY_FOLDER, false);
                sttCameraUploads.setCUDestinationFolder(isSecondary, handleInUserAttr);
            }
        }
    };

    private BroadcastReceiver reEnableCameraUploadsPreferenceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && BROADCAST_ACTION_REENABLE_CU_PREFERENCE.equals(intent.getAction()) && sttCameraUploads != null) {
                sttCameraUploads.reEnableCameraUploadsPreference(intent.getIntExtra(KEY_REENABLE_WHICH_PREFERENCE, 0));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.section_photo_sync).toUpperCase());
        sttCameraUploads = new SettingsCameraUploadsFragment();
        replaceFragment(sttCameraUploads);

        registerReceiver(networkReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));

        registerReceiver(cameraUploadDestinationReceiver,
                new IntentFilter(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING));

        IntentFilter filterCUMUSettings =
                new IntentFilter(ACTION_UPDATE_ENABLE_CU_SETTING);
        filterCUMUSettings.addAction(ACTION_UPDATE_DISABLE_CU_SETTING);
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
        switch (requestCode) {
            case REQUEST_CAMERA_UPLOAD: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkIfShouldShowBusinessCUAlert();
                } else {
                    Util.showSnackbar(this, getString(R.string.on_refuse_storage_permission));
                }
                break;
            }
        }
    }

    /**
     * Method for enabling Camera Uploads.
     */
    private void enableCU() {
        if (sttCameraUploads != null) {
            sttCameraUploads.enableCameraUpload();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleNormal);
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