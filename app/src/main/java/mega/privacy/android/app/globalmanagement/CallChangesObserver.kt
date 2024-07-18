package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ChatSessionUpdatesResult
import mega.privacy.android.domain.entity.call.EndCallReason
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainImmediateDispatcher
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.call.GetCallHandleListUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaHandleList
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call status handler
 *
 * @property rtcAudioManagerGateway
 * @property megaChatApi
 * @property application
 * @constructor Create empty Call status handler
 */
@Singleton
class CallChangesObserver @Inject constructor(
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val megaChatApi: MegaChatApiAndroid,
    private val application: Application,
    private val chatManagement: ChatManagement,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val passcodeManagement: PasscodeManagement,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getCallHandleListUseCase: GetCallHandleListUseCase,
    private val isChatNotifiableUseCase: IsChatNotifiableUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher,
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private var openCallChatId: Long = -1

    /**
     * listen all call status observer
     */
    fun init() {
        applicationScope.launch {
            monitorChatCallUpdatesUseCase()
                .catch { e -> Timber.e(e, "Error listening call updates") }
                .collect { call ->
                    runCatching {
                        withContext(mainImmediateDispatcher) {
                            val changes = call.changes.orEmpty()
                            when {
                                changes.contains(ChatCallChanges.Status)
                                -> onHandleCallStatusChange(call)

                                changes.contains(ChatCallChanges.RingingStatus)
                                -> handleCallRinging(call)

                                changes.contains(ChatCallChanges.CallComposition)
                                -> handleCallComposition(call)
                            }
                        }
                    }.onFailure {
                        Timber.e(it, "Error handling call status change")
                    }
                }
        }
        applicationScope.launch {
            monitorChatSessionUpdatesUseCase()
                .catch { e -> Timber.e(e, "Error listening session updates") }
                .collect { callAndSession ->
                    runCatching {
                        handleSessionStatusChange(callAndSession)
                    }.onFailure {
                        Timber.e(it, "Error handling session status change")
                    }
                }
        }
    }

    private suspend fun handleCallComposition(call: ChatCall) {
        val numberOfParticipants = call.numParticipants ?: return
        if (call.callCompositionChange == CallCompositionChanges.Added && numberOfParticipants > 1) {
            Timber.d("Handle call")
            if (getMyUserHandleUseCase() == call.peerIdCallCompositionChange) {
                CallUtil.clearIncomingCallNotification(call.callId)
                chatManagement.removeValues(call.chatId)
            }
        }
    }

    private suspend fun onHandleCallStatusChange(call: ChatCall) {
        val callStatus = call.status
        val isOutgoing = call.isOutgoing
        val isRinging = call.isRinging
        val callId = call.callId
        val chatId = call.chatId
        if (chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE || callId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error in chatId or callId")
            return
        }
        Timber.d("Call status is ${callStatus}, chat id is $chatId, call id is $callId")
        when (call.status) {
            ChatCallStatus.Connecting -> {
                if (isOutgoing && chatManagement.isRequestSent(callId)) {
                    rtcAudioManagerGateway.removeRTCAudioManager()
                }
            }

            ChatCallStatus.UserNoPresent -> {
                val listAllCalls =
                    megaChatApi.chatCalls?.takeIf { it.size() > 0 } ?: return
                if (isRinging) {
                    Timber.d("Is incoming call")
                    incomingCall(listAllCalls, chatId, callStatus)
                } else {
                    val chatRoom = megaChatApi.getChatRoom(chatId)
                    if (chatRoom != null && chatRoom.isGroup && !chatRoom.isMeeting) {
                        Timber.d("Check if the incoming group call notification should be displayed")
                        chatManagement.checkActiveGroupChat(chatId)
                    }
                }
            }

            ChatCallStatus.Joining, ChatCallStatus.InProgress -> {
                megaChatApi.chatCalls?.takeIf { it.size() > 0 } ?: return
                chatManagement.addNotificationShown(chatId)
                Timber.d("Is ongoing call")
                CallUtil.ongoingCall(
                    rtcAudioManagerGateway,
                    chatId,
                    callId,
                    if (isOutgoing && chatManagement
                            .isRequestSent(callId)
                    ) Constants.AUDIO_MANAGER_CALL_OUTGOING else Constants.AUDIO_MANAGER_CALL_IN_PROGRESS
                )
            }

            ChatCallStatus.TerminatingUserParticipation -> {
                Timber.d(
                    "The user participation in the call has ended. The termination code is ${call.termCode}"
                )
                chatManagement.controlCallFinished(callId, chatId)
            }

            ChatCallStatus.Destroyed -> {
                Timber.d("Call has ended. End call reason is ${call.endCallReason}")
                chatManagement.controlCallFinished(callId, chatId)
                checkCallDestroyed(
                    chatId = chatId,
                    callId = callId,
                    endCallReason = call.endCallReason,
                    isIgnored = call.isIgnored
                )
            }

            else -> Unit
        }
    }

    private suspend fun handleCallRinging(call: ChatCall) {
        val callStatus = call.status
        val isRinging = call.isRinging
        val listAllCalls = megaChatApi.chatCalls
        if (listAllCalls == null || listAllCalls.size() == 0L) {
            Timber.e("Calls not found")
            return
        }
        if (isRinging) {
            Timber.d("Is incoming call")
            incomingCall(listAllCalls, call.chatId, callStatus)
        } else {
            CallUtil.clearIncomingCallNotification(call.callId)
            chatManagement.removeValues(call.chatId)
        }
    }

    private suspend fun handleSessionStatusChange(callAndSession: ChatSessionUpdatesResult) {
        val session = callAndSession.session
        val sessionStatus = session?.status
        callAndSession.call?.apply {
            megaChatApi.getChatRoom(chatId)?.let { room ->
                if (sessionStatus == ChatSessionStatus.Progress &&
                    (room.isGroup || room.isMeeting || session.peerId != getMyUserHandleUseCase())
                ) {
                    Timber.d("Session is in progress")
                    callAndSession.call?.let {
                        chatManagement.setRequestSentCall(it.callId, false)
                    }
                    rtcAudioManagerGateway.updateRTCAudioMangerTypeStatus(Constants.AUDIO_MANAGER_CALL_IN_PROGRESS)
                }
            }
        }
    }

    /**
     * Init wake lock
     *
     */
    private fun initWakeLock() {
        val pm = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MegaIncomingCallPowerLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 1000L)
        }
    }

    /**
     * Get open call chat id
     *
     */
    fun getOpenCallChatId(): Long = openCallChatId

    /**
     * Set open call chat id
     *
     */
    fun setOpenCallChatId(value: Long) {
        Timber.d("New open call chat ID: $value")
        openCallChatId = value
    }

    /**
     * Check several call
     *
     * @param listAllCalls
     * @param callStatus
     * @param isRinging
     * @param incomingCallChatId
     */
    private suspend fun checkSeveralCall(
        listAllCalls: MegaHandleList,
        callStatus: ChatCallStatus?,
        isRinging: Boolean,
        incomingCallChatId: Long,
    ) {
        Timber.d("Several calls = ${listAllCalls.size()}- Current call Status: $callStatus")
        callStatus ?: return
        if (isRinging) {
            if (CallUtil.participatingInACall()) {
                Timber.d("Several calls: show notification")
                checkQueuedCalls(incomingCallChatId)
                return
            }
            if (callStatus == ChatCallStatus.UserNoPresent) {
                megaChatApi.getChatRoom(incomingCallChatId)?.let { chatRoom ->
                    if (!CallUtil.isOneToOneCall(chatRoom)
                        && !chatManagement.isOpeningMeetingLink(incomingCallChatId)
                    ) {
                        Timber.d("Show incoming group call notification")
                        megaChatApi.getChatCall(incomingCallChatId)?.let {
                            showOneCallNotification(it)
                        }
                        return
                    }
                    if (CallUtil.isOneToOneCall(chatRoom) && openCallChatId != chatRoom.chatId) {
                        Timber.d("Show incoming one to one call screen")
                        val callToLaunch = megaChatApi.getChatCall(chatRoom.chatId)
                        checkOneToOneIncomingCall(callToLaunch)
                        return
                    }
                }
            }
        }
        val handleList = getCallHandleListUseCase(callStatus)
        if (handleList.isEmpty()) return
        var callToLaunch: MegaChatCall? = null
        for (i in handleList.indices) {
            if (openCallChatId != handleList[i]) {
                megaChatApi.getChatCall(handleList[i])?.takeIf { it.isOnHold }?.let {
                    callToLaunch = it
                }
            } else {
                Timber.d("The call is already opened")
            }
        }
        callToLaunch?.let { checkOneToOneIncomingCall(it) }
    }

    /**
     * Check one call
     *
     * @param incomingCallChatId
     */
    fun checkOneCall(incomingCallChatId: Long) {
        Timber.d("One call : Chat ID is $incomingCallChatId, openCall Chat ID is $openCallChatId")
        if (openCallChatId == incomingCallChatId) {
            Timber.d("The call is already opened")
            return
        }
        val callToLaunch = megaChatApi.getChatCall(incomingCallChatId) ?: run {
            Timber.w("Call is null")
            return
        }

        val callStatus = callToLaunch.status
        if (callStatus > MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            Timber.w("Launch not in correct status: $callStatus")
            return
        }
        val chatRoom = megaChatApi.getChatRoom(incomingCallChatId) ?: run {
            Timber.w("Chat room is null")
            return
        }
        if (!CallUtil.isOneToOneCall(chatRoom) && callToLaunch.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && callToLaunch.isRinging
            && !chatManagement.isOpeningMeetingLink(incomingCallChatId)
        ) {
            Timber.d("Group call or meeting, the notification should be displayed")
            showOneCallNotification(callToLaunch)
            return
        }
        checkOneToOneIncomingCall(callToLaunch)
    }

    private fun checkQueuedCalls(incomingCallChatId: Long) {
        try {
            ChatAdvancedNotificationBuilder.newInstance(application)
                .checkQueuedCalls(incomingCallChatId)
        } catch (e: java.lang.Exception) {
            Timber.e(e)
        }
    }

    /**
     * Check whether an incoming 1-to-1 call should show notification or incoming call screen
     *
     * @param callToLaunch The incoming call
     */
    private fun checkOneToOneIncomingCall(callToLaunch: MegaChatCall) {
        if (shouldNotify(application) && !activityLifecycleHandler.isActivityVisible) {
            initWakeLock()
            Timber.d("The notification should be displayed. Chat ID of incoming call ${callToLaunch.chatid}")
            showOneCallNotification(callToLaunch)
        } else {
            Timber.d("The call screen should be displayed. Chat ID of incoming call ${callToLaunch.chatid}")
            MegaApplication.getInstance()
                .createOrUpdateAudioManager(false, Constants.AUDIO_MANAGER_CALL_RINGING)
            launchCallActivity(callToLaunch)
        }
    }

    private fun launchCallActivity(call: MegaChatCall) {
        Timber.d("Show the call screen: ${CallUtil.callStatusToString(call.status)}, callId = ${call.callId}")
        CallUtil.openMeetingRinging(application, call.chatid, passcodeManagement)
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    fun showOneCallNotification(incomingCall: MegaChatCall) {
        Timber.d("Show incoming call notification and start to sound. Chat ID is ${incomingCall.chatid}")
        MegaApplication.getInstance()
            .createOrUpdateAudioManager(false, Constants.AUDIO_MANAGER_CALL_RINGING)
        chatManagement.addNotificationShown(incomingCall.chatid)
        ChatAdvancedNotificationBuilder.newInstance(application)
            .showOneCallNotification(incomingCall)
    }

    private suspend fun checkCallDestroyed(
        chatId: Long,
        callId: Long,
        endCallReason: EndCallReason?,
        isIgnored: Boolean,
    ) {
        chatManagement.setOpeningMeetingLink(chatId, false)
        wakeLock?.takeIf { it.isHeld }?.release()
        chatManagement.removeNotificationShown(chatId)

        val isRequestSent = chatManagement.isRequestSent(callId)
        chatManagement.setRequestSentCall(callId, false)

        try {
            if (endCallReason == EndCallReason.NoAnswer && !isIgnored && !isRequestSent) {
                val chatRoom = megaChatApi.getChatRoom(chatId)
                if (chatRoom != null
                    && !chatRoom.isGroup
                    && !chatRoom.isMeeting && isChatNotifiableUseCase(chatId)
                ) {
                    try {
                        Timber.d("Show missed call notification")
                        ChatAdvancedNotificationBuilder.newInstance(application)
                            .showMissedCallNotification(chatId, callId)
                    } catch (e: Exception) {
                        Timber.e(e, "EXCEPTION when showing missed call notification")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "EXCEPTION when showing missed call notification")
        }
    }


    /**
     * Method that performs the necessary actions when there is an incoming call.
     *
     * @param listAllCalls       List of all current calls
     * @param incomingCallChatId Chat ID of incoming call
     * @param callStatus         Call Status
     */
    private suspend fun incomingCall(
        listAllCalls: MegaHandleList,
        incomingCallChatId: Long,
        callStatus: ChatCallStatus?,
    ) {
        Timber.d("Chat ID of incoming call is %s", incomingCallChatId)
        if (!isChatNotifiableUseCase(incomingCallChatId) ||
            chatManagement.isNotificationShown(incomingCallChatId) ||
            !CallUtil.areNotificationsSettingsEnabled()
        ) {
            Timber.d("The chat is not notifiable or the notification is already being displayed")
            return
        }

        val chatRoom = megaChatApi.getChatRoom(incomingCallChatId)
        if (chatRoom == null) {
            Timber.e("The chat does not exist")
            return
        }

        if (!chatRoom.isMeeting || !chatManagement.isOpeningMeetingLink(incomingCallChatId)) {
            Timber.d("It is necessary to check the number of current calls")
            withContext(mainImmediateDispatcher) {
                controlNumberOfCalls(listAllCalls, callStatus, incomingCallChatId)
            }
        }
    }

    private suspend fun controlNumberOfCalls(
        listAllCalls: MegaHandleList,
        callStatus: ChatCallStatus?,
        incomingCallChatId: Long,
    ) {
        if (listAllCalls.size() == 1L) {
            checkOneCall(incomingCallChatId)
        } else {
            checkSeveralCall(listAllCalls, callStatus, true, incomingCallChatId)
        }
    }

    private fun shouldNotify(context: Context?) =
        Util.isAndroid10OrUpper() && !Settings.canDrawOverlays(context)
}
