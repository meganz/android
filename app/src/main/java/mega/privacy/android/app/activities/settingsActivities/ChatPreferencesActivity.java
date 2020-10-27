package mega.privacy.android.app.activities.settingsActivities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE;
import static mega.privacy.android.app.utils.Constants.GO_OFFLINE;
import static mega.privacy.android.app.utils.Constants.GO_ONLINE;
import static mega.privacy.android.app.utils.Constants.INVALID_OPTION;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.Constants.MAX_AUTOAWAY_TIMEOUT;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatPreferencesActivity extends PreferencesBaseActivity implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaChatListenerInterface {

    private SettingsChatFragment sttChat;
    private AlertDialog newFolderDialog;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
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

    private BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)) {
                sttChat.updateNotifChat();
            }
        }
    };

    private BroadcastReceiver richLinksUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (intent.getAction().equals(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE)) {
                sttChat.updateEnabledRichLinks();
            }
        }
    };

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if (intent.getAction().equals(BROADCAST_ACTION_INTENT_STATUS_SETTING_UPDATE)) {
                boolean cancelled = intent.getBooleanExtra(PRESENCE_CANCELLED, false);
                sttChat.updatePresenceConfigChat(cancelled);
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
        registerReceiver(statusUpdateReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_STATUS_SETTING_UPDATE));
        registerReceiver(chatRoomMuteUpdateReceiver,
                new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
        if (config != null) {
            if (config.isPending()) {
                logDebug("Config is pending - do not update UI");
            } else if (sttChat != null) {
                sttChat.updatePresenceConfigChat(false);
            }
        } else {
            logWarning("Config is null");
        }
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

    }

    /**
     * Method for displaying the AutoAwayValu dialogue.
     */
    public void showAutoAwayValueDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_autoaway, null);
        builder.setView(v);

        final EditText input = v.findViewById(R.id.autoaway_edittext);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

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
        int timeout;
        try {
            timeout = Integer.parseInt(value.toString().trim());
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
            if (sttChat != null) {
                sttChat.updatePresenceConfigChat(cancelled);
            }
        } else {
            int timeout = Integer.parseInt(value);
            if (megaChatApi != null) {
                megaChatApi.setPresenceAutoaway(true, timeout * 60);
            }
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
            megaChatApi.setLastGreenVisible(enable, this);
        }
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        if (users != null) {
            for (int i = 0; i < users.size(); i++) {
                MegaUser user = users.get(i);
                if (user != null) {
                    if (user.isOwnChange() > 0) {
                        if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS)) {
                            megaApi.shouldShowRichLinkWarning(this);
                            megaApi.isRichPreviewsEnabled(this);
                        }
                    }
                } else {
                    continue;
                }
            }
        }
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: " + request.getFlag());
            } else {
                logError("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: " + e.getErrorType());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER && request.getParamType() == MegaApiJava.USER_ATTR_RICH_PREVIEWS) {
            if (e.getErrorCode() != MegaError.API_OK && sttChat != null) {
                sttChat.updateEnabledRichLinks();
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER && request.getParamType() == MegaApiJava.USER_ATTR_RICH_PREVIEWS) {
            if (e.getErrorCode() == MegaError.API_ENOENT) {
                logWarning("Attribute USER_ATTR_RICH_PREVIEWS not set");
            }
            if (request.getNumDetails() == 1) {
                MegaApplication.setShowRichLinkWarning(request.getFlag());
                MegaApplication.setCounterNotNowRichLinkWarning((int) request.getNumber());
            } else if (request.getNumDetails() == 0) {
                MegaApplication.setEnabledRichLinks(request.getFlag());
                if (sttChat != null) {
                    sttChat.updateEnabledRichLinks();
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
        unregisterReceiver(richLinksUpdateReceiver);
        unregisterReceiver(statusUpdateReceiver);
        unregisterReceiver(chatRoomMuteUpdateReceiver);
    }


}
