package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.util.Pair
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.IncomingCallNotification
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaHandleList
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call status handler
 *
 * @property rtcAudioManagerGateway
 * @property megaChatApi
 * @property megaApi
 * @property application
 * @constructor Create empty Call status handler
 */
@Singleton
class CallChangesObserver @Inject constructor(
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val megaChatApi: MegaChatApiAndroid,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val application: Application,
    private val chatManagement: ChatManagement,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val passcodeManagement: PasscodeManagement,
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private var openCallChatId: Long = -1

    /**
     * listen all call status observer
     */
    fun init() {
        LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callStatusObserver)
        LiveEventBus.get(EventConstants.EVENT_RINGING_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callRingingStatusObserver)
        LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE, Pair::class.java)
            .observeForever(sessionStatusObserver)
        LiveEventBus.get(EventConstants.EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .observeForever(callCompositionObserver)
    }

    private val callCompositionObserver =
        Observer { call: MegaChatCall ->
            if (call.callCompositionChange == 1 && call.numParticipants > 1) {
                megaChatApi.getChatRoom(call.chatid)?.let {
                    Timber.d("Stop sound")
                    if (megaChatApi.myUserHandle == call.peeridCallCompositionChange) {
                        CallUtil.clearIncomingCallNotification(call.callId)
                        chatManagement.removeValues(call.chatid)
                        if (call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                            LiveEventBus.get(EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT,
                                Long::class.java).post(call.chatid)
                        }
                    }
                }
            }
        }

    private val callStatusObserver = Observer { call: MegaChatCall ->
        val callStatus = call.status
        val isOutgoing = call.isOutgoing
        val isRinging = call.isRinging
        val callId = call.callId
        val chatId = call.chatid
        if (chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE || callId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error in chatId or callId")
            return@Observer
        }
        Timber.d("Call status is ${CallUtil.callStatusToString(callStatus)}, chat id is $chatId, call id is $callId")
        when (call.status) {
            MegaChatCall.CALL_STATUS_CONNECTING -> {
                if (isOutgoing && chatManagement.isRequestSent(callId)) {
                    rtcAudioManagerGateway.removeRTCAudioManager()
                }
            }
            MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> {
                val listAllCalls =
                    megaChatApi.chatCalls?.takeIf { it.size() > 0 } ?: return@Observer
                if (isRinging) {
                    Timber.d("Is incoming call")
                    CallUtil.incomingCall(listAllCalls, chatId, callStatus)
                } else {
                    val chatRoom = megaChatApi.getChatRoom(chatId)
                    if (chatRoom != null && chatRoom.isGroup) {
                        Timber.d("Check if the incoming group call notification should be displayed")
                        chatManagement.checkActiveGroupChat(chatId)
                    }
                }
            }
            MegaChatCall.CALL_STATUS_JOINING, MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                megaChatApi.chatCalls?.takeIf { it.size() > 0 } ?: return@Observer
                chatManagement.addNotificationShown(chatId)
                Timber.d("Is ongoing call")
                CallUtil.ongoingCall(rtcAudioManagerGateway,
                    chatId,
                    callId,
                    if (isOutgoing && chatManagement
                            .isRequestSent(callId)
                    ) Constants.AUDIO_MANAGER_CALL_OUTGOING else Constants.AUDIO_MANAGER_CALL_IN_PROGRESS)
            }
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION -> {
                Timber.d("The user participation in the call has ended. The termination code is ${
                    CallUtil.terminationCodeForCallToString(call.termCode)
                }")
                chatManagement.controlCallFinished(callId, chatId)
            }
            MegaChatCall.CALL_STATUS_DESTROYED -> {
                Timber.d("Call has ended. End call reason is ${CallUtil.endCallReasonToString(call.endCallReason)}")
                chatManagement.controlCallFinished(callId, chatId)
                checkCallDestroyed(
                    chatId = chatId,
                    callId = callId,
                    endCallReason = call.endCallReason,
                    isIgnored = call.isIgnored
                )
            }
        }
    }

    private val callRingingStatusObserver = Observer { call: MegaChatCall ->
        val callStatus = call.status
        val isRinging = call.isRinging
        val listAllCalls = megaChatApi.chatCalls
        if (listAllCalls == null || listAllCalls.size() == 0L) {
            Timber.e("Calls not found")
            return@Observer
        }
        if (isRinging) {
            Timber.d("Is incoming call")
            CallUtil.incomingCall(listAllCalls, call.chatid, callStatus)
        } else {
            CallUtil.clearIncomingCallNotification(call.callId)
            chatManagement.removeValues(call.chatid)
        }
    }

    private val sessionStatusObserver = Observer { callAndSession: Pair<*, *> ->
        val session = callAndSession.second as? MegaChatSession ?: return@Observer
        val call = callAndSession.first as? MegaChatCall ?: return@Observer
        val sessionStatus = session.status
        megaChatApi.getChatRoom(call.chatid)?.let { room ->
            if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS &&
                (room.isGroup || room.isMeeting || session.peerid != megaApi.myUserHandleBinary)
            ) {
                Timber.d("Session is in progress")
                chatManagement.setRequestSentCall(call.callId, false)
                rtcAudioManagerGateway.updateRTCAudioMangerTypeStatus(Constants.AUDIO_MANAGER_CALL_IN_PROGRESS)
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
    fun checkSeveralCall(
        listAllCalls: MegaHandleList,
        callStatus: Int,
        isRinging: Boolean,
        incomingCallChatId: Long,
    ) {
        Timber.d("Several calls = ${listAllCalls.size()}- Current call Status: ${
            CallUtil.callStatusToString(callStatus)
        }")
        if (isRinging) {
            if (CallUtil.participatingInACall()) {
                Timber.d("Several calls: show notification")
                checkQueuedCalls(incomingCallChatId)
                return
            }
            if (callStatus == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
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
        val handleList = megaChatApi.getChatCalls(callStatus)
        if (handleList == null || handleList.size() == 0L) return
        var callToLaunch: MegaChatCall? = null
        for (i in 0 until handleList.size()) {
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
        if (IncomingCallNotification.shouldNotify(application) && !activityLifecycleHandler.isActivityVisible) {
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

    private fun checkCallDestroyed(
        chatId: Long,
        callId: Long,
        endCallReason: Int,
        isIgnored: Boolean,
    ) {
        chatManagement.setOpeningMeetingLink(chatId, false)
        if (IncomingCallNotification.shouldNotify(application)) {
            IncomingCallNotification.toSystemSettingNotification(application)
        }
        wakeLock?.takeIf { it.isHeld }?.release()
        chatManagement.removeNotificationShown(chatId)
        try {
            if (endCallReason == MegaChatCall.END_CALL_REASON_NO_ANSWER && !isIgnored) {
                val chatRoom = megaChatApi.getChatRoom(chatId)
                if (chatRoom != null
                    && !chatRoom.isGroup
                    && !chatRoom.isMeeting && megaApi.isChatNotifiable(chatId)
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
}