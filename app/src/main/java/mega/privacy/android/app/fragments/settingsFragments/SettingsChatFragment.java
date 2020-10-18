package mega.privacy.android.app.fragments.settingsFragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaPushNotificationSettings;
import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.DBUtil.isSendOriginalAttachments;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.isOnline;

public class SettingsChatFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {
    MegaChatPresenceConfig statusConfig;

    private Preference chatNotificationsPreference;
    private ListPreference statusChatListPreference;

    private SwitchPreferenceCompat autoAwaySwitch;
    private Preference chatAutoAwayPreference;

    private TwoLineCheckPreference chatPersistenceCheck;

    private SwitchPreferenceCompat enableLastGreenChatSwitch;
    private ListPreference chatAttachmentsChatListPreference;
    private SwitchPreferenceCompat richLinksSwitch;


    public SettingsChatFragment () {
        super();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_chat);

        chatNotificationsPreference = findPreference(KEY_CHAT_NOTIFICATIONS_CHAT);
        chatNotificationsPreference.setOnPreferenceClickListener(this);
        chatNotificationsPreference.setOnPreferenceChangeListener(this);
        updateNotifChat();

        statusChatListPreference = findPreference(KEY_CHAT_STATUS);
        statusChatListPreference.setOnPreferenceChangeListener(this);

        autoAwaySwitch = findPreference(KEY_CHAT_AUTOAWAY_SWITCH);
        autoAwaySwitch.setOnPreferenceClickListener(this);

        chatAutoAwayPreference = findPreference(KEY_CHAT_AUTOAWAY_PREFERENCE);
        chatAutoAwayPreference.setOnPreferenceClickListener(this);

        chatPersistenceCheck = findPreference(KEY_CHAT_PERSISTENCE);
        chatPersistenceCheck.setOnPreferenceClickListener(this);

        enableLastGreenChatSwitch = findPreference(KEY_CHAT_LAST_GREEN);
        enableLastGreenChatSwitch.setOnPreferenceClickListener(this);

        chatAttachmentsChatListPreference = findPreference(KEY_CHAT_SEND_ORIGINALS);
        chatAttachmentsChatListPreference.setOnPreferenceChangeListener(this);

        richLinksSwitch = findPreference(KEY_CHAT_RICH_LINK);
        richLinksSwitch.setOnPreferenceClickListener(this);

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        //Get chat status
        statusConfig = megaChatApi.getPresenceConfig();
        if (statusConfig != null) {
            logDebug("SETTINGS chatStatus pending: " + statusConfig.isPending());
            logDebug("Status: " + statusConfig.getOnlineStatus());

            statusChatListPreference.setValue(statusConfig.getOnlineStatus() + "");
            if (statusConfig.getOnlineStatus() == MegaChatApi.STATUS_INVALID) {
                statusChatListPreference.setSummary(getString(R.string.recovering_info));
            } else {
                statusChatListPreference.setSummary(statusChatListPreference.getEntry());
            }

            showPresenceChatConfig();

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
        } else {
            waitPresenceConfig();
        }

        /*Chat video quality*/
        boolean sendOriginalAttachment = isSendOriginalAttachments();
        if (sendOriginalAttachment) {
            chatAttachmentsChatListPreference.setValue(1 + "");
        } else {
            chatAttachmentsChatListPreference.setValue(0 + "");

        }
        chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());

        /*Rich URL Previews*/
        boolean richLinks = MegaApplication.isEnabledRichLinks();
        richLinksSwitch.setChecked(richLinks);
        richLinksSwitch.setVisible(false);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (preference.getKey()) {
            case KEY_CHAT_NOTIFICATIONS_CHAT:
                startActivity(new Intent(context, ChatNotificationsPreferencesActivity.class));
                break;

            case KEY_CHAT_AUTOAWAY_SWITCH:
                if (!isOnline(context)){
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }
                statusConfig = megaChatApi.getPresenceConfig();
                if(statusConfig!=null){
                    if(statusConfig.isAutoawayEnabled()){
                        logDebug("Change AUTOAWAY chat to false");
                        megaChatApi.setPresenceAutoaway(false, 0);
                        chatAutoAwayPreference.setVisible(false);
                    } else{
                        logDebug("Change AUTOAWAY chat to true");
                        megaChatApi.setPresenceAutoaway(true, 300);
                        chatAutoAwayPreference.setVisible(true);
                        chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, 5));
                    }
                }
                break;

            case KEY_CHAT_AUTOAWAY_PREFERENCE:
                if (!isOnline(context)){
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }
                ((ChatPreferencesActivity)context).showAutoAwayValueDialog();
                break;

            case KEY_CHAT_PERSISTENCE:
                if (!isOnline(context)){
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }

                if(statusConfig.isPersist()){
                    logDebug("Change persistence chat to false");
                    megaChatApi.setPresencePersist(false);
                }
                else{
                    logDebug("Change persistence chat to true");
                    megaChatApi.setPresencePersist(true);
                }
                break;

            case KEY_CHAT_LAST_GREEN:
                if (!isOnline(context)){
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }

                if(!enableLastGreenChatSwitch.isChecked()){
                    logDebug("Disable last green");
                    ((ChatPreferencesActivity)context).enableLastGreen(false);
                }
                else{
                    logDebug("Enable last green");
                    ((ChatPreferencesActivity)context).enableLastGreen(true);
                }
                break;
            case KEY_CHAT_RICH_LINK:
                if (!isOnline(context)) {
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }

                if (richLinksSwitch.isChecked()) {
                    logDebug("Enable rich links");
                    megaApi.enableRichPreviews(true, (ChatPreferencesActivity) context);
                } else {
                    logDebug("Disable rich links");
                    megaApi.enableRichPreviews(false, (ChatPreferencesActivity) context);
                }
                break;
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        logDebug("onPreferenceChange");
        prefs = dbH.getPreferences();
        int newStatus;
        switch (preference.getKey()) {
            case KEY_CHAT_NOTIFICATIONS_CHAT:
                updateNotifChat();
                break;

            case KEY_CHAT_STATUS:
                if (!isOnline(context)){
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }
                statusChatListPreference.setSummary(statusChatListPreference.getEntry());
                newStatus= Integer.parseInt((String)newValue);
                megaChatApi.setOnlineStatus(newStatus, (ChatPreferencesActivity) context);
                break;

            case KEY_CHAT_SEND_ORIGINALS:
                if (!isOnline(context)) {
                    Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
                    return false;
                }

                newStatus = Integer.parseInt((String) newValue);
                if (newStatus == 0) {
                    dbH.setSendOriginalAttachments(false + "");
                    chatAttachmentsChatListPreference.setValue(0 + "");
                } else if (newStatus == 1) {
                    dbH.setSendOriginalAttachments(true + "");
                    chatAttachmentsChatListPreference.setValue(1 + "");
                }
                chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());
                break;
        }

        return true;
    }

    public void updateNotifChat() {
        MegaPushNotificationSettings pushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();

        String option = NOTIFICATIONS_ENABLED;
        if (pushNotificationSettings != null && pushNotificationSettings.isGlobalChatsDndEnabled()) {
            option = pushNotificationSettings.getGlobalChatsDnd() == 0 ? NOTIFICATIONS_DISABLED : NOTIFICATIONS_DISABLED_X_TIME;
        }

        switch (option) {
            case NOTIFICATIONS_DISABLED:
                chatNotificationsPreference.setSummary(getString(R.string.mute_chatroom_notification_option_off));
                break;

            case NOTIFICATIONS_ENABLED:
                chatNotificationsPreference.setSummary(getString(R.string.mute_chat_notification_option_on));
                break;

            default:
                chatNotificationsPreference.setSummary(getCorrectStringDependingOnOptionSelected(pushNotificationSettings.getGlobalChatsDnd()));
        }
    }

    public void updateEnabledRichLinks() {
        if (MegaApplication.isEnabledRichLinks() != richLinksSwitch.isChecked()) {
            richLinksSwitch.setOnPreferenceClickListener(null);
            richLinksSwitch.setChecked(MegaApplication.isEnabledRichLinks());
            richLinksSwitch.setOnPreferenceClickListener(this);
        }
    }

    public void updatePresenceConfigChat(boolean cancelled){
        if(!cancelled){
            statusConfig = megaChatApi.getPresenceConfig();
        }

        showPresenceChatConfig();
    }

    public void waitPresenceConfig(){
        autoAwaySwitch.setVisible(false);
        chatAutoAwayPreference.setVisible(false);
        chatPersistenceCheck.setVisible(false);

        statusChatListPreference.setValue(MegaChatApi.STATUS_OFFLINE+"");
        statusChatListPreference.setSummary(statusChatListPreference.getEntry());
        enableLastGreenChatSwitch.setEnabled(false);
    }

    public void showPresenceChatConfig(){
        statusChatListPreference.setValue(statusConfig.getOnlineStatus()+"");
        statusChatListPreference.setSummary(statusChatListPreference.getEntry());

        if(statusConfig.getOnlineStatus()!= MegaChatApi.STATUS_ONLINE){
            autoAwaySwitch.setVisible(false);
            chatAutoAwayPreference.setVisible(false);

            if(statusConfig.getOnlineStatus()== MegaChatApi.STATUS_OFFLINE){
                chatPersistenceCheck.setVisible(false);
            } else{
                chatPersistenceCheck.setVisible(true);
                chatPersistenceCheck.setChecked(statusConfig.isPersist());
            }

        } else {
            chatPersistenceCheck.setVisible(true);

            //I'm online
            if(statusConfig.isPersist()){
                autoAwaySwitch.setVisible(false);
                chatAutoAwayPreference.setVisible(false);
            } else{
                autoAwaySwitch.setVisible(true);
                if(statusConfig.isAutoawayEnabled()){
                    int timeout = (int)statusConfig.getAutoawayTimeout()/60;
                    autoAwaySwitch.setChecked(true);
                    chatAutoAwayPreference.setVisible(true);
                    chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, timeout));
                } else{
                    autoAwaySwitch.setChecked(false);
                    chatAutoAwayPreference.setVisible(false);
                }
            }
        }


        //Show configuration last green
        if(statusConfig.isLastGreenVisible()){
            logDebug("Last visible ON");
            enableLastGreenChatSwitch.setEnabled(true);
            if(!enableLastGreenChatSwitch.isChecked()){
                enableLastGreenChatSwitch.setOnPreferenceClickListener(null);
                enableLastGreenChatSwitch.setChecked(true);
            }
            enableLastGreenChatSwitch.setOnPreferenceClickListener(this);
        }
        else{
            logDebug("Last visible OFF");
            enableLastGreenChatSwitch.setEnabled(true);
            if(enableLastGreenChatSwitch.isChecked()){
                enableLastGreenChatSwitch.setOnPreferenceClickListener(null);
                enableLastGreenChatSwitch.setChecked(false);
            }
            enableLastGreenChatSwitch.setOnPreferenceClickListener(this);
        }
    }

}
