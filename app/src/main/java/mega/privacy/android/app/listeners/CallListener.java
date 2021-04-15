package mega.privacy.android.app.listeners;

import android.content.Intent;

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

        if (megaApplication == null) {
            megaApplication = MegaApplication.getInstance();
        }

        Intent intentGeneral = new Intent(ACTION_UPDATE_CALL);
        intentGeneral.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
        intentGeneral.putExtra(UPDATE_CALL_ID, call.getCallId());
        megaApplication.sendBroadcast(intentGeneral);

        // Call status has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            logDebug("Call status changed, current status is " + callStatusToString(callStatus));
            Intent intentStatus = new Intent(ACTION_CALL_STATUS_UPDATE);
            intentStatus.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intentStatus.putExtra(UPDATE_CALL_ID, call.getCallId());
            intentStatus.putExtra(UPDATE_CALL_STATUS, callStatus);
            intentStatus.putExtra(CALL_IS_OUTGOING, call.isOutgoing());
            intentStatus.putExtra(CALL_IS_RINGING, call.isRinging());
            if (callStatus == MegaChatCall.CALL_STATUS_DESTROYED) {
                intentStatus.putExtra(UPDATE_CALL_TERM_CODE, call.getTermCode());
                intentStatus.putExtra(UPDATE_CALL_IGNORE, call.isIgnored());
                //intentStatus.putExtra(UPDATE_CALL_LOCAL_TERM_CODE, call.isLocalTermCode());
            }
            megaApplication.sendBroadcast(intentStatus);
        }

        // Local audio/video flags has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags ");
            Intent intentLocalFlags = new Intent(ACTION_CHANGE_LOCAL_AVFLAGS);
            intentLocalFlags.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intentLocalFlags.putExtra(UPDATE_CALL_ID, call.getCallId());
            megaApplication.sendBroadcast(intentLocalFlags);
        }

        // Peer has changed its ringing state
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)) {
            logDebug("Changes in ringing status call:: call.isRinging() = "+call.isRinging());
            Intent intent = new Intent(ACTION_CHANGE_RINGING_STATUS);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            intent.putExtra(CALL_IS_RINGING, call.isRinging());
            intent.putExtra(UPDATE_CALL_STATUS, call.getStatus());
            megaApplication.sendBroadcast(intent);
        }

        // Call composition has changed (User added or removed from call)
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.getCallCompositionChange() != 0) {
            logDebug("Call composition changed. Call status is " + callStatusToString(call.getStatus()) + ". Num of participants is " + call.getPeeridParticipants().size());
            Intent intent = new Intent(ACTION_CHANGE_COMPOSITION);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            intent.putExtra(TYPE_CHANGE_COMPOSITION, call.getCallCompositionChange());
            intent.putExtra(UPDATE_PEER_ID, call.getPeeridCallCompositionChange());
            megaApplication.sendBroadcast(intent);
        }

        // Call is set onHold
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD)) {
            logDebug("Call on hold changed ");
            Intent intent = new Intent(ACTION_CHANGE_CALL_ON_HOLD);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            megaApplication.sendBroadcast(intent);
        }

        // Speak has been enabled
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_SPEAK)) {
            logDebug("Call speak changed ");
            Intent intent = new Intent(ACTION_CHANGE_CALL_SPEAK);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            megaApplication.sendBroadcast(intent);
        }

        // Indicates if we are speaking
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL)) {
            logDebug("Local audio level changed ");
            Intent intent = new Intent(ACTION_CHANGE_LOCAL_AUDIO_LEVEL);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            megaApplication.sendBroadcast(intent);
        }

        // Network quality has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY)) {
            logDebug("Network quality changed ");
            Intent intent = new Intent(ACTION_CHANGE_NETWORK_QUALITY);
            intent.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intent.putExtra(UPDATE_CALL_ID, call.getCallId());
            megaApplication.sendBroadcast(intent);
        }
    }

    @Override
    public void onChatSessionUpdate(MegaChatApiJava api, long chatid, long callid, MegaChatSession session) {
        if (session == null) {
            logWarning("Session null");
            return;
        }

        // Session status has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            logDebug("Session status changed, current status is " + sessionStatusToString(session.getStatus()));
            Intent intent = new Intent(ACTION_SESSION_STATUS_UPDATE);
            intent.putExtra(UPDATE_SESSION_STATUS, session.getStatus());

//            if (session.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
//                logDebug("Term code is " + session.getTermCode());
//                intentStatus.putExtra(UPDATE_SESSION_TERM_CODE, session.getTermCode());
//            }

            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Remote audio/video flags has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            logDebug("Changes in remote av flags ");
            Intent intent = new Intent(ACTION_CHANGE_REMOTE_AVFLAGS);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Session speak requested
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED)) {
            Intent intent = new Intent(ACTION_CHANGE_SESSION_SPEAK_REQUESTED);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Hi-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES)) {
            Intent intent = new Intent(ACTION_CHANGE_SESSION_ON_HIRES);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Low-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES)) {
            Intent intent = new Intent(ACTION_CHANGE_SESSION_ON_LOWRES);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Session is on hold
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD)) {
            logDebug("Session on hold changed ");
            Intent intent = new Intent(ACTION_CHANGE_SESSION_ON_HOLD);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }

        // Indicates if peer is speaking
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL)) {
            logDebug("Remote audio level changed ");
            Intent intent = new Intent(ACTION_CHANGE_REMOTE_AUDIO_LEVEL);
            intent.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intent.putExtra(UPDATE_CALL_ID, callid);
            intent.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intent.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            megaApplication.sendBroadcast(intent);
        }
    }
}
