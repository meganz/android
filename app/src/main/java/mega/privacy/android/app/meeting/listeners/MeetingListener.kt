package mega.privacy.android.app.meeting.listeners

import android.util.Pair
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_SPEAK_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_RINGING_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HIRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_LOWRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_SPEAK_REQUESTED
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_CALL
import mega.privacy.android.app.utils.CallUtil.callStatusToString
import mega.privacy.android.app.utils.CallUtil.sessionStatusToString
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MINUTE
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCallListenerInterface
import nz.mega.sdk.MegaChatSession

class MeetingListener : MegaChatCallListenerInterface {

    val timerLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val customCountDownTimer = CustomCountDownTimer(timerLiveData)

    override fun onChatCallUpdate(api: MegaChatApiJava?, call: MegaChatCall?) {
        if (api == null || call == null) {
            logWarning("MegaChatApiJava or call is null")
            return
        }

        if (MegaApplication.isLoggingOut()) {
            logWarning("Logging out")
            return
        }

        sendCallEvent(EVENT_UPDATE_CALL, call)

        // Call status has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            logDebug("Call status changed, current status is ${callStatusToString(call.status)}, call id is ${call.callId}. Call is Ringing ${call.isRinging}")
            sendCallEvent(EVENT_CALL_STATUS_CHANGE, call)
            checkFirstParticipant(api, call)
            if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || call.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                cancelCountDown()
            }
        }

        // Local audio/video flags has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags. Audio enable ${call.hasLocalAudio()}, Video enable ${call.hasLocalVideo()}")
            sendCallEvent(EVENT_LOCAL_AVFLAGS_CHANGE, call)
        }

        // Peer has changed its ringing state
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)) {
            logDebug("Changes in ringing status call. Call is ${call.callId}. Call is Ringing ${call.isRinging}")
            sendCallEvent(EVENT_RINGING_STATUS_CHANGE, call)
        }

        // Call composition has changed (User added or removed from call)
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.callCompositionChange != 0) {
            logDebug("Call composition changed. Call status is ${callStatusToString(call.status)}. Num of participants is ${call.numParticipants}")
            sendCallEvent(EVENT_CALL_COMPOSITION_CHANGE, call)
            checkLastParticipant(api, call)
        }

        // Call is set onHold
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD)) {
            logDebug("Call on hold changed")
            sendCallEvent(EVENT_CALL_ON_HOLD_CHANGE, call)
        }

        // Speak has been enabled
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_SPEAK)) {
            logDebug("Call speak changed")
            sendCallEvent(EVENT_CALL_SPEAK_CHANGE, call)
        }

        // Indicates if we are speaking
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL)) {
            logDebug("Local audio level changed")
            sendCallEvent(EVENT_LOCAL_AUDIO_LEVEL_CHANGE, call)
        }

        // Network quality has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY)) {
            logDebug("Network quality changed")
            sendCallEvent(EVENT_LOCAL_NETWORK_QUALITY_CHANGE, call)
        }
    }

    override fun onChatSessionUpdate(
        api: MegaChatApiJava,
        chatid: Long,
        callid: Long,
        session: MegaChatSession?
    ) {
        if (session == null) {
            logWarning("Session is null")
            return
        }

        if (MegaApplication.isLoggingOut()) {
            logWarning("Logging out")
            return
        }

        val call = api.getChatCallByCallId(callid)
        call?.let {
            sendCallEvent(EVENT_UPDATE_CALL, it)
        }

        // Session status has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            cancelCountDown()
            logDebug("Session status changed, current status is ${sessionStatusToString(session.status)}, of participant with clientID ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_STATUS_CHANGE, session, callid)
        }

        // Remote audio/video flags has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            logDebug("Changes in remote av flags. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_REMOTE_AVFLAGS_CHANGE, session, callid)
        }

        // Session speak requested
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED)) {
            logDebug("Changes in speak requested. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_SPEAK_REQUESTED, session, callid)
        }

        // Hi-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES)) {
            logDebug("Session on high resolution changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_HIRES_CHANGE, session, callid)
        }

        // Low-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES)) {
            logDebug("Session on low resolution changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_LOWRES_CHANGE, session, callid)
        }

        // Session is on hold
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD)) {
            logDebug("Session on hold changed. Session on hold ${session.isOnHold}. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_HOLD_CHANGE, session, callid)
        }

        // Indicates if peer is speaking
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL)) {
            logDebug("Remote audio level changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_REMOTE_AUDIO_LEVEL_CHANGE, session, callid)
        }
    }

    private fun sendCallEvent(type: String, call: MegaChatCall) {
        LiveEventBus.get(
            type,
            MegaChatCall::class.java
        ).post(call)
    }

    private fun sendSessionEvent(type: String, session: MegaChatSession, callId: Long) {
        val sessionAndCall = Pair.create(callId, session)
        LiveEventBus.get(
            type,
            Pair::class.java
        ).post(sessionAndCall)
    }

    /**
     * Control when I am the last participant in the call and the microphone should be muted
     *
     * @param call MegaChatCall
     * @param api MegaChatApiJava
     */
    private fun checkLastParticipant(api: MegaChatApiJava, call: MegaChatCall) {
        if (call.hasLocalAudio() && call.callCompositionChange == TYPE_LEFT &&
            call.numParticipants == 1 &&
            call.peeridParticipants.get(0) == api.myUserHandle
        ) {
            api.getChatRoom(call.chatid)?.let { chat ->
                if (chat.isMeeting || chat.isGroup) {
                    api.disableAudio(call.chatid, null)
                }
            }
        }
    }

    /**
     * Control when I am the only one on the call, no one has joined and more than 1 minute has expired
     *
     * @param call MegaChatCall
     * @param api MegaChatApiJava
     */
    private fun checkFirstParticipant(api: MegaChatApiJava, call: MegaChatCall) {
        if (call.hasLocalAudio() && call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS &&
            MegaApplication.getChatManagement().isRequestSent(call.callId)
        ) {
            api.getChatRoom(call.chatid)?.let { chat ->
                if (chat.isMeeting || chat.isGroup) {
                    customCountDownTimer.start(SECONDS_IN_MINUTE)
                    customCountDownTimer.mutableLiveData.observeForever{ counterState ->
                        counterState?.let { isFinished ->
                            if (isFinished) {
                                api.disableAudio(call.chatid, null)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
    * Cancel count down timer
    */
    private fun cancelCountDown(){
        customCountDownTimer.stop()
    }
}
