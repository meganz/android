package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING;
import static mega.privacy.android.app.utils.Constants.SELECT_NOTIFICATION_SOUND;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class CameraUploadsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsChatFragment sttChat;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aB.setTitle(getString(R.string.section_photo_sync).toUpperCase());
        sttChat = new SettingsChatFragment();
        replaceFragment(sttChat);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        logDebug("Result code: " + resultCode);

        if (resultCode == RESULT_OK && requestCode == SELECT_NOTIFICATION_SOUND) {
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}