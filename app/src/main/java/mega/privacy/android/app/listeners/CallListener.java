package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.CallService;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CallListener implements MegaChatCallListenerInterface {

    Context context;

    public CallListener(Context context) {
        this.context = context;
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        if (call == null || (context instanceof ChatActivityLollipop && call.getChatid() != ((ChatActivityLollipop) context).getCurrentChatid()) || (context instanceof ChatCallActivity && call.getChatid() != ((ChatCallActivity) context).getCurrentChatid())) {
            logDebug("Call null or different chat");
            return;
        }
        logDebug("Call Status is " + callStatusToString(call.getStatus()));
        if (context instanceof ChatActivityLollipop && call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS) && call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            ((ChatActivityLollipop) context).cancelRecording();
        }
        if (context instanceof ChatCallActivity) {
            ((ChatCallActivity) context).updateCall(call);
        }
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            logDebug("Call status changed, current status is " + callStatusToString(call.getStatus()));
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).updateLayout(call);
            }
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).checkCall(call);
            }
            if (callStatus == MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM && context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).updateLocalAV();
            }
            if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                if (context instanceof CallService) {
                    ((CallService) context).updateNotificationContent(call);
                }
                if (context instanceof AudioVideoPlayerLollipop) {
                    ((AudioVideoPlayerLollipop) context).checkCall();
                }
            }
            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                if (context instanceof CallService) {
                    ((CallService) context).updateNotificationContent(call);
                }
                if (context instanceof AudioVideoPlayerLollipop) {
                    ((AudioVideoPlayerLollipop) context).checkCall();
                }
            }
            if (callStatus == MegaChatCall.CALL_STATUS_JOINING && context instanceof CallService) {
                ((CallService) context).updateNotificationContent(call);
            }
            if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                if (context instanceof CallService) {
                    ((CallService) context).updateNotificationContent(call);
                }
                if (context instanceof ChatCallActivity) {
                    ((ChatCallActivity) context).checkInprogressCall(call.getId());
                }
            }
            if (callStatus == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION) {
                if (context instanceof ChatCallActivity) {
                    ((ChatCallActivity) context).checkTerminatingCall(call);
                }
                if (context instanceof CallService) {
                    ((CallService) context).checkDestroyCall();
                }
            }
            if (callStatus == MegaChatCall.CALL_STATUS_DESTROYED) {
                if (context instanceof ChatCallActivity) {
                    ((ChatCallActivity) context).checkTerminatingCall(call);
                }
                if (context instanceof CallService) {
                    ((CallService) context).checkDestroyCall();
                }
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).usersWithVideo(call);
                }
            }
            if (callStatus == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).checkUserNoPresentInCall(call.getId());
            }
            if (callStatus == MegaChatCall.CALL_STATUS_RECONNECTING && context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).checkReconnectingCall(call);
            }
        }
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags ");
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).usersWithVideo(call);
            }
            if (context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).updateLocalAV();
            }
        }
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
            if (call.getCallCompositionChange() == 0) {
                logDebug("No changes in the call composition");
                return;
            }
            logDebug("Call composition changed. Call status is " + callStatusToString(call.getStatus()) + ". Num of participants is " + call.getPeeridParticipants().size());
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).usersWithVideo(call);
            }
            if (context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).checkCompositionChanges(call);
            }
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
            logDebug("Changes in remote AV flags");
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).usersWithVideo(api.getChatCall(chatid));
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
            logDebug("Session status changed, current status is " + sessionStatusToString(session.getStatus()));
            if (session.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                logDebug("Term code is " + session.getTermCode());
                if (session.getTermCode() == MegaChatCall.TERM_CODE_ERROR) {
                    if (context instanceof ChatCallActivity) {
                        ((ChatCallActivity) context).checkReconnectingCall(api.getChatCall(chatid));
                    }
                    return;
                }
                if (session.getTermCode() == MegaChatCall.TERM_CODE_USER_HANGUP && context instanceof ChatCallActivity) {
                    ((ChatCallActivity) context).checkTerminatingCall(api.getChatCall(chatid));
                }
            }
            if (session.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS && context instanceof ChatCallActivity) {
                ((ChatCallActivity) context).hideReconnecting();
                ((ChatCallActivity) context).updateAVFlags(session);

            }
        }
    }
}
