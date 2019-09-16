package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class SettingsChatFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs;
    ChatSettings chatSettings;

    public static String KEY_CHAT_NOTIFICATIONS = "settings_chat_notifications";
    public static String KEY_CHAT_SOUND = "settings_chat_sound";
    public static String KEY_CHAT_VIBRATE = "settings_chat_vibrate";

    SwitchPreferenceCompat chatNotificationsSwitch;
    Preference chatSoundPreference;
    SwitchPreferenceCompat chatVibrateSwitch;

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
            LogUtil.logDebug("Chat settings is NULL");
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
            LogUtil.logDebug("There is chat settings");
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
                LogUtil.logDebug("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                chatSoundPreference.setSummary(defaultSound.getTitle(context));
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
            }
            else{
                if(chatSettings.getNotificationsSound().equals("")){
                    LogUtil.logDebug("Notification sound is EMPTY");
                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri);
                    if (defaultSound == null) {
//                        None Mode could not be fetched as default sound in some devices such as Huawei's devices, set Silent as title
                        LogUtil.logWarning("defaultSound == null");
                        chatSoundPreference.setSummary(getString(R.string.settings_chat_silent_sound_not));
                    }
                    else {
                        chatSoundPreference.setSummary(defaultSound.getTitle(context));
                    }
                }
                else{
                    String soundString = chatSettings.getNotificationsSound();
                    LogUtil.logDebug("Sound stored in DB: " + soundString);
                    Uri uri = Uri.parse(soundString);
                    LogUtil.logDebug("Uri: " + uri);

                    if(soundString.equals("true")){

                        Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2);
                        chatSoundPreference.setSummary(defaultSound2.getTitle(context));
                        LogUtil.logDebug("Notification sound: " + defaultSound2.getTitle(context));
                        dbH.setNotificationSoundChat(defaultSoundUri2.toString());
                    }
                    else{
                        Ringtone sound = RingtoneManager.getRingtone(context, Uri.parse(soundString));
                        if(sound==null){
                            LogUtil.logWarning("Sound is null");
                            chatSoundPreference.setSummary("None");
                        }
                        else{
                            String titleSound = sound.getTitle(context);
                            LogUtil.logDebug("Notification sound: " + titleSound);
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
            LogUtil.logDebug("KEY_CHAT_NOTIFICATIONS");
            chatNotifications = !chatNotifications;
            setChatPreferences();
        }
        else if (preference.getKey().compareTo(KEY_CHAT_VIBRATE) == 0){
            LogUtil.logDebug("KEY_CHAT_VIBRATE");
            chatVibration = !chatVibration;
            if (chatVibration){
                dbH.setVibrationEnabledChat(true+"");
            }
            else{
                dbH.setVibrationEnabledChat(false+"");
            }
        }
        else if (preference.getKey().compareTo(KEY_CHAT_SOUND) == 0){
            LogUtil.logDebug("KEY_CHAT_SOUND");

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
                LogUtil.logDebug("Title sound notification: " + title);
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
}
