package mega.privacy.android.app.listeners;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class CallListener implements MegaChatCallListenerInterface {

    private MegaApplication megaApplication;

    public CallListener() {
        megaApplication = MegaApplication.getInstance();
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        if (call == null) {
            logWarning("Call null");
            return;
        }

        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CALL_UPDATE);

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            logDebug("Call status changed, current status is " + callStatusToString(callStatus));
            intent.setAction(ACTION_CALL_STATUS_UPDATE);
            intent.putExtra(UPDATE_CALL_STATUS, callStatus);
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags ");
            intent.setAction(ACTION_CHANGE_LOCAL_AVFLAGS);
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
            if (call.getCallCompositionChange() == 0) {
                logDebug("No changes in the call composition");
                return;
            }

            logDebug("Call composition changed. Call status is " + callStatusToString(call.getStatus()) + ". Num of participants is " + call.getPeeridParticipants().size());
            intent.setAction(ACTION_CHANGE_COMPOSITION);
        }

        intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
        intent.putExtra(UPDATE_CALL_ID, call.getId());
        LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intent);
    }

    @Override
    public void onChatSessionUpdate(MegaChatApiJava api, long chatid, long callid, MegaChatSession session) {
        if (session == null) {
            logDebug("Session null");
            return;
        }

        Intent intent = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            intent.setAction(ACTION_CHANGE_REMOTE_AVFLAGS);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
            intent.setAction(ACTION_CHANGE_AUDIO_LEVEL);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
            intent.setAction(ACTION_CHANGE_NETWORK_QUALITY);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            logDebug("Session status changed, current status is " + sessionStatusToString(session.getStatus()));
            intent.setAction(ACTION_SESSION_STATUS_UPDATE);
            intent.putExtra(UPDATE_SESSION_STATUS, session.getStatus());

            if (session.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                logDebug("Term code is " + session.getTermCode());
                intent.putExtra(UPDATE_SESSION_TERM_CODE, session.getTermCode());
            }
        }

        intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
        intent.putExtra(UPDATE_CALL_ID, callid);
        intent.putExtra(UPDATE_SESSION_PEER_ID, session.getPeerid());
        intent.putExtra(UPDATE_SESSION_CLIENT_ID, session.getClientid());
        LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intent);
    }
}
