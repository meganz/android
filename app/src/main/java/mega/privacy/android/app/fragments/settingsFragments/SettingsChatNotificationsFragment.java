package mega.privacy.android.app.fragments.settingsFragments;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import nz.mega.sdk.MegaPushNotificationSettings;

import static mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_DND;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_NOTIFICATIONS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_SOUND;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_VIBRATE;
import static mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsChatAlertDialog;
import static mega.privacy.android.app.utils.ChatUtil.getGeneralNotification;
import static mega.privacy.android.app.utils.Constants.INVALID_OPTION;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_X_TIME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnOptionSelected;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class SettingsChatNotificationsFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    ChatSettings chatSettings;

    private SwitchPreferenceCompat chatNotificationsSwitch;
    private Preference chatSoundPreference;
    private SwitchPreferenceCompat chatVibrateSwitch;
    private SwitchPreferenceCompat chatDndSwitch;

    public SettingsChatNotificationsFragment () {
        super();

        chatSettings = dbH.getChatSettings();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_chat);

        chatNotificationsSwitch = findPreference(KEY_CHAT_NOTIFICATIONS);
        chatNotificationsSwitch.setOnPreferenceClickListener(this);

        chatSoundPreference = findPreference(KEY_CHAT_SOUND);
        chatSoundPreference.setVisible(true);
        chatSoundPreference.setOnPreferenceClickListener(this);

        chatVibrateSwitch = findPreference(KEY_CHAT_VIBRATE);
        chatVibrateSwitch.setVisible(true);
        chatVibrateSwitch.setEnabled(true);
        chatVibrateSwitch.setOnPreferenceClickListener(this);

        chatDndSwitch = findPreference(KEY_CHAT_DND);
        chatDndSwitch.setVisible(false);
        chatDndSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            if (((SwitchPreferenceCompat) preference).isChecked()) {
                MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, NOTIFICATIONS_ENABLED, MEGACHAT_INVALID_HANDLE);
            } else {
                createMuteNotificationsChatAlertDialog(((ChatPreferencesActivity) context), MEGACHAT_INVALID_HANDLE);
            }
            return false;
        });

        chatNotificationsSwitch.setChecked(getGeneralNotification().equals(NOTIFICATIONS_ENABLED));

        updateSwitch();

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
    }

    /**
     * Method to update the UI items when the Push notification Settings change.
     */
    public void updateSwitch() {
        MegaPushNotificationSettings pushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        String option = NOTIFICATIONS_ENABLED;

        if (pushNotificationSettings != null) {
            if (pushNotificationSettings.isGlobalChatsDndEnabled()) {
                option = pushNotificationSettings.getGlobalChatsDnd() == 0 ? NOTIFICATIONS_DISABLED : NOTIFICATIONS_DISABLED_X_TIME;
            } else {
                option = NOTIFICATIONS_ENABLED;
            }
        }

        if (option.equals(NOTIFICATIONS_DISABLED)) {
            chatDndSwitch.setVisible(false);
            return;
        }

        chatNotificationsSwitch.setChecked(option.equals(NOTIFICATIONS_ENABLED));

        if (chatSettings.getVibrationEnabled() == null || Boolean.parseBoolean(chatSettings.getVibrationEnabled())) {
            dbH.setVibrationEnabledChat(true + "");
            chatSettings.setVibrationEnabled(true + "");
            chatVibrateSwitch.setChecked(true);
        } else {
            dbH.setVibrationEnabledChat(false + "");
            chatSettings.setVibrationEnabled(false + "");
            chatVibrateSwitch.setChecked(false);
        }

        if (isTextEmpty(chatSettings.getNotificationsSound())) {
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
            chatSoundPreference.setSummary(defaultSound == null ? getString(R.string.settings_chat_silent_sound_not) : defaultSound.getTitle(context));
        } else if (chatSettings.getNotificationsSound().equals(INVALID_OPTION)) {
            chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
        } else {
            String soundString = chatSettings.getNotificationsSound();
            if (soundString.equals("true")) {
                Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
                chatSoundPreference.setSummary(defaultSound2.getTitle(context));
                dbH.setNotificationSoundChat(defaultSoundUri2.toString());

            } else {
                Ringtone sound = RingtoneManager.getRingtone(context, Uri.parse(soundString));
                if (sound != null) {
                    chatSoundPreference.setSummary(sound.getTitle(context));
                } else {
                    logWarning("Sound is null");
                }
            }
        }

        chatDndSwitch.setVisible(true);

        if (option.equals(NOTIFICATIONS_ENABLED)) {
            chatDndSwitch.setChecked(false);
            chatDndSwitch.setSummary(getString(R.string.mute_chatroom_notification_option_off));
        } else {
            chatDndSwitch.setChecked(true);
            long timestampMute = pushNotificationSettings.getGlobalChatsDnd();
            chatDndSwitch.setSummary(getCorrectStringDependingOnOptionSelected(timestampMute));
        }

        getPreferenceScreen().addPreference(chatSoundPreference);
        getPreferenceScreen().addPreference(chatVibrateSwitch);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (preference.getKey()) {
            case KEY_CHAT_NOTIFICATIONS:
                MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, chatNotificationsSwitch.isChecked() ? NOTIFICATIONS_ENABLED : NOTIFICATIONS_DISABLED, MEGACHAT_INVALID_HANDLE);
                break;

            case KEY_CHAT_VIBRATE:
                if (chatSettings.getVibrationEnabled() == null || Boolean.parseBoolean(chatSettings.getVibrationEnabled())) {
                    dbH.setVibrationEnabledChat(false + "");
                    chatSettings.setVibrationEnabled(false + "");
                } else {
                    dbH.setVibrationEnabledChat(true + "");
                    chatSettings.setVibrationEnabled(true + "");
                }
                break;

            case KEY_CHAT_SOUND:
                ((ChatNotificationsPreferencesActivity) context).changeSound(chatSettings.getNotificationsSound());
                break;
        }

        return true;
    }

    public void setNotificationSound (Uri uri){

        String chosenSound = "-1";
        if(uri!=null){
            Ringtone sound = RingtoneManager.getRingtone(context, uri);

            String title = sound.getTitle(context);

            if(title!=null){
                logDebug("Title sound notification: " + title);
                chatSoundPreference.setSummary(title);
            }

            chosenSound = uri.toString();
        }
        else{
            chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
        }

        if(chatSettings==null){
            chatSettings = new ChatSettings();
            chatSettings.setNotificationsSound(chosenSound);
            dbH.setChatSettings(chatSettings);
        }
        else{
            chatSettings.setNotificationsSound(chosenSound);
            dbH.setNotificationSoundChat(chosenSound);
        }

    }
}

