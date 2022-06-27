package mega.privacy.android.app.activities.settingsActivities;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING;
import static mega.privacy.android.app.utils.Constants.SELECT_NOTIFICATION_SOUND;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatNotificationsFragment;
import timber.log.Timber;


public class ChatNotificationsPreferencesActivity extends PreferencesBaseActivity {

    private SettingsChatNotificationsFragment sttChatNotifications;

    private BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)) {
                sttChatNotifications.updateSwitch();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.title_properties_chat_notifications_contact);

        sttChatNotifications = new SettingsChatNotificationsFragment();
        replaceFragment(sttChatNotifications);
        registerReceiver(chatRoomMuteUpdateReceiver,
                new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));
    }

    public void changeSound(String soundString) {
        Timber.d("Sound string: %s", soundString);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_title));

        if (soundString == null) {
            Timber.w("NULL sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        } else if (soundString.equals("-1")) {
            Timber.w("Notification sound -1");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        } else if (soundString.isEmpty()) {
            Timber.w("Empty sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        } else {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundString));
        }

        this.startActivityForResult(intent, SELECT_NOTIFICATION_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Timber.d("Result code: %s", resultCode);

        if (resultCode == RESULT_OK && requestCode == SELECT_NOTIFICATION_SOUND) {
            Timber.d("Selected notification sound OK");
            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (sttChatNotifications != null && sttChatNotifications.isAdded()) {
                sttChatNotifications.setNotificationSound(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(chatRoomMuteUpdateReceiver);
    }
}