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
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification
import mega.privacy.android.app.utils.CallUtil.openMeetingInProgress
import mega.privacy.android.app.utils.CallUtil.openMeetingRinging
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.SetIgnoredCallUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.SetFakeIncomingCallStateUseCase
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
 * @property getChatRoomUseCase                     [GetChatRoomUseCase]
 * @property getChatCallUseCase                     [GetChatCallUseCase]
 * @property notificationManager                    [NotificationManagerCompat]
 * @property ioDispatcher                           [CoroutineDispatcher]
 * @property coroutineScope                         [CoroutineScope]
 * @property megaApi                                [MegaApiAndroid]
 * @property megaChatApi                            [MegaApiAndroid]
 * @property app                                    [MegaApplication]
 * @property megaChatApiGateway                     [MegaChatApiGateway]
 * @property holdChatCallUseCase                    [HoldChatCallUseCase]
 * @property hangChatCallUseCase                    [HangChatCallUseCase]
 * @property setIgnoredCallUseCase                  [SetIgnoredCallUseCase]
 * @property setFakeIncomingCallStateUseCase        [SetFakeIncomingCallStateUseCase]
 */
@AndroidEntryPoint
class CallNotificationIntentService : Service() {

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
    lateinit var getChatRoomUseCase: GetChatRoomUseCase

    @Inject
    lateinit var getChatCallUseCase: GetChatCallUseCase

    @Inject
    lateinit var holdChatCallUseCase: HoldChatCallUseCase

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var hangChatCallUseCase: HangChatCallUseCase

    @Inject
    lateinit var setIgnoredCallUseCase: SetIgnoredCallUseCase

    @Inject
    lateinit var setFakeIncomingCallStateUseCase: SetFakeIncomingCallStateUseCase

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
        val action = intent.action ?: return

        if (action == DISMISS) {
            rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
        }

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


            Timber.d("The button clicked is : $action, currentChatId = $chatIdCurrentCall, incomingCall = $chatIdIncomingCall")

            when (action) {
                ANSWER, END_ANSWER, END_JOIN -> if (chatIdCurrentCall == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    val call = megaChatApi.getChatCall(chatIdIncomingCall)
                    if (call != null && call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                        Timber.d("Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    } else {
                        Timber.d("Hanging up current call ... ")
                        hangChatCall(call.callId)
                    }
                } else {
                    if (currentCall == null) {
                        Timber.d("Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    } else {
                        Timber.d("Hanging up current call ... ")
                        hangChatCall(callIdCurrentCall)
                    }
                }

                START_SCHED_MEET -> {
                    processMeetingCall()
                    notificationManager.cancel(chatIdIncomingCall.toInt())
                }

                DECLINE -> {
                    Timber.d("Hanging up incoming call ... ")
                    hangChatCall(callIdIncomingCall)
                }

                IGNORE -> {
                    Timber.d("Ignore incoming call... ")
                    ignoreCall(chatIdIncomingCall)
                }

                DISMISS -> {
                    setFakeIncomingCall(
                        chatId = chatIdIncomingCall, FakeIncomingCallState.Dismiss
                    )
                    stopSelf()
                }

                HOLD_ANSWER, HOLD_JOIN -> if (currentCall == null || currentCall.isOnHold) {
                    Timber.d("Answering incoming call ...")
                    answerCall(chatIdIncomingCall)
                } else {
                    Timber.d("Set the current call on hold...")
                    setCallOnHold(chatIdCurrentCall)
                }

                else -> throw IllegalArgumentException("Unsupported action: $action")
            }
        }
    }

    /**
     * Set fake incoming call
     *
     * @param chatId Chat id
     * @param type  [FakeIncomingCallState]
     */
    private fun setFakeIncomingCall(chatId: Long, type: FakeIncomingCallState) {
        coroutineScope.launch {
            runCatching {
                setFakeIncomingCallStateUseCase(chatId = chatId, type = type)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Set call on hold
     *
     * @param currentChatId Chat id
     */
    private fun setCallOnHold(currentChatId: Long) {
        coroutineScope.launch {
            runCatching {
                holdChatCallUseCase(chatId = currentChatId, setOnHold = true)
            }.onSuccess {
                it?.apply {
                    if (chatIdCurrentCall == chatId && isOnHold) {
                        Timber.d("Current call on hold. Answering incoming call ...")
                        answerCall(chatIdIncomingCall)
                    }
                }
            }.onFailure { error ->
                Timber.e(error)
            }
        }
    }

    /**
     * Hang chat call
     *
     * @param callId Call id
     */
    private fun hangChatCall(callId: Long) {
        coroutineScope.launch {
            runCatching {
                hangChatCallUseCase(callId = callId)
            }.onSuccess {
                it?.let { call ->
                    when (call.callId) {
                        callIdIncomingCall -> {
                            Timber.d("Incoming call hung up. ")
                            clearIncomingCallNotification(callIdIncomingCall)
                            setFakeIncomingCall(
                                chatId = call.chatId,
                                type = FakeIncomingCallState.Remove
                            )

                            stopSelf()
                        }

                        callIdCurrentCall -> {
                            Timber.d("Current call hung up. Answering incoming call ...")
                            answerCall(chatIdIncomingCall)
                        }
                    }
                }
            }.onFailure { error ->
                Timber.e(error)
            }
        }
    }

    /**
     * Ignore chat call
     *
     * @param chatId Call id
     */
    private fun ignoreCall(chatId: Long) {
        coroutineScope.launch {
            runCatching {
                setIgnoredCallUseCase(chatId = chatId)
            }.onSuccess {
                rtcAudioManagerGateway.stopSounds()
                clearIncomingCallNotification(callIdIncomingCall)
                setFakeIncomingCall(chatId = chatId, type = FakeIncomingCallState.Remove)
                stopSelf()
            }.onFailure { error ->
                Timber.e(error)
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
                val call = getChatCallUseCase(chatIdIncomingCall)
                val audioPermission = hasPermissions(
                    this@CallNotificationIntentService,
                    Manifest.permission.RECORD_AUDIO
                )

                if (isWaitingRoom && !isHost) {
                    openWaitingRoom(chatIdIncomingCall)
                } else if (audioPermission.not()) {
                    if (call == null) {
                        openChatRoom(chatId = chatIdIncomingCall)
                    } else {
                        openMeetingRinging(
                            this@CallNotificationIntentService,
                            chatIdIncomingCall,
                            passcodeManagement
                        )
                    }

                    stopSelf()
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
                    }.onSuccess { meeting ->
                        if (meeting.chatId != megaChatApiGateway.getChatInvalidHandle()) {
                            openMeetingInProgress(
                                this@CallNotificationIntentService,
                                meeting.chatId,
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
     * Open the waiting room
     *
     * @param chatId    Meeting's Chat ID
     */
    private fun openChatRoom(chatId: Long) {
        val intent = Intent(applicationContext, ChatHostActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, chatId)
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        applicationContext.startActivity(intent)
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
            }.onFailure {
                Timber.w("Exception answering call: $it")
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
        const val START_SCHED_MEET = "START_SCHED_MEET"
        const val DISMISS = "DISMISS"
    }
}