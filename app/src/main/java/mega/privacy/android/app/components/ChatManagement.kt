package mega.privacy.android.app.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.util.Pair
import androidx.preference.PreferenceManager
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.constants.EventConstants.EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_NOT_OUTGOING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_OUTGOING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS
import mega.privacy.android.app.listeners.ChatRoomListener
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.domain.entity.statistics.EndedEmptyCallTimeout
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.BroadcastJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for chat management
 *
 * @property applicationScope                       [CoroutineScope]
 * @property hangChatCallUseCase                    [HangChatCallUseCase]
 * @property sendStatisticsMeetingsUseCase          [SendStatisticsMeetingsUseCase]
 * @property rtcAudioManagerGateway                 [RTCAudioManagerGateway]
 * @property megaChatApi                            [MegaChatApiAndroid]
 * @property broadcastJoinedSuccessfullyUseCase     [BroadcastJoinedSuccessfullyUseCase]
 */
@Singleton
class ChatManagement @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val megaChatApi: MegaChatApiAndroid,
    private val broadcastJoinedSuccessfullyUseCase: BroadcastJoinedSuccessfullyUseCase,
) {
    private val app: MegaApplication = getInstance()
    private var countDownTimerToEndCall: CountDownTimer? = null

    /**
     * List of chat room listeners
     */
    private val chatRoomListeners = HashMap<Long, ChatRoomListener>()

    /**
     * Counter down value used to hang a call when user is alone (empty call scenario)
     */
    var millisecondsOnlyMeInCallDialog = 0L

    /**
     * Boolean indicating whether the end call dialog was ignored.
     */
    var hasEndCallDialogBeenIgnored = false

    // List of chat ids to control if a chat is already joining.
    private val joiningChatIds: MutableList<Long> = mutableListOf()

    // List of chat ids to control if a chat is already leaving.
    private val leavingChatIds: MutableList<Long> = mutableListOf()

    // List of chats ids to control if I'm trying to join the call
    private val joiningCallChatIds: MutableList<Long> = mutableListOf()

    // List of chats with video activated in the call
    private val hashMapVideo = HashMap<Long, Boolean>()

    // List of chats with speaker activated in the call
    private val hashMapSpeaker = HashMap<Long, Boolean>()

    // List of outgoing calls
    private val hashMapOutgoingCall = HashMap<Long, Boolean>()

    // List of chats in which a meeting is being opened via a link
    private val hashOpeningMeetingLink = HashMap<Long, Boolean>()

    // List of group chats being participated in
    private val currentActiveGroupChat = mutableListOf<Long>()

    // List of calls for which an incoming call notification has already been shown
    private val notificationShown = mutableListOf<Long>()

    // List of pending messages ids in which its transfer is to be cancelled
    private val hashMapPendingMessagesToBeCancelled = HashMap<Int, Long>()

    // List of messages id to delete
    private val messagesToBeDeleted = mutableListOf<Long>()

    /**
     * If this has a valid value, means there is a pending chat link to join.
     */
    var pendingJoinLink: String? = null

    /**
     * Boolean indicating whether the call is in a temporary state.
     */
    var isInTemporaryState = false

    /**
     * Boolean indicating whether the local video is being disabled.
     */
    var isDisablingLocalVideo = false
    private var isScreenOn = true
    private var isScreenBroadcastRegister = false

    /**
     * Method for open a particular chatRoom.
     *
     * @param chatId The chat ID.
     * @return True, if it has been done correctly. False if not.
     */
    fun openChatRoom(chatId: Long): Boolean {
        closeChatRoom(chatId)
        chatRoomListeners[chatId] = ChatRoomListener()
        return megaChatApi.openChatRoom(chatId, chatRoomListeners[chatId])
    }

    /**
     * Method for close a particular chatRoom.
     *
     * @param chatId The chat ID.
     */
    private fun closeChatRoom(chatId: Long) {
        if (chatRoomListeners.containsKey(chatId)) {
            megaChatApi.closeChatRoom(chatId, chatRoomListeners[chatId])
            chatRoomListeners.remove(chatId)
        }
    }

    /**
     * Check if there is a pending join link
     *
     * @return True if there is a pending join link or false otherwise
     */
    fun isPendingJoinLink() = !pendingJoinLink.isNullOrEmpty()

    /**
     * Add joining chat ID
     *
     * @param joiningChatId The joining chat ID
     */
    fun addJoiningChatId(joiningChatId: Long) = joiningChatIds.add(joiningChatId)

    /**
     * Remove joining chat ID
     *
     * @param joiningChatId The joining chat ID
     */
    fun removeJoiningChatId(joiningChatId: Long) = joiningChatIds.remove(joiningChatId)

    /**
     * Broadcasting that successfully joined to a chat
     */
    fun broadcastJoinedSuccessfully() {
        applicationScope.launch {
            broadcastJoinedSuccessfullyUseCase()
        }
    }

    /**
     * Check if there is already joining to a chat
     *
     * @param joinChatId The chat ID to check
     * @return True if there is already joining to the chat or false otherwise
     */
    fun isAlreadyJoining(joinChatId: Long) = joiningChatIds.contains(joinChatId)

    /**
     * Add leaving chat ID
     *
     * @param leavingChatId The leaving chat ID
     */
    fun addLeavingChatId(leavingChatId: Long) = leavingChatIds.add(leavingChatId)

    /**
     * Remove leaving chat ID
     *
     * @param leavingChatId The leaving chat ID
     */
    fun removeLeavingChatId(leavingChatId: Long) = leavingChatIds.remove(leavingChatId)

    /**
     * Check if there is already leaving a chat
     *
     * @param leaveChatId The chat ID to check
     * @return True if there is already leaving the chat or false otherwise
     */
    fun isAlreadyLeaving(leaveChatId: Long) = leavingChatIds.contains(leaveChatId)

    /**
     * Add joining call chat ID
     *
     * @param joiningCallChatId The joining call chat ID
     */
    fun addJoiningCallChatId(joiningCallChatId: Long) = joiningCallChatIds.add(joiningCallChatId)

    /**
     * Remove joining call chat ID
     *
     * @param joiningCallChatId The joining call chat ID
     */
    fun removeJoiningCallChatId(joiningCallChatId: Long) =
        joiningCallChatIds.remove(joiningCallChatId)

    /**
     * Check if there is already joining to a call
     *
     * @param joiningCallChatId The call chat ID to check
     * @return True if there is already joining to the call or false otherwise
     */
    fun isAlreadyJoiningCall(joiningCallChatId: Long) =
        joiningCallChatIds.contains(joiningCallChatId)

    /**
     * Get speaker status
     *
     * @param chatId The chat ID
     * @return True if speaker is enabled or false otherwise
     */
    fun getSpeakerStatus(chatId: Long) =
        if (hashMapSpeaker.containsKey(chatId) && hashMapSpeaker[chatId] != null) {
            hashMapSpeaker[chatId]!!
        } else {
            setSpeakerStatus(chatId, false)
            false
        }

    /**
     * Set speaker status
     *
     * @param chatId The chat ID
     * @param speakerStatus True if speaker is enabled or false otherwise
     */
    fun setSpeakerStatus(chatId: Long, speakerStatus: Boolean) {
        hashMapSpeaker[chatId] = speakerStatus
    }

    /**
     * Get video status
     *
     * @param chatId The chat ID
     * @return True if video is enabled or false otherwise
     */
    fun getVideoStatus(chatId: Long) =
        if (hashMapVideo.containsKey(chatId) && hashMapVideo[chatId] != null) {
            hashMapVideo[chatId]!!
        } else {
            setVideoStatus(chatId, false)
            false
        }

    /**
     * Set video status
     *
     * @param chatId The chat ID
     * @param videoStatus True if video is enabled or false otherwise
     */
    fun setVideoStatus(chatId: Long, videoStatus: Boolean) {
        hashMapVideo[chatId] = videoStatus
    }

    /**
     * Set a pending message to be cancelled from a chat
     *
     * @param tag The message TAG
     * @param chatId The chat ID
     */
    fun setPendingMessageToBeCancelled(tag: Int, chatId: Long) {
        if (getPendingMsgIdToBeCancelled(tag) == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            hashMapPendingMessagesToBeCancelled[tag] = chatId
        }
    }

    /**
     * Get the chat from where a message is pending to be cancelled
     *
     * @param tag The message TAG
     * @return The chat ID
     */
    fun getPendingMsgIdToBeCancelled(tag: Int) =
        if (hashMapPendingMessagesToBeCancelled.containsKey(tag)) {
            hashMapPendingMessagesToBeCancelled[tag]
        } else {
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        }

    /**
     * Remove a pending message from the list of messages to be cancelled
     *
     * @param tag The message TAG
     */
    fun removePendingMsgToBeCancelled(tag: Int) {
        if (getPendingMsgIdToBeCancelled(tag) != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            hashMapPendingMessagesToBeCancelled.remove(tag)
        }
    }

    /**
     * Add a message to the list of messages to be deleted
     *
     * @param pMsgId The message ID
     */
    fun addMsgToBeDelete(pMsgId: Long) {
        if (!isMsgToBeDelete(pMsgId)) {
            messagesToBeDeleted.add(pMsgId)
        }
    }

    /**
     * Check if a message is pending to be deleted
     *
     * @param pMsgId The message ID
     * @return True if the message is in the list of messages to be deleted or false otherwise
     */
    fun isMsgToBeDelete(pMsgId: Long) =
        if (messagesToBeDeleted.isEmpty()) false else messagesToBeDeleted.contains(pMsgId)

    /**
     * Remove a message from the list of messages to be deleted
     *
     * @param pMsgId The message ID
     */
    fun removeMsgToDelete(pMsgId: Long) {
        if (isMsgToBeDelete(pMsgId)) {
            messagesToBeDeleted.remove(pMsgId)
        }
    }

    /**
     * Check if is opening a meeting link of a chat
     *
     * @param chatId The chat ID to check
     * @return True if is opening a meeting link of the chat or false otherwise
     */
    fun isOpeningMeetingLink(chatId: Long) =
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE && hashOpeningMeetingLink.containsKey(
                chatId
            ) && hashOpeningMeetingLink[chatId] != null
        ) {
            hashOpeningMeetingLink[chatId]!!
        } else {
            false
        }

    /**
     * Set if is opening a meeting link of a chat
     *
     * @param chatId The chat ID of the meeting link
     * @param isOpeningMeetingLink True if is opening the meeting ling or false otherwise
     */
    fun setOpeningMeetingLink(chatId: Long, isOpeningMeetingLink: Boolean) {
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            hashOpeningMeetingLink[chatId] = isOpeningMeetingLink
        }
    }

    /**
     * Check if a call request has been sent
     *
     * @param callId ID of the call to check
     * @return True if the call request has been sent or false otherwise
     */
    fun isRequestSent(callId: Long) =
        if (hashMapOutgoingCall.containsKey(callId) && hashMapOutgoingCall[callId] != null) {
            hashMapOutgoingCall[callId]!!
        } else {
            false
        }

    /**
     * Set if a call request has been sent
     *
     * @param callId The call ID
     * @param isRequestSent True if the call request has been sent or false otherwise
     */
    fun setRequestSentCall(callId: Long, isRequestSent: Boolean) {
        if (isRequestSent(callId) == isRequestSent) return
        hashMapOutgoingCall[callId] = isRequestSent
        if (isRequestSent) {
            LiveEventBus.get(EVENT_OUTGOING_CALL, Long::class.java).post(callId)
        } else {
            LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long::class.java).post(callId)
        }
    }

    /**
     * Add current active group chat
     *
     * @param chatId The chat ID
     */
    fun addCurrentGroupChat(chatId: Long) {
        if (currentActiveGroupChat.isEmpty() || !currentActiveGroupChat.contains(chatId)) {
            currentActiveGroupChat.add(chatId)
        }
    }

    /**
     * Add notification shown
     *
     * @param chatId The chat ID of the notification
     */
    fun addNotificationShown(chatId: Long) {
        if (!isNotificationShown(chatId)) {
            notificationShown.add(chatId)
        }
    }

    /**
     * Remove notification shown
     *
     * @param chatId The chat ID of the notification
     */
    fun removeNotificationShown(chatId: Long) {
        if (isNotificationShown(chatId)) {
            notificationShown.remove(chatId)
        }
    }

    /**
     * Check if notification has been shown
     *
     * @param chatId The chat ID of the notification
     */
    fun isNotificationShown(chatId: Long) =
        if (notificationShown.isEmpty()) false else notificationShown.contains(chatId)

    /**
     * Remove current active group chat and notification shown
     *
     * @param chatId The chat ID
     */
    fun removeActiveChatAndNotificationShown(chatId: Long) {
        currentActiveGroupChat.remove(chatId)
        removeNotificationShown(chatId)
    }

    private fun removeStatusVideoAndSpeaker(chatId: Long) {
        hashMapSpeaker.remove(chatId)
        hashMapVideo.remove(chatId)
    }

    /**
     * Method to control when a call has ended
     *
     * @param callId Call ID
     * @param chatId Chat ID
     */
    fun controlCallFinished(callId: Long, chatId: Long) {
        val listCalls = CallUtil.getCallsParticipating()
        if (listCalls == null || listCalls.isEmpty()) {
            rtcAudioManagerGateway.unregisterProximitySensor()
        }
        CallUtil.clearIncomingCallNotification(callId)
        removeValues(chatId)
        removeStatusVideoAndSpeaker(chatId)
        unregisterScreenReceiver()
    }

    /**
     * Method to start a timer to end the call
     *
     * @param chatId Chat ID of the call
     */
    fun startCounterToFinishCall(chatId: Long) {
        stopCounterToFinishCall()
        hasEndCallDialogBeenIgnored = false
        millisecondsOnlyMeInCallDialog =
            TimeUnit.SECONDS.toMillis(Constants.SECONDS_TO_WAIT_ALONE_ON_THE_CALL)
        if (countDownTimerToEndCall == null) {
            countDownTimerToEndCall = object :
                CountDownTimer(millisecondsOnlyMeInCallDialog, TimeUnit.SECONDS.toMillis(1)) {
                override fun onTick(millisUntilFinished: Long) {
                    millisecondsOnlyMeInCallDialog = millisUntilFinished
                }

                override fun onFinish() {
                    val call = megaChatApi.getChatCall(chatId)
                    if (call != null) {
                        LiveEventBus.get(
                            EVENT_UPDATE_WAITING_FOR_OTHERS, Pair::class.java
                        ).post(
                            Pair.create(chatId, false)
                        )
                        applicationScope.launch {
                            runCatching {
                                hangChatCallUseCase(call.callId)
                                sendStatisticsMeetingsUseCase(EndedEmptyCallTimeout())
                            }.onFailure { exception ->
                                Timber.e(exception.message)
                            }.onSuccess { Timber.d("Call hung up due to timeout") }
                        }
                    }
                }
            }.start()
        }
    }

    /**
     * Method to stop the counter to end the call
     */
    fun stopCounterToFinishCall() {
        millisecondsOnlyMeInCallDialog = 0
        countDownTimerToEndCall?.cancel()
        countDownTimerToEndCall = null
    }

    /**
     * Method to remove the audio manager and reset video and speaker settings
     *
     * @param chatId Chat ID
     */
    fun removeValues(chatId: Long) {
        PreferenceManager.getDefaultSharedPreferences(getInstance().applicationContext).edit()
            .remove(
                Constants.KEY_IS_SHOWED_WARNING_MESSAGE + chatId
            ).apply()
        if (!CallUtil.existsAnOngoingOrIncomingCall()) {
            rtcAudioManagerGateway.removeRTCAudioManager()
            rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
        } else if (CallUtil.participatingInACall()) {
            rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
        }
    }

    /**
     * Method to control whether or not to display the incoming call notification when I am added to a group and a call is in progress.
     *
     * @param chatId Chat ID of the new group chat
     */
    fun checkActiveGroupChat(chatId: Long) {
        if (currentActiveGroupChat.isEmpty() || !currentActiveGroupChat.contains(chatId)) {
            megaChatApi.getChatCall(chatId)?.let {
                if (it.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                    addCurrentGroupChat(chatId)
                    checkToShowIncomingGroupCallNotification(it, chatId)
                }
            } ?: Timber.e("Call is null")
        }
    }

    /**
     * Method that displays the incoming call notification when I am added to a group that has a call in progress
     *
     * @param call   The call in this chat room
     * @param chatId The chat ID
     */
    private fun checkToShowIncomingGroupCallNotification(call: MegaChatCall, chatId: Long) {
        if (call.isRinging || isNotificationShown(chatId)) {
            Timber.d("Call is ringing or notification is shown")
            return
        }
        if (CallUtil.CheckIfIAmParticipatingWithAnotherClient(call)) {
            Timber.d("I am participating with another client")
            return
        }
        val chatRoom = megaChatApi.getChatRoom(chatId)
        if (chatRoom == null) {
            Timber.e("Chat is null")
            return
        }
        if (isOpeningMeetingLink(chatId)) {
            Timber.d("Opening meeting link, don't show notification")
            return
        }
        Timber.d("Show incoming call notification")
        app.showOneCallNotification(call)
    }

    /**
     * Register screen broadcast receiver
     */
    fun registerScreenReceiver() {
        if (isScreenBroadcastRegister) return
        val filterScreen = IntentFilter()
        filterScreen.addAction(Intent.ACTION_SCREEN_OFF)
        filterScreen.addAction(Intent.ACTION_USER_PRESENT)
        getInstance().registerReceiver(screenOnOffReceiver, filterScreen)
        isScreenBroadcastRegister = true
    }

    /**
     * Un-register screen broadcast receiver
     */
    private fun unregisterScreenReceiver() {
        if (isScreenBroadcastRegister) {
            getInstance().unregisterReceiver(screenOnOffReceiver)
            isScreenBroadcastRegister = false
        }
    }

    /**
     * Broadcast for controlling changes in screen.
     */
    private var screenOnOffReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) return
            val callInProgress = CallUtil.getCallInProgress()
            if (callInProgress == null || callInProgress.status != MegaChatCall.CALL_STATUS_JOINING && callInProgress.status != MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                return
            }
            if (!getVideoStatus(callInProgress.chatid)) {
                isInTemporaryState = false
                return
            }
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    rtcAudioManagerGateway.muteOrUnMute(true)
                    isDisablingLocalVideo = false
                    isInTemporaryState = true
                    if (callInProgress.hasLocalVideo() && !isDisablingLocalVideo) {
                        Timber.d("Screen locked, local video is going to be disabled")
                        isScreenOn = false
                        CallUtil.enableOrDisableLocalVideo(
                            false, callInProgress.chatid, DisableAudioVideoCallListener(
                                getInstance()
                            )
                        )
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    isDisablingLocalVideo = false
                    isInTemporaryState = false
                    if (!callInProgress.hasLocalVideo() && !isDisablingLocalVideo) {
                        Timber.d("Screen unlocked, local video is going to be enabled")
                        isScreenOn = true
                        CallUtil.enableOrDisableLocalVideo(
                            true, callInProgress.chatid, DisableAudioVideoCallListener(
                                getInstance()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Method for checking when there are changes in the proximity sensor
     *
     * @param isNear True, if the device is close to the ear. False, if it is far away
     */
    fun controlProximitySensor(isNear: Boolean) = CallUtil.getCallInProgress()?.let { call ->
        if (call.status != MegaChatCall.CALL_STATUS_JOINING
            && call.status != MegaChatCall.CALL_STATUS_IN_PROGRESS
            || !isScreenOn || !VideoCaptureUtils.isFrontCameraInUse()
        ) return@let

        when {
            !getVideoStatus(call.chatid) -> {
                isInTemporaryState = false
            }
            isNear -> {
                Timber.d("Proximity sensor, video off")
                isDisablingLocalVideo = false
                isInTemporaryState = true
                LiveEventBus.get(
                    EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE, Boolean::class.java
                ).post(false)
                if (call.hasLocalVideo() && !isDisablingLocalVideo) {
                    CallUtil.enableOrDisableLocalVideo(
                        false, call.chatid, DisableAudioVideoCallListener(
                            getInstance()
                        )
                    )
                }
            }
            else -> {
                Timber.d("Proximity sensor, video on")
                isDisablingLocalVideo = false
                isInTemporaryState = false
                LiveEventBus.get(
                    EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE, Boolean::class.java
                ).post(true)
                if (!call.hasLocalVideo() && !isDisablingLocalVideo) {
                    CallUtil.enableOrDisableLocalVideo(
                        true, call.chatid, DisableAudioVideoCallListener(
                            getInstance()
                        )
                    )
                }
            }
        }
    }
}