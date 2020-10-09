package mega.privacy.android.app.fragments.settingsFragments;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import nz.mega.sdk.MegaPushNotificationSettings;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.isOnline;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class SettingsChatFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    ChatSettings chatSettings;

    private Preference chatNotificationsPreference;
    private ListPreference statusListPreference;
    private SwitchPreferenceCompat autoawaySwitch;
    private Preference autoawayPreference;
    private TwoLineCheckPreference persistenceTwoLineCheckPreference;
    private SwitchPreferenceCompat lastGreenSwitch;
    private ListPreference sendOriginalListPreference;
    private SwitchPreferenceCompat richLinksSwitch;

    public SettingsChatFragment () {
        super();
        chatSettings = dbH.getChatSettings();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_chat);

        chatNotificationsPreference = findPreference(KEY_CHAT_NOTIFICATIONS_CHAT);
        statusListPreference = findPreference(KEY_CHAT_STATUS);
        autoawaySwitch = findPreference(KEY_CHAT_AUTOAWAY_SWITCH);
        autoawayPreference = findPreference(KEY_CHAT_AUTOAWAY_PREFERENCE);
        persistenceTwoLineCheckPreference = findPreference(KEY_CHAT_PERSISTENCE);
        lastGreenSwitch = findPreference(KEY_CHAT_LAST_GREEN);
        sendOriginalListPreference = findPreference(KEY_CHAT_SEND_ORIGINALS);
        richLinksSwitch = findPreference(KEY_CHAT_RICH_LINK);
        richLinksSwitch.setOnPreferenceClickListener(this);


        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }



        boolean richLinks = MegaApplication.isEnabledRichLinks();
        richLinksSwitch.setChecked(richLinks);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (preference.getKey()) {

            case KEY_CHAT_RICH_LINK:
                if (!isOnline(context)){
                    ((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                    return false;
                }

                if(richLinksSwitch.isChecked()){
                    logDebug("Enable rich links");
                    megaApi.enableRichPreviews(true, (ManagerActivityLollipop)context);
                }
                else{
                    logDebug("Disable rich links");
                    megaApi.enableRichPreviews(false, (ManagerActivityLollipop)context);
                }
                break;
        }

        return true;
    }

    public void updateEnabledRichLinks(){
        logDebug("updateEnabledRichLinks");

        if(MegaApplication.isEnabledRichLinks()!=richLinksSwitch.isChecked()){
            richLinksSwitch.setOnPreferenceClickListener(null);
            richLinksSwitch.setChecked(MegaApplication.isEnabledRichLinks());
            richLinksSwitch.setOnPreferenceClickListener(this);
        }
    }
}
