package mega.privacy.android.app.meeting

import mega.privacy.android.app.MegaApplication.Companion.getInstance
import dagger.hilt.android.AndroidEntryPoint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import mega.privacy.android.app.meeting.listeners.HangChatCallListener.OnCallHungUpCallback
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener.OnCallOnHoldCallback
import javax.inject.Inject
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.call.AnswerCallUseCase
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaApiAndroid
import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import nz.mega.sdk.MegaChatCall
import mega.privacy.android.app.meeting.listeners.HangChatCallListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import mega.privacy.android.app.R
import mega.privacy.android.app.usecase.call.AnswerCallUseCase.AnswerCallResult
import mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification
import mega.privacy.android.app.utils.CallUtil.openMeetingInProgress
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.lang.IllegalArgumentException

/**
 * Service which should be for call notifications.
 *
 * @property passcodeManagement [PasscodeManagement]
 * @property rtcAudioManagerGateway [RTCAudioManagerGateway]
 * @property answerCallUseCase [AnswerCallUseCase]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property coroutineScope [CoroutineScope]
 * @property megaApi [MegaApiAndroid]
 * @property megaChatApi [MegaApiAndroid]
 * @property app [MegaApplication]
 */
@AndroidEntryPoint
class CallNotificationIntentService : Service(),
    OnCallHungUpCallback, OnCallOnHoldCallback {

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var answerCallUseCase: AnswerCallUseCase

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    /**
     * Coroutine dispatcher for camera upload work
     */
    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    var app: MegaApplication? = null

    /**
     * Coroutine Scope for camera upload work
     */
    private var coroutineScope: CoroutineScope? = null

    private var chatIdIncomingCall: Long = 0
    private var callIdIncomingCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var chatIdCurrentCall: Long = 0
    private var callIdCurrentCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var isTraditionalCall = true

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()

        coroutineScope = CoroutineScope(ioDispatcher)

        app = application as MegaApplication
    }

    /**
     * Bind service
     */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Flags: $flags, Start ID: $startId")

        if (intent == null) {
            return START_NOT_STICKY
        }

        onHandleIntent(intent)
        return START_NOT_STICKY
    }

    /**
     * Service ends
     */
    override fun onDestroy() {
        Timber.d("Service destroys.")
        super.onDestroy()
        coroutineScope?.cancel()
    }

    private fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent")
        if (intent == null) return

        intent.extras?.let { extras ->
            chatIdCurrentCall = extras.getLong(Constants.CHAT_ID_OF_CURRENT_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE)

            val currentCall = megaChatApi.getChatCall(chatIdCurrentCall)
            if (currentCall != null) {
                callIdCurrentCall = currentCall.callId
            }

            chatIdIncomingCall = extras.getLong(Constants.CHAT_ID_OF_INCOMING_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
            megaChatApi.getChatCall(chatIdIncomingCall)?.let { incomingCall ->
                callIdIncomingCall = incomingCall.callId
                clearIncomingCallNotification(callIdIncomingCall)
                megaChatApi.getChatRoom(chatIdIncomingCall)?.let { incomingCallChat ->
                    if (incomingCallChat.isMeeting) {
                        isTraditionalCall = false
                    }
                }
            }

            val action = intent.action ?: return
            Timber.d("The button clicked is : $action, currentChatId = $chatIdCurrentCall, incomingCall = $chatIdIncomingCall")

            when (action) {
                ANSWER, END_ANSWER, END_JOIN -> if (chatIdCurrentCall == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    val call = megaChatApi.getChatCall(chatIdIncomingCall)
                    if (call != null && call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                        Timber.d("Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    }
                } else {
                    if (currentCall == null) {
                        Timber.d("Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    } else {
                        Timber.d("Hanging up current call ... ")
                        megaChatApi.hangChatCall(callIdCurrentCall,
                            HangChatCallListener(this, this))
                    }
                }
                DECLINE -> {
                    Timber.d("Hanging up incoming call ... ")
                    megaChatApi.hangChatCall(callIdIncomingCall, HangChatCallListener(this, this))
                }
                IGNORE -> {
                    Timber.d("Ignore incoming call... ")
                    megaChatApi.setIgnoredCall(chatIdIncomingCall)
                    rtcAudioManagerGateway.stopSounds()
                    clearIncomingCallNotification(callIdIncomingCall)
                    stopSelf()
                }
                HOLD_ANSWER, HOLD_JOIN -> if (currentCall == null || currentCall.isOnHold) {
                    Timber.d("Answering incoming call ...")
                    answerCall(chatIdIncomingCall)
                } else {
                    Timber.d("Putting the current call on hold...")
                    megaChatApi.setCallOnHold(chatIdCurrentCall,
                        true,
                        SetCallOnHoldListener(this, this))
                }
                else -> throw IllegalArgumentException("Unsupported action: $action")
            }
        }
    }

    /**
     * Hang call
     *
     * @param callId The call id.
     */
    override fun onCallHungUp(callId: Long) {
        if (callId == callIdIncomingCall) {
            Timber.d("Incoming call hung up. ")
            clearIncomingCallNotification(callIdIncomingCall)
            stopSelf()
        } else if (callId == callIdCurrentCall) {
            Timber.d("Current call hung up. Answering incoming call ...")
            answerCall(chatIdIncomingCall)
        }
    }

    /**
     * Put call on hold
     *
     * @param chatId The chat id.
     * @param isOnHold True, if should be on hold, false otherwise.
     */
    override fun onCallOnHold(chatId: Long, isOnHold: Boolean) {
        if (chatIdCurrentCall == chatId && isOnHold) {
            Timber.d("Current call on hold. Answering incoming call ...")
            answerCall(chatIdIncomingCall)
        }
    }

    /**
     * Method for answering a call
     *
     * @param chatId Chat ID
     */
    private fun answerCall(chatId: Long) {
        answerCallUseCase.answerCall(chatId, false, isTraditionalCall, false)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { (resultChatId): AnswerCallResult, throwable: Throwable? ->
                if (resultChatId != chatIdIncomingCall) return@subscribe

                if (throwable == null) {
                    Timber.d("Incoming call answered")
                    openMeetingInProgress(this,
                        chatIdIncomingCall,
                        true,
                        passcodeManagement)
                    clearIncomingCallNotification(callIdIncomingCall)
                    stopSelf()
                } else {
                    Util.showSnackbar(getInstance().applicationContext,
                        StringResourcesUtils.getString(R.string.call_error))
                }
            }
    }

    companion object {
        const val ANSWER = "ANSWER"
        const val DECLINE = "DECLINE"
        const val HOLD_ANSWER = "HOLD_ANSWER"
        const val END_ANSWER = "END_ANSWER"
        const val IGNORE = "IGNORE"
        const val HOLD_JOIN = "HOLD_JOIN"
        const val END_JOIN = "END_JOIN"
    }
}