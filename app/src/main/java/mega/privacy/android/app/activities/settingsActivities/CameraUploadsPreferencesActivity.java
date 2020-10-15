package mega.privacy.android.app.activities.settingsActivities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsCUFragment;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING;
import static mega.privacy.android.app.constants.SettingsConstants.REQUEST_CAMERA_FOLDER;
import static mega.privacy.android.app.constants.SettingsConstants.REQUEST_CODE_TREE_LOCAL_CAMERA;
import static mega.privacy.android.app.constants.SettingsConstants.REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER;
import static mega.privacy.android.app.constants.SettingsConstants.REQUEST_MEGA_CAMERA_FOLDER;
import static mega.privacy.android.app.constants.SettingsConstants.REQUEST_MEGA_SECONDARY_MEDIA_FOLDER;
import static mega.privacy.android.app.constants.SettingsConstants.SELECTED_MEGA_FOLDER;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetCUTimestampsAndCache;
import static mega.privacy.android.app.utils.Constants.SELECT_NOTIFICATION_SOUND;
import static mega.privacy.android.app.utils.Constants.SET_PIN;
import static mega.privacy.android.app.utils.JobUtil.rescheduleCameraUpload;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class CameraUploadsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsCUFragment sttCameraUploads;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aB.setTitle(getString(R.string.section_photo_sync).toUpperCase());
        sttCameraUploads = new SettingsCUFragment();
        replaceFragment(sttCameraUploads);
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//
//        logDebug("Result code: " + resultCode);
//
//        if (resultCode == RESULT_OK) {
//
//            REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER;
//            REQUEST_MEGA_SECONDARY_MEDIA_FOLDER;
//            REQUEST_CAMERA_FOLDER;
//            REQUEST_MEGA_CAMERA_FOLDER
//        }
//        super.onActivityResult(requestCode, resultCode, intent);
//    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}