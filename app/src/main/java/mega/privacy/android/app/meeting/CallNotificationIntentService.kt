package mega.privacy.android.app.meeting

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.HangChatCallListener
import mega.privacy.android.app.meeting.listeners.HangChatCallListener.OnCallHungUpCallback
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener.OnCallOnHoldCallback
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification
import mega.privacy.android.app.utils.CallUtil.openMeetingInProgress
import mega.privacy.android.app.utils.CallUtil.openMeetingRinging
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.meeting.StartScheduledMeetingUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import javax.inject.Inject

/**
 * Service which should be for call notifications.
 *
 * @property passcodeManagement                     [PasscodeManagement]
 * @property rtcAudioManagerGateway                 [RTCAudioManagerGateway]
 * @property answerChatCallUseCase                  [AnswerChatCallUseCase]
 * @property startScheduledMeetingUseCase           [StartScheduledMeetingUseCase]
 * @property startMeetingInWaitingRoomChatUseCase   [StartMeetingInWaitingRoomChatUseCase]
 * @property getChatRoomUseCase                     [GetChatRoom]
 * @property notificationManager                    [NotificationManagerCompat]
 * @property ioDispatcher                           [CoroutineDispatcher]
 * @property coroutineScope                         [CoroutineScope]
 * @property megaApi                                [MegaApiAndroid]
 * @property megaChatApi                            [MegaApiAndroid]
 * @property app                                    [MegaApplication]
 * @property megaChatApiGateway                     [MegaChatApiGateway]
 */
@AndroidEntryPoint
class CallNotificationIntentService : Service(),
    OnCallHungUpCallback, OnCallOnHoldCallback {

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var answerChatCallUseCase: AnswerChatCallUseCase

    @Inject
    lateinit var startScheduledMeetingUseCase: StartScheduledMeetingUseCase

    @Inject
    lateinit var startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var getChatRoomUseCase: GetChatRoom

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

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

    @Inject
    lateinit var megaChatApiGateway: MegaChatApiGateway

    var app: MegaApplication? = null

    private val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ioDispatcher)
    }

    private var chatIdIncomingCall: Long = 0
    private var callIdIncomingCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var chatIdCurrentCall: Long = 0
    private var callIdCurrentCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var isTraditionalCall = true
    private var schedIdIncomingCall: Long = MegaChatApiJava.MEGACHAT_INVALID_HANDLE


    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()

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
        coroutineScope.cancel()
    }

    private fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent")
        if (intent == null) return

        intent.extras?.let { extras ->
            chatIdCurrentCall = extras.getLong(
                Constants.CHAT_ID_OF_CURRENT_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )

            val currentCall = megaChatApi.getChatCall(chatIdCurrentCall)
            if (currentCall != null) {
                callIdCurrentCall = currentCall.callId
            }

            chatIdIncomingCall = extras.getLong(
                Constants.CHAT_ID_OF_INCOMING_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )

            schedIdIncomingCall = extras.getLong(
                Constants.SCHEDULED_MEETING_ID,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )

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
                    } else {
                        Timber.d("Hanging up current call ... ")
                        megaChatApi.hangChatCall(
                            call.callId,
                            HangChatCallListener(this, this)
                        )
                    }
                } else {
                    if (currentCall == null) {
                        Timber.d("Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    } else {
                        Timber.d("Hanging up current call ... ")
                        megaChatApi.hangChatCall(
                            callIdCurrentCall,
                            HangChatCallListener(this, this)
                        )
                    }
                }

                START_SCHED_MEET -> {
                    processMeetingCall()
                    notificationManager.cancel(chatIdIncomingCall.toInt())
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
                    megaChatApi.setCallOnHold(
                        chatIdCurrentCall,
                        true,
                        SetCallOnHoldListener(this, this)
                    )
                }

                else -> throw IllegalArgumentException("Unsupported action: $action")
            }
        }
    }

    /**
     * Process Meeting Chat Call
     */
    private fun processMeetingCall() {
        coroutineScope.launch {
            runCatching {
                val chatRoom = getChatRoomUseCase(chatIdIncomingCall)
                val isWaitingRoom = chatRoom?.isWaitingRoom ?: false
                val isHost = chatRoom?.ownPrivilege == ChatRoomPermission.Moderator
                if (isWaitingRoom && !isHost) {
                    openWaitingRoom(chatIdIncomingCall)
                } else {
                    runCatching {
                        if (isWaitingRoom) {
                            requireNotNull(
                                startMeetingInWaitingRoomChatUseCase(
                                    chatId = chatIdIncomingCall,
                                    schedIdWr = schedIdIncomingCall,
                                    enabledVideo = false,
                                    enabledAudio = false,
                                )
                            )
                        } else {
                            requireNotNull(
                                startScheduledMeetingUseCase(
                                    chatId = chatIdIncomingCall,
                                    schedId = schedIdIncomingCall,
                                    enableVideo = false,
                                    enableAudio = false
                                )
                            )
                        }
                    }.onSuccess { call ->
                        if (call.chatId != megaChatApiGateway.getChatInvalidHandle()) {
                            openMeetingInProgress(
                                this@CallNotificationIntentService,
                                call.chatId,
                                true,
                                passcodeManagement
                            )
                        }
                        stopSelf()
                    }.onFailure { error ->
                        Timber.e(error)
                        stopSelf()
                    }
                }
            }.onSuccess {
                stopSelf()
            }.onFailure { error ->
                Timber.e(error)
                stopSelf()
            }
        }
    }

    /**
     * Open the waiting room
     *
     * @param chatId    Meeting's Chat ID
     */
    private fun openWaitingRoom(chatId: Long) {
        val intent = Intent(applicationContext, WaitingRoomActivity::class.java).apply {
            putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        applicationContext.startActivity(intent)
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
        if (!hasPermissions(this@CallNotificationIntentService, Manifest.permission.RECORD_AUDIO)) {
            openMeetingRinging(
                this@CallNotificationIntentService,
                chatIdIncomingCall,
                passcodeManagement
            )
            clearIncomingCallNotification(callIdIncomingCall)
            stopSelf()
            return
        }

        coroutineScope.launch {
            runCatching {
                answerChatCallUseCase(chatId = chatId, video = false, audio = isTraditionalCall)
            }.onSuccess { call ->
                call.apply {
                    chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                        ?.let { callChatId ->
                            openMeetingInProgress(
                                this@CallNotificationIntentService,
                                chatIdIncomingCall,
                                true,
                                passcodeManagement
                            )
                            clearIncomingCallNotification(callChatId)
                            stopSelf()
                        }
                }
                coroutineScope.cancel()
            }.onFailure { Timber.w("Exception answering call: $it") }
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
        const val START_SCHED_MEET = "START_SCHED_MEET"
    }
}