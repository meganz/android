package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class SettingsChatFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener
{
    Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs;
    ChatSettings chatSettings;

    public static String KEY_CHAT_NOTIFICATIONS = "settings_chat_notifications";
    public static String KEY_CHAT_SOUND = "settings_chat_sound";
    public static String KEY_CHAT_VIBRATE = "settings_chat_vibrate";

    SwitchPreference chatNotificationsSwitch;
    Preference chatSoundPreference;
    SwitchPreference chatVibrateSwitch;

    TwoLineCheckPreference chatVibrateCheck;
    TwoLineCheckPreference chatNotificationsCheck;

    boolean chatNotifications;
    boolean chatVibration;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        if (megaApi == null) {
            megaApi = ((MegaApplication) getActivity().getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) getActivity().getApplication()).getMegaChatApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
        prefs = dbH.getPreferences();
        chatSettings = dbH.getChatSettings();

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_chat);

        chatSoundPreference = findPreference(KEY_CHAT_SOUND);
        chatSoundPreference.setOnPreferenceClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            chatNotificationsSwitch = (SwitchPreference) findPreference(KEY_CHAT_NOTIFICATIONS);
            chatNotificationsSwitch.setOnPreferenceClickListener(this);

            chatVibrateSwitch = (SwitchPreference) findPreference(KEY_CHAT_VIBRATE);
            chatVibrateSwitch.setOnPreferenceClickListener(this);

        }
        else{
            chatVibrateCheck = (TwoLineCheckPreference) findPreference(KEY_CHAT_VIBRATE);
            chatVibrateCheck.setOnPreferenceClickListener(this);

            chatNotificationsCheck = (TwoLineCheckPreference) findPreference(KEY_CHAT_NOTIFICATIONS);
            chatNotificationsCheck.setOnPreferenceClickListener(this);
        }

        if(chatSettings==null){
            log("Chat settings is NULL");
            dbH.setNotificationEnabledChat(true+"");
            dbH.setVibrationEnabledChat(true+"");
            dbH.setNotificationSoundChat("");
            chatNotifications = true;
            chatVibration = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                chatNotificationsSwitch.setChecked(chatNotifications);
                chatVibrateSwitch.setChecked(chatVibration);
            }
            else{
                chatNotificationsCheck.setChecked(chatNotifications);
                chatVibrateCheck.setChecked(chatVibration);
            }

            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
            chatSoundPreference.setSummary(defaultSound.getTitle(context));
        }
        else{
            log("There is chat settings");
            if (chatSettings.getNotificationsEnabled() == null){
                dbH.setNotificationEnabledChat(true+"");
                chatNotifications = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chatNotificationsSwitch.setChecked(chatNotifications);
                }
                else{
                    chatNotificationsCheck.setChecked(chatNotifications);
                }
            }
            else{
                chatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chatNotificationsSwitch.setChecked(chatNotifications);
                }
                else{
                    chatNotificationsCheck.setChecked(chatNotifications);
                }
            }

            if (chatSettings.getVibrationEnabled() == null){
                dbH.setVibrationEnabledChat(true+"");
                chatVibration = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chatVibrateSwitch.setChecked(chatVibration);
                }
                else{
                    chatVibrateCheck.setChecked(chatVibration);
                }
            }
            else{
                chatVibration = Boolean.parseBoolean(chatSettings.getVibrationEnabled());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chatVibrateSwitch.setChecked(chatVibration);
                }
                else{
                    chatVibrateCheck.setChecked(chatVibration);
                }
            }

//            Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
//            chatSoundPreference.setSummary(defaultSound2.getTitle(context));
//            log("---Notification sound: "+defaultSound2.getTitle(context));

            if (chatSettings.getNotificationsSound() == null){
                log("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                chatSoundPreference.setSummary(defaultSound.getTitle(context));
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
            }
            else{
                if(chatSettings.getNotificationsSound().equals("")){
                    log("Notification sound is EMPTY");
                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                    chatSoundPreference.setSummary(defaultSound.getTitle(context));
                }
                else{
                    String soundString = chatSettings.getNotificationsSound();
                    log("Sound stored in DB: "+soundString);
                    Uri uri = Uri.parse(soundString);
                    log("Uri: "+uri);

                    if(soundString.equals("true")){

                        Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
                        chatSoundPreference.setSummary(defaultSound2.getTitle(context));
                        log("---Notification sound: "+defaultSound2.getTitle(context));
                        dbH.setNotificationSoundChat(defaultSoundUri2.toString());
                    }
                    else{
                        Ringtone sound = RingtoneManager.getRingtone(context, Uri.parse(soundString));
                        if(sound==null){
                            log("Sound is null");
                            chatSoundPreference.setSummary("None");
                        }
                        else{
                            String titleSound = sound.getTitle(context);
                            log("Notification sound: "+titleSound);
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
            log("KEY_CHAT_NOTIFICATIONS");
            chatNotifications = !chatNotifications;
            setChatPreferences();
        }
        else if (preference.getKey().compareTo(KEY_CHAT_VIBRATE) == 0){
            log("KEY_CHAT_VIBRATE");
            chatVibration = !chatVibration;
            if (chatVibration){
                dbH.setVibrationEnabledChat(true+"");
            }
            else{
                dbH.setVibrationEnabledChat(false+"");
            }
        }
        else if (preference.getKey().compareTo(KEY_CHAT_SOUND) == 0){
            log("KEY_CHAT_SOUND");

            ((ChatPreferencesActivity) context).changeSound(chatSettings.getNotificationsSound());
        }
        return true;
    }

    public void setChatPreferences(){
        if (chatNotifications){
            dbH.setNotificationEnabledChat(true+"");
            getPreferenceScreen().addPreference(chatSoundPreference);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getPreferenceScreen().addPreference(chatVibrateSwitch);

            }
            else{
                getPreferenceScreen().addPreference(chatNotificationsCheck);
            }
        }
        else{
            dbH.setNotificationEnabledChat(false+"");
            getPreferenceScreen().removePreference(chatSoundPreference);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getPreferenceScreen().removePreference(chatVibrateSwitch);

            }
            else{
                getPreferenceScreen().removePreference(chatNotificationsCheck);
            }
        }
    }

    public void setNotificationSound (Uri uri){

        String chosenSound = "-1";
        if(uri!=null){
            Ringtone sound = RingtoneManager.getRingtone(context, uri);

            String title = sound.getTitle(context);

            if(title!=null){
                log("Title sound notification: "+title);
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("SettingsChatFragment", log);
    }
}
