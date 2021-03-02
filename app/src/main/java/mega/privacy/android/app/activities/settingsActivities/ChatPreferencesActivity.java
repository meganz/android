package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class ChatPreferencesActivity extends PreferencesBaseActivity {

    private SettingsChatFragment sttChat;
    private AlertDialog newFolderDialog;

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received!");
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);

            if (actionType == GO_OFFLINE) {
                sttChat.setOnlineOptions(false);
            } else if (actionType == GO_ONLINE) {
                sttChat.setOnlineOptions(true);
            }
        }
    };

    private final BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)) {
                sttChat.updateNotifChat();
            }
        }
    };

    private final BroadcastReceiver richLinksUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (intent.getAction().equals(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE)) {
                sttChat.updateEnabledRichLinks();
            }
        }
    };

    private final BroadcastReceiver signalPresenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (megaChatApi != null && megaChatApi.getPresenceConfig() != null && !megaChatApi.getPresenceConfig().isPending()) {
                sttChat.updatePresenceConfigChat(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.section_chat).toUpperCase());
        sttChat = new SettingsChatFragment();
        replaceFragment(sttChat);

        registerReceiver(networkReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));

        registerReceiver(richLinksUpdateReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE));

        registerReceiver(chatRoomMuteUpdateReceiver,
                new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));

        registerReceiver(signalPresenceReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE));
    }

    /**
     * Method for displaying the AutoAwayValue dialogue.
     */
    public void showAutoAwayValueDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_autoaway, null);
        builder.setView(v);

        final EditText input = v.findViewById(R.id.autoaway_edittext);
        input.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = validateAutoAway(v1.getText());
                if (value != null) {
                    setAutoAwayValue(value, false);
                }
                newFolderDialog.dismiss();
                return true;
            }
            return false;
        });
        input.setImeActionLabel(getString(R.string.general_create), EditorInfo.IME_ACTION_DONE);
        input.requestFocus();

        builder.setTitle(getString(R.string.title_dialog_set_autoaway_value));
        Button set = v.findViewById(R.id.autoaway_set_button);
        set.setOnClickListener(v12 -> {
            String value = validateAutoAway(input.getText());
            if (value != null) {
                setAutoAwayValue(value, false);
            }
            newFolderDialog.dismiss();
        });

        Button cancel = v.findViewById(R.id.autoaway_cancel_button);
        cancel.setOnClickListener(v13 -> {
            setAutoAwayValue(INVALID_OPTION, true);
            newFolderDialog.dismiss();
        });

        newFolderDialog = builder.create();
        newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        newFolderDialog.show();
    }

    /**
     * Method to validate AutoAway.
     *
     * @param value The introduced string
     * @return The result string.
     */
    private String validateAutoAway(CharSequence value) {
        try {
            int timeout = Integer.parseInt(value.toString().trim());
            if (timeout <= 0) {
                timeout = 1;
            } else if (timeout > MAX_AUTOAWAY_TIMEOUT) {
                timeout = MAX_AUTOAWAY_TIMEOUT;
            }
            return String.valueOf(timeout);

        } catch (Exception e) {
            logWarning("Unable to parse user input, user entered: '" + value + "'");
            return null;
        }
    }

    /**
     * Establishing the value of Auto Away.
     *
     * @param value     The value.
     * @param cancelled If it is cancelled.
     */
    private void setAutoAwayValue(String value, boolean cancelled) {
        logDebug("Value: " + value);
        if (cancelled) {
            needUpdatePresence(cancelled);
        } else {
            int timeout = Integer.parseInt(value);
            if (megaChatApi != null) {
                megaChatApi.setPresenceAutoaway(true, timeout * 60);
            }
        }
    }

    /**
     * Method required to update presence.
     *
     * @param cancelled If it is cancelled
     */
    public void needUpdatePresence(boolean cancelled) {
        if (sttChat != null) {
            sttChat.updatePresenceConfigChat(cancelled);
        }
    }

    /**
     * Enable or disable the visibility of last seen.
     *
     * @param enable True to enable. False to disable.
     */
    public void enableLastGreen(boolean enable) {
        logDebug("Enable Last Green: " + enable);

        if (megaChatApi != null) {
            megaChatApi.setLastGreenVisible(enable, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
        unregisterReceiver(richLinksUpdateReceiver);
        unregisterReceiver(chatRoomMuteUpdateReceiver);
        unregisterReceiver(signalPresenceReceiver);
    }
}
