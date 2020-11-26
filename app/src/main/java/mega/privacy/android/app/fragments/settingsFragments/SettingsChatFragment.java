package mega.privacy.android.app.fragments.settingsFragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaPushNotificationSettings;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.DBUtil.isSendOriginalAttachments;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.isOffline;
import static mega.privacy.android.app.utils.Util.isOnline;

public class SettingsChatFragment extends SettingsBaseFragment {

    private MegaChatPresenceConfig statusConfig;
    private Preference chatNotificationsPreference;
    private ListPreference statusChatListPreference;
    private SwitchPreferenceCompat autoAwaySwitch;
    private Preference chatAutoAwayPreference;
    private TwoLineCheckPreference chatPersistenceCheck;
    private SwitchPreferenceCompat enableLastGreenChatSwitch;
    private ListPreference chatAttachmentsChatListPreference;
    private SwitchPreferenceCompat richLinksSwitch;

    public SettingsChatFragment() {
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
            logDebug("ChatStatus pending: " + statusConfig.isPending() + ", status: " + statusConfig.getOnlineStatus());

            statusChatListPreference.setValue(statusConfig.getOnlineStatus() + "");
            statusChatListPreference.setSummary(statusConfig.getOnlineStatus() == MegaChatApi.STATUS_INVALID ?
                    getString(R.string.recovering_info) : statusChatListPreference.getEntry());

            showPresenceChatConfig();

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }

        } else {
            waitPresenceConfig();
        }

        /*Chat video quality*/
        chatAttachmentsChatListPreference.setValue(isSendOriginalAttachments() ? 1 + "" : 0 + "");
        chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());

        /*Rich URL Previews*/
        richLinksSwitch.setChecked(MegaApplication.isEnabledRichLinks());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        if (isOffline(context))
            return false;

        switch (preference.getKey()) {
            case KEY_CHAT_NOTIFICATIONS_CHAT:
                startActivity(new Intent(context, ChatNotificationsPreferencesActivity.class));
                break;

            case KEY_CHAT_AUTOAWAY_SWITCH:
                statusConfig = megaChatApi.getPresenceConfig();
                if (statusConfig != null) {
                    if (statusConfig.isAutoawayEnabled()) {
                        logDebug("Change AUTOAWAY chat to false");
                        megaChatApi.setPresenceAutoaway(false, 0);
                        getPreferenceScreen().removePreference(chatAutoAwayPreference);
                    } else {
                        logDebug("Change AUTOAWAY chat to true");
                        megaChatApi.setPresenceAutoaway(true, 300);
                        getPreferenceScreen().addPreference(chatAutoAwayPreference);
                        chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, 5));
                    }
                }
                break;

            case KEY_CHAT_AUTOAWAY_PREFERENCE:
                ((ChatPreferencesActivity) context).showAutoAwayValueDialog();
                break;

            case KEY_CHAT_PERSISTENCE:
                megaChatApi.setPresencePersist(!statusConfig.isPersist());
                break;

            case KEY_CHAT_LAST_GREEN:
                ((ChatPreferencesActivity) context).enableLastGreen(enableLastGreenChatSwitch.isChecked());
                break;

            case KEY_CHAT_RICH_LINK:
                megaApi.enableRichPreviews(richLinksSwitch.isChecked(), new SetAttrUserListener(context));
                break;
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (isOffline(context))
            return false;

        int newStatus;
        switch (preference.getKey()) {
            case KEY_CHAT_NOTIFICATIONS_CHAT:
                updateNotifChat();
                break;

            case KEY_CHAT_STATUS:
                statusChatListPreference.setSummary(statusChatListPreference.getEntry());
                newStatus = Integer.parseInt((String) newValue);
                megaChatApi.setOnlineStatus(newStatus);
                break;

            case KEY_CHAT_SEND_ORIGINALS:
                newStatus = Integer.parseInt((String) newValue);
                if (newStatus == 0) {
                    dbH.setSendOriginalAttachments(false + "");
                    chatAttachmentsChatListPreference.setValue(newStatus + "");
                } else if (newStatus == 1) {
                    dbH.setSendOriginalAttachments(true + "");
                    chatAttachmentsChatListPreference.setValue(newStatus + "");
                }
                chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());
                break;
        }

        return true;
    }

    /**
     * Method for updating chat notifications.
     */
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

    /**
     * Method for updating the rich link previews option.
     */
    public void updateEnabledRichLinks() {
        if (MegaApplication.isEnabledRichLinks() == richLinksSwitch.isChecked())
            return;

        richLinksSwitch.setOnPreferenceClickListener(null);
        richLinksSwitch.setChecked(MegaApplication.isEnabledRichLinks());
        richLinksSwitch.setOnPreferenceClickListener(this);
    }

    /**
     * Method for updating the presence configuration.
     *
     * @param cancelled If it is cancelled
     */
    public void updatePresenceConfigChat(boolean cancelled) {
        if (!cancelled) {
            statusConfig = megaChatApi.getPresenceConfig();
        }

        showPresenceChatConfig();
    }

    /**
     * Method for showing and hiding what is needed while waiting for presence config.
     */
    private void waitPresenceConfig() {
        getPreferenceScreen().removePreference(autoAwaySwitch);
        getPreferenceScreen().removePreference(chatAutoAwayPreference);
        getPreferenceScreen().removePreference(chatPersistenceCheck);
        statusChatListPreference.setValue(MegaChatApi.STATUS_OFFLINE + "");
        statusChatListPreference.setSummary(statusChatListPreference.getEntry());
        enableLastGreenChatSwitch.setEnabled(false);
    }

    private void showPresenceChatConfig() {
        statusChatListPreference.setValue(statusConfig.getOnlineStatus() + "");
        statusChatListPreference.setSummary(statusChatListPreference.getEntry());

        if (statusConfig.getOnlineStatus() == MegaChatApi.STATUS_ONLINE ||
                statusConfig.getOnlineStatus() != MegaChatApi.STATUS_OFFLINE) {
            getPreferenceScreen().addPreference(chatPersistenceCheck);
            chatPersistenceCheck.setChecked(statusConfig.isPersist());
        }

        if (statusConfig.getOnlineStatus() != MegaChatApi.STATUS_ONLINE) {
            getPreferenceScreen().removePreference(autoAwaySwitch);
            getPreferenceScreen().removePreference(chatAutoAwayPreference);

            if (statusConfig.getOnlineStatus() == MegaChatApi.STATUS_OFFLINE) {
                getPreferenceScreen().removePreference(chatPersistenceCheck);
            }
        } else if (statusConfig.isPersist()) {
            getPreferenceScreen().removePreference(autoAwaySwitch);
            getPreferenceScreen().removePreference(chatAutoAwayPreference);
        } else {
            getPreferenceScreen().addPreference(autoAwaySwitch);
            if (statusConfig.isAutoawayEnabled()) {
                int timeout = (int) statusConfig.getAutoawayTimeout() / 60;
                autoAwaySwitch.setChecked(true);
                getPreferenceScreen().addPreference(chatAutoAwayPreference);
                chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, timeout));
            } else {
                autoAwaySwitch.setChecked(false);
                getPreferenceScreen().removePreference(chatAutoAwayPreference);
            }
        }

        enableLastGreenChatSwitch.setEnabled(true);
        if (!enableLastGreenChatSwitch.isChecked()) {
            enableLastGreenChatSwitch.setOnPreferenceClickListener(null);
            enableLastGreenChatSwitch.setChecked(statusConfig.isLastGreenVisible());
        }
        enableLastGreenChatSwitch.setOnPreferenceClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setOnlineOptions(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
        return v;
    }

    public void setOnlineOptions(boolean isOnline) {
        chatNotificationsPreference.setEnabled(isOnline);
        statusChatListPreference.setEnabled(isOnline);
        autoAwaySwitch.setEnabled(isOnline);
        chatAutoAwayPreference.setEnabled(isOnline);
        chatPersistenceCheck.setEnabled(isOnline);
        enableLastGreenChatSwitch.setEnabled(isOnline);
        chatAttachmentsChatListPreference.setEnabled(isOnline);
        richLinksSwitch.setEnabled(isOnline);
    }
}
