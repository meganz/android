package mega.privacy.android.app.lollipop.megachat;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class SettingsChatFragment extends PreferenceFragment
{
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

        chatNotificationsSwitch = (SwitchPreference) findPreference(KEY_CHAT_NOTIFICATIONS);
        chatSoundPreference = findPreference(KEY_CHAT_SOUND);
        chatSoundPreference.setSummary("Prueba");
        chatVibrateSwitch = (SwitchPreference) findPreference(KEY_CHAT_VIBRATE);
    }

}
