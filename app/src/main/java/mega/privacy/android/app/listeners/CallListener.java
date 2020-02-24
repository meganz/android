package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.CallService;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CallListener implements MegaChatCallListenerInterface {

    Context context;
    private MegaApplication application =  MegaApplication.getInstance();

    public CallListener(Context context) {
        this.context = context;
    }


    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        if (call == null || (context instanceof ChatActivityLollipop && call.getChatid() != ((ChatActivityLollipop) context).getCurrentChatid()) || (context instanceof ChatCallActivity && call.getChatid() != ((ChatCallActivity) context).getCurrentChatid())) {
            logDebug("Call null or different chat");
            return;
        }
        if (context instanceof ChatActivityLollipop && call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS) && call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            ((ChatActivityLollipop) context).cancelRecording();
        }

        if (context instanceof ChatCallActivity) {
            ((ChatCallActivity) context).updateCall(call);
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();

            logDebug("Call status has changed to " + callStatusToString(callStatus));

            if (context instanceof ChatActivityLollipop) ((ChatActivityLollipop) context).updateLayout(call);

            if (context instanceof ChatCallActivity && callStatus == MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM) ((ChatCallActivity) context).updateLocalAV();

            if (context instanceof CallService && (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT || callStatus == MegaChatCall.CALL_STATUS_RING_IN || callStatus == MegaChatCall.CALL_STATUS_JOINING || callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS))
                ((CallService) context).updateNotificationContent(call);

            if(context instanceof ManagerActivityLollipop && callStatus > MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM) ((ManagerActivityLollipop) context).checkCall(call);

            if(context instanceof AudioVideoPlayerLollipop && (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) ((AudioVideoPlayerLollipop) context).checkCall();

            if (context instanceof ChatCallActivity && callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) ((ChatCallActivity) context).checkInprogressCall(call.getId());


            if (callStatus == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || callStatus == MegaChatCall.CALL_STATUS_DESTROYED) {
                if (context instanceof ChatCallActivity) ((ChatCallActivity) context).checkTerminatingCall(call.getId(), call.getChatid());
                if (context instanceof CallService) ((CallService) context).checkDestroyCall();
            }

            if (callStatus == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                if (context instanceof ChatCallActivity) ((ChatCallActivity) context).checkUserNoPresentInCall(call.getId());
            }
        }
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags ");
            if (context instanceof ChatActivityLollipop) ((ChatActivityLollipop) context).usersWithVideo();
            if (context instanceof ChatCallActivity) ((ChatCallActivity) context).updateLocalAV();

        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
            if (call.getCallCompositionChange() == 0) {
                logDebug("No changes in the call composition");
                return;
            }
            logDebug("Changes in call composition, current call status is " + callStatusToString(call.getStatus()) + ", number of participants " + call.getPeeridParticipants().size());
            if (context instanceof ChatActivityLollipop)
                ((ChatActivityLollipop) context).usersWithVideo();
            if (context instanceof ChatCallActivity)
                ((ChatCallActivity) context).checkCompositionChanges(call);
        }
    }

    @Override
    public void onChatSessionUpdate(MegaChatApiJava api, long chatid, long callid, MegaChatSession session) {
        if (session == null || (context instanceof ChatCallActivity && chatid != ((ChatCallActivity) context).getCurrentChatid()) || (context instanceof ChatActivityLollipop && chatid != ((ChatActivityLollipop) context).getCurrentChatid())) {
            logDebug("Session null or different chat");
            return;
        }

        if (context instanceof ChatCallActivity) {
            ((ChatCallActivity) context).updateCall(api.getChatCall(chatid));
        }
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            logDebug("Changes in remote av flags or in the call composition");
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).usersWithVideo();
            }
            if (context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).updateRemoteAV(session);
            }
        }
        if (!(context instanceof ChatCallActivity)) return;

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
            ((ChatCallActivity) context).checkAudioLevel(session);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
            ((ChatCallActivity) context).checkNetworkQuality(session);
        }
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            logDebug("Session status changed, new status is " + sessionStatusToString(session.getStatus()));
            if (session.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                logDebug("term code is " + session.getTermCode());
                if (session.getTermCode() == MegaChatCall.TERM_CODE_ERROR) {
                    if (context instanceof ChatCallActivity)
                        ((ChatCallActivity) context).checkReconnectingCall(callid);
                    return;
                }
                if (session.getTermCode() == MegaChatCall.TERM_CODE_USER_HANGUP) {
                    if (context instanceof ChatCallActivity)
                        ((ChatCallActivity) context).checkTerminatingCall(callid, chatid);
                }
            }
            if (session.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                if (context instanceof ChatCallActivity)
                    ((ChatCallActivity) context).updateAVFlags(session);
            }
        }
    }
}
