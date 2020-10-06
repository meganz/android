package mega.privacy.android.app.activities.settingsActivities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatPreferencesActivity extends PreferencesBaseActivity {

    private SettingsChatFragment sttChat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.section_chat).toUpperCase());

        sttChat = new SettingsChatFragment();
        replaceFragment(sttChat);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("Result code: " + resultCode);

        if (resultCode == RESULT_OK ) { }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
