package mega.privacy.android.app.activities.settingsActivities;


import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatPreferencesActivity extends PreferencesBaseActivity {

    private SettingsChatFragment sttChat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");

        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.section_chat));

        sttChat = new SettingsChatFragment();
        replaceFragment(sttChat);
    }

    public void changeSound(String soundString) {
        logDebug("Sound string: " + soundString);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_title));

        if (soundString == null) {
            logWarning("NULL sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        } else if (soundString.equals("-1")) {
            logWarning("Notification sound -1");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        } else if (soundString.isEmpty()) {
            logWarning("Empty sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        } else {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundString));
        }


        this.startActivityForResult(intent, SELECT_NOTIFICATION_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        logDebug("Result code: " + resultCode);

        if (resultCode == RESULT_OK && requestCode == SELECT_NOTIFICATION_SOUND) {
            logDebug("Selected notification sound OK");

            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if (sttChat != null) {
                if (sttChat.isAdded()) {
                    sttChat.setNotificationSound(uri);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }
}
