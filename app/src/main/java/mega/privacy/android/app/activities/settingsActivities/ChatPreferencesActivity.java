package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatFragment;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.MAX_AUTOAWAY_TIMEOUT;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatPreferencesActivity extends PreferencesBaseActivity implements MegaChatRequestListenerInterface, MegaRequestListenerInterface {

    private SettingsChatFragment sttChat;
    private AlertDialog newFolderDialog;

    private BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if(intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)){
                sttChat.updateNotifChat();
            }
        }
    };

    private BroadcastReceiver richLinksUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if(intent.getAction().equals(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE)){
                sttChat.updateEnabledRichLinks();
            }
        }
    };

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttChat == null)
                return;

            if(intent.getAction().equals(BROADCAST_ACTION_INTENT_STATUS_SETTING_UPDATE)){
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
        registerReceiver(richLinksUpdateReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE));
        registerReceiver(statusUpdateReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_STATUS_SETTING_UPDATE));
        registerReceiver(chatRoomMuteUpdateReceiver, new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));
    }

    public void showAutoAwayValueDialog(){
        logDebug("showAutoAwayValueDialog");

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
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String value = validateAutoAway(v.getText());
                    if (value != null) {
                        setAutoAwayValue(value, false);
                    }
                    newFolderDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
        input.requestFocus();

        builder.setTitle(getString(R.string.title_dialog_set_autoaway_value));
        Button set = (Button) v.findViewById(R.id.autoaway_set_button);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = validateAutoAway(input.getText());
                if (value != null) {
                    setAutoAwayValue(value, false);
                }
                newFolderDialog.dismiss();
            }
        });
        Button cancel = (Button) v.findViewById(R.id.autoaway_cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoAwayValue("-1", true);
                newFolderDialog.dismiss();
            }
        });

        newFolderDialog = builder.create();
        newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        newFolderDialog.show();
    }

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

    private void setAutoAwayValue(String value, boolean cancelled){
        logDebug("Value: " + value);
        if(cancelled){
            Intent intentPresence = new Intent(BROADCAST_ACTION_INTENT_STATUS_SETTING_UPDATE);
            intentPresence.putExtra(PRESENCE_CANCELLED, true);
            sendBroadcast(intentPresence);
        }
        else{
            int timeout = Integer.parseInt(value);
            if(megaChatApi!=null){
                megaChatApi.setPresenceAutoaway(true, timeout*60);
            }
        }
    }

    public void enableLastGreen(boolean enable){
        logDebug("Enable Last Green: "+ enable);

        if(megaChatApi!=null){
            megaChatApi.setLastGreenVisible(enable, this);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(richLinksUpdateReceiver);
        unregisterReceiver(statusUpdateReceiver);
        unregisterReceiver(chatRoomMuteUpdateReceiver);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if(request.getType() == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: " + request.getFlag());
            }
            else{
                logError("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: " + e.getErrorType());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) { }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER && request.getParamType() == MegaApiJava.USER_ATTR_RICH_PREVIEWS) {
            logDebug("change isRickLinkEnabled - USER_ATTR_RICH_PREVIEWS finished");
            if (e.getErrorCode() != MegaError.API_OK){
                logError("ERROR:USER_ATTR_RICH_PREVIEWS");
                sttChat.updateEnabledRichLinks();
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
