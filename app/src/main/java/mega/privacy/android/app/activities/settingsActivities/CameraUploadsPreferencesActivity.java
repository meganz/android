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
import mega.privacy.android.app.fragments.settingsFragments.SettingsCUFragment;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class CameraUploadsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsCUFragment sttCameraUploads;
    private AlertDialog businessCUAlert;

    private BroadcastReceiver offlineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_ONLINE_OPTIONS_SETTING)) {
                boolean isOnline = intent.getBooleanExtra(ONLINE_OPTION, false);
                sttCameraUploads.setOnlineOptions(isOnline);
            }
        }
    };

    private BroadcastReceiver disableCameraUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_DISABLE_CU_SETTING)) {
                sttCameraUploads.disableCameraUpload();
            }
        }
    };

    private BroadcastReceiver disableMediaUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_DISABLE_MU_SETTING)) {
                sttCameraUploads.disableMediaUploadUIProcess();
            }
        }
    };

    private BroadcastReceiver disableCameraUploadUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_DISABLE_CU_UI_SETTING)) {
                sttCameraUploads.disableCameraUploadUIProcess();
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
                long primaryHandle = intent.getLongExtra(PRIMARY_HANDLE, -1);
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

    private BroadcastReceiver enableCameraUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttCameraUploads == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_ENABLE_CU_SETTING)) {
                sttCameraUploads.enableCameraUpload();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.section_photo_sync).toUpperCase());
        sttCameraUploads = new SettingsCUFragment();
        replaceFragment(sttCameraUploads);

        registerReceiver(offlineReceiver,
                new IntentFilter(ACTION_UPDATE_ONLINE_OPTIONS_SETTING));
        registerReceiver(cameraUploadDestinationReceiver,
                new IntentFilter(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING));
        registerReceiver(enableCameraUploadReceiver,
                new IntentFilter(ACTION_UPDATE_ENABLE_CU_SETTING));
        registerReceiver(disableCameraUploadUIReceiver,
                new IntentFilter(ACTION_UPDATE_DISABLE_CU_UI_SETTING));
        registerReceiver(disableCameraUploadReceiver,
                new IntentFilter(ACTION_UPDATE_DISABLE_CU_SETTING));
        registerReceiver(disableMediaUploadReceiver,
                new IntentFilter(ACTION_UPDATE_DISABLE_MU_SETTING));
        IntentFilter filterUpdateCUSettings =
                new IntentFilter(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CAMERA_UPLOADS_SETTING);
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING);
        registerReceiver(updateCUSettingsReceiver, filterUpdateCUSettings);
        registerReceiver(receiverCUAttrChanged,
                new IntentFilter(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE));
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
        unregisterReceiver(offlineReceiver);
        unregisterReceiver(cameraUploadDestinationReceiver);
        unregisterReceiver(enableCameraUploadReceiver);
        unregisterReceiver(updateCUSettingsReceiver);
        unregisterReceiver(disableCameraUploadUIReceiver);
        unregisterReceiver(disableCameraUploadReceiver);
        unregisterReceiver(disableMediaUploadReceiver);
        unregisterReceiver(receiverCUAttrChanged);
    }
}