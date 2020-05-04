package mega.privacy.android.app.fragments.settingsFragments;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class SettingsChatFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    ChatSettings chatSettings;

    SwitchPreferenceCompat chatNotificationsSwitch;
    Preference chatSoundPreference;
    SwitchPreferenceCompat chatVibrateSwitch;

    boolean chatNotifications;
    boolean chatVibration;

    public SettingsChatFragment () {
        super();

        chatSettings = dbH.getChatSettings();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_chat);

        chatSoundPreference = findPreference(KEY_CHAT_SOUND);
        chatSoundPreference.setOnPreferenceClickListener(this);

        chatNotificationsSwitch = (SwitchPreferenceCompat) findPreference(KEY_CHAT_NOTIFICATIONS);
        chatNotificationsSwitch.setOnPreferenceClickListener(this);

        chatVibrateSwitch = (SwitchPreferenceCompat) findPreference(KEY_CHAT_VIBRATE);
        chatVibrateSwitch.setOnPreferenceClickListener(this);


        if(chatSettings==null){
            logDebug("Chat settings is NULL");
            dbH.setNotificationEnabledChat(true+"");
            dbH.setVibrationEnabledChat(true+"");
            dbH.setNotificationSoundChat("");
            chatNotifications = true;
            chatVibration = true;

            chatNotificationsSwitch.setChecked(chatNotifications);
            chatVibrateSwitch.setChecked(chatVibration);

            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
            chatSoundPreference.setSummary(defaultSound.getTitle(context));
        }
        else{
            logDebug("There is chat settings");
            if (chatSettings.getNotificationsEnabled() == null){
                dbH.setNotificationEnabledChat(true+"");
                chatNotifications = true;
                chatNotificationsSwitch.setChecked(chatNotifications);
            }
            else{
                chatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());
                chatNotificationsSwitch.setChecked(chatNotifications);
            }

            if (chatSettings.getVibrationEnabled() == null){
                dbH.setVibrationEnabledChat(true+"");
                chatVibration = true;
                chatVibrateSwitch.setChecked(chatVibration);
            }
            else{
                chatVibration = Boolean.parseBoolean(chatSettings.getVibrationEnabled());
                chatVibrateSwitch.setChecked(chatVibration);
            }

//            Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
//            chatSoundPreference.setSummary(defaultSound2.getTitle(context));
//            log("---Notification sound: "+defaultSound2.getTitle(context));

            if (chatSettings.getNotificationsSound() == null){
                logDebug("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                chatSoundPreference.setSummary(defaultSound.getTitle(context));
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
            }
            else{
                if(chatSettings.getNotificationsSound().equals("")){
                    logDebug("Notification sound is EMPTY");
                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                    if (defaultSound == null) {
//                        None Mode could not be fetched as default sound in some devices such as Huawei's devices, set Silent as title
                        logWarning("defaultSound == null");
                        chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
                    }
                    else {
                        chatSoundPreference.setSummary(defaultSound.getTitle(context));
                    }
                }
                else{
                    String soundString = chatSettings.getNotificationsSound();
                    logDebug("Sound stored in DB: " + soundString);
                    Uri uri = Uri.parse(soundString);
                    logDebug("Uri: " + uri);

                    if(soundString.equals("true")){

                        Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
                        chatSoundPreference.setSummary(defaultSound2.getTitle(context));
                        logDebug("Notification sound: " + defaultSound2.getTitle(context));
                        dbH.setNotificationSoundChat(defaultSoundUri2.toString());
                    }
                    else{
                        Ringtone sound = RingtoneManager.getRingtone(context, Uri.parse(soundString));
                        if(sound==null){
                            logWarning("Sound is null");
                            chatSoundPreference.setSummary("None");
                        }
                        else{
                            String titleSound = sound.getTitle(context);
                            logDebug("Notification sound: " + titleSound);
                            chatSoundPreference.setSummary(titleSound);
                        }
                    }

                }
            }
        }

        setChatPreferences();

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (preference.getKey().compareTo(KEY_CHAT_NOTIFICATIONS) == 0){
            logDebug("KEY_CHAT_NOTIFICATIONS");
            chatNotifications = !chatNotifications;
            setChatPreferences();
        }
        else if (preference.getKey().compareTo(KEY_CHAT_VIBRATE) == 0){
            logDebug("KEY_CHAT_VIBRATE");
            chatVibration = !chatVibration;
            if (chatVibration){
                dbH.setVibrationEnabledChat(true+"");
            }
            else{
                dbH.setVibrationEnabledChat(false+"");
            }
        }
        else if (preference.getKey().compareTo(KEY_CHAT_SOUND) == 0){
            logDebug("KEY_CHAT_SOUND");

            ((ChatPreferencesActivity) context).changeSound(chatSettings.getNotificationsSound());
        }
        return true;
    }

    public void setChatPreferences(){
        if (chatNotifications){
            dbH.setNotificationEnabledChat(true+"");
            getPreferenceScreen().addPreference(chatSoundPreference);

            getPreferenceScreen().addPreference(chatVibrateSwitch);
        }
        else{
            dbH.setNotificationEnabledChat(false+"");
            getPreferenceScreen().removePreference(chatSoundPreference);

            getPreferenceScreen().removePreference(chatVibrateSwitch);
        }
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
