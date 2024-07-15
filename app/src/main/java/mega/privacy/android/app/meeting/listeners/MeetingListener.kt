package mega.privacy.android.app.meeting.listeners

import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer

import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.utils.CallUtil.callStatusToString
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MINUTE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCallListenerInterface
import nz.mega.sdk.MegaChatSession
import timber.log.Timber

class MeetingListener : MegaChatCallListenerInterface {

    var customCountDownTimer: CustomCountDownTimer? = null

    override fun onChatCallUpdate(api: MegaChatApiJava?, call: MegaChatCall?) {
        if (api == null || call == null) {
            Timber.w("MegaChatApiJava or call is null")
            return
        }

        if (MegaApplication.isLoggingOut) {
            Timber.w("Logging out")
            return
        }

        // Call status has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            Timber.d("Call status changed, current status is ${callStatusToString(call.status)}, call id is ${call.callId}. Call is Ringing ${call.isRinging}")
            checkFirstParticipant(api, call)
            if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || call.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                stopCountDown()
            }
        }

        // Call composition has changed (User added or removed from call)
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.callCompositionChange != 0) {
            Timber.d("Call composition changed. Call status is ${callStatusToString(call.status)}. Num of participants is ${call.numParticipants}")
            stopCountDown()
        }

    }

    override fun onChatSessionUpdate(
        api: MegaChatApiJava,
        chatid: Long,
        callid: Long,
        session: MegaChatSession?,
    ) {
        if (session == null) {
            Timber.w("Session is null")
            return
        }

        if (MegaApplication.isLoggingOut) {
            Timber.w("Logging out")
            return
        }
    }

    /**
     * Control when I am the only one on the call, no one has joined and more than 1 minute has expired
     *
     * @param call MegaChatCall
     * @param api MegaChatApiJava
     */
    private fun checkFirstParticipant(api: MegaChatApiJava, call: MegaChatCall) {
        api.getChatRoom(call.chatid)?.let { chat ->
            if (chat.isMeeting || chat.isGroup) {
                if (call.hasLocalAudio() && call.status == CALL_STATUS_IN_PROGRESS &&
                    MegaApplication.getChatManagement().isRequestSent(call.callId)
                ) {
                    stopCountDown()
                    if (customCountDownTimer == null) {
                        val timerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                        customCountDownTimer = CustomCountDownTimer(timerLiveData)

                        timerLiveData.observeOnce { counterState ->
                            counterState?.let { isFinished ->
                                if (isFinished) {
                                    Timber.d("Nobody has joined the group call/meeting, muted micro")
                                    customCountDownTimer = null
                                    api.disableAudio(call.chatid, null)
                                }
                            }
                        }
                    }

                    customCountDownTimer?.start(SECONDS_IN_MINUTE)
                }
            }
        }
    }

    /**
     * Stop count down timer
     */
    private fun stopCountDown() {
        customCountDownTimer?.stop()
        customCountDownTimer = null
    }
}
