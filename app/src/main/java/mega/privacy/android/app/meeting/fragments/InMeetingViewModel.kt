package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession
import kotlin.random.Random

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback {

    //private var chatRoom: MegaChatRoom? = null
    var chatRoom: MutableLiveData<MegaChatRoom> = MutableLiveData<MegaChatRoom>()

    var waitingForMeetingLink: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    // Meeting
    private val _callLiveData: MutableLiveData<MegaChatCall?> = MutableLiveData<MegaChatCall?>()
    val callLiveData: LiveData<MegaChatCall?> = _callLiveData

    // Chat title
    private val _chatTitle: MutableLiveData<String> =
        MutableLiveData<String>(inMeetingRepository.getInitialMeetingName())
    val chatTitle: LiveData<String> = _chatTitle

    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    //TODO test code start
    val frames: MutableLiveData<MutableList<Bitmap>> = MutableLiveData(mutableListOf())

    private val updateCallObserver =
        Observer<MegaChatCall> {
            if (isSameChatRoom(it.chatid)) {
                _callLiveData.value = it
            }
        }

    private val callStatusObserver =
        androidx.lifecycle.Observer<MegaChatCall> {
        }

    init {
        LiveEventBus.get(
            Constants.EVENT_CALL_STATUS_CHANGE,
            MegaChatCall::class.java
        ).observeForever(callStatusObserver)

        LiveEventBus.get(Constants.EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)
    }

    /**
     * Method to know if this chat is public
     *
     * @return True, if it's public. False, otherwise
     */
    fun isChatRoomPublic(): Boolean {
        chatRoom.value?.let {
            if (it.isPublic)
                return true
        }
        return false
    }

    /**
     * Method to know if it is the same chat
     *
     * @param chatId chat ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameChatRoom(chatId: Long): Boolean {
        chatRoom.value?.let {
            if (it.chatId == chatId)
                return true
        }
        return false
    }

    /**
     * Method to know if it is the same call
     *
     * @param callId call ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameCall(callId: Long): Boolean {
        _callLiveData.value?.let {
            if (it.callId == callId)
                return true
        }

        return false
    }

    /**
     * Method to set a call
     *
     * @param chatId chat ID
     */
    fun setCall(chatId: Long) {
        if (isSameChatRoom(chatId)) {
            _callLiveData.value = inMeetingRepository.getMeeting(chatId)
        }
    }

    /**
     * Method to get a call
     *
     * @return MegaChatCall
     */
    fun getCall(): MegaChatCall? {
        chatRoom.value?.let { return inMeetingRepository.getMeeting(it.chatId) }
        return null
    }

    /**
     * Method to get a chat
     *
     * @return MegaChatRoom
     */
    fun getChat(): MegaChatRoom? {
        return chatRoom.value
    }

    /**
     * Method to set a chat
     *
     * @param chatId chat ID
     */
    fun setChat(chatId: Long) {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return

        inMeetingRepository.getChatRoom(chatId).also { chatRoom.value = it }
        chatRoom.value?.let {
            setCall(chatId)
            _chatTitle.value = getTitleChat(it)
        }
    }

    /**
     * Get the chat ID of the current meeting
     *
     * @return chat ID
     */
    fun getChatId(): Long {
        chatRoom.value?.let {
            return it.chatId
        }
        return MEGACHAT_INVALID_HANDLE
    }

    /**
     * Method to know if it's me
     *
     * @param peerId The handle
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean {
        return inMeetingRepository.isMe(peerId)
    }

    /**
     * Method to know if I have asked for a chat link and I am waiting
     *
     */
    fun isWaitingForLink(): Boolean {
        waitingForMeetingLink.value?.let {
            return it
        }
        return false
    }

    fun getSession(clientId: Long): MegaChatSession? {
        if (clientId == MEGACHAT_INVALID_HANDLE)
            return null

        _callLiveData.value?.let {
            return it.getMegaChatSession(clientId)
        }

        return null
    }

    /**
     * Method to set up if I have requested a chat link and I am waiting
     *
     */
    fun setWaitingForLink(isWaiting: Boolean) {
        waitingForMeetingLink.value = isWaiting
    }

    /**
     * Method to know if a one-to-one call is audio only
     *
     * @return True, if it's audio call. False, otherwise
     */
    fun isAudioCall(): Boolean {
        _callLiveData.value?.let {
            val session = CallUtil.getSessionIndividualCall(it)
            if (session != null && session.isOnHold && MegaApplication.wasLocalVideoEnable()) {
                return false
            }

            return session == null || (!it.hasLocalVideo() && !session.hasVideo())
        }
        return true
    }

    /**
     *  Method to know if it is a one-to-one chat call
     *
     *  @return True, if it is a one-to-one chat call. False, otherwise
     */
    fun isOneToOneCall(): Boolean {
        chatRoom.value?.let {
            if (!it.isGroup)
                return true
        }
        return false
    }

    /**
     * Method to know if a call is on hold
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOnHold(): Boolean {
        _callLiveData.value?.let { call ->
            if (call.isOnHold) {
                return true
            }
        }

        return false
    }

    /**
     * Method to know if a call or session is on hold
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHold(): Boolean {
        _callLiveData.value?.let { call ->
            if (call.isOnHold) {
                return true
            }
            if (isOneToOneCall()) {
                val session = inMeetingRepository.getSessionOneToOneCall(call)
                session?.let { it ->
                    if (it.isOnHold) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Method to switch a call on hold
     *
     * @param isOn True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(isOn: Boolean) {
        chatRoom.value?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isOn)
        }
    }

    /**
     * Method for displaying the correct banner: If the call is muted or on hold
     *
     * @return Banner text
     */
    fun showAppropriateBanner(bannerIcon: ImageView?, bannerText: TextView?): Boolean {
        when {
            isCallOrSessionOnHold() -> {
                bannerIcon?.let {
                    it.isVisible = false
                }
                bannerText?.let {
                    it.text = StringResourcesUtils.getString(R.string.call_on_hold)
                }
                return true
            }
            else -> {
                _callLiveData.value?.let { call ->
                    if (isOneToOneCall()) {
                        val session = inMeetingRepository.getSessionOneToOneCall(call)
                        when {
                            session != null -> {
                                if (!session.hasAudio() && session.peerid != MEGACHAT_INVALID_HANDLE) {
                                    bannerIcon?.let {
                                        it.isVisible = true
                                    }
                                    bannerText?.let {
                                        it.text = StringResourcesUtils.getString(
                                            R.string.muted_contact_micro,
                                            inMeetingRepository.getContactOneToOneCallName(
                                                session.peerid
                                            ))
                                        return true
                                    }

                                }
                            }
                        }

                        if (!call.hasLocalAudio()) {
                            bannerIcon?.let {
                                it.isVisible = false
                            }
                            bannerText?.let {
                                it.text = StringResourcesUtils.getString(R.string.muted_own_micro)
                            }
                            return true
                        }
                    }
                }

            }
        }
        return false
    }

    /**
     *  Method to know if it is a outgoing call
     *
     *  @return True, if it is a outgoing call. False, otherwise
     */
    fun isRequestSent(): Boolean {
        val callId = _callLiveData.value?.callId ?: return false

        return callId != MEGACHAT_INVALID_HANDLE && MegaApplication.isRequestSent(callId)
    }

    /**
     * Set view change to manual mode
     */
    fun setSpeakerViewManual() {
        _callLiveData.value?.let { MegaApplication.setSpeakerViewAutomatic(it.callId, false) }
    }

    /**
     * Method to know if the automatic view change mode is on
     *
     *  @return True, if it's automatic. False, otherwise
     */
    fun isSpeakerViewAutomatic(): Boolean {
        return _callLiveData.value?.let { MegaApplication.isSpeakerViewAutomatic(it.callId) } == true
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean {
        _callLiveData.value?.let {
            if (it.hasLocalVideo() && !it.isOnHold) {
                return true
            }
        }
        return false
    }

    /**
     * Method to start a meeting
     *
     * @param audioEnable if the audio is enable
     * @param videoEnable if the video is enable
     * @param listener MegaChatRequestListenerInterface
     */
    fun startMeeting(
        audioEnable: Boolean,
        videoEnable: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        if (chatRoom.value != null && chatRoom.value?.chatId != MEGACHAT_INVALID_HANDLE) {
            //The chat exists
            MegaApplication.getInstance().openCallService(chatRoom.value!!.chatId)

            chatRoom.value?.let {
                inMeetingRepository.startMeeting(
                    it.chatId,
                    audioEnable,
                    videoEnable,
                    listener
                )
            }
        } else {
            //The chat doesn't exist
            inMeetingRepository.createMeeting(
                _chatTitle.value!!,
                CreateGroupChatWithPublicLink()
            )
        }
    }

    fun addParticipant(add: Boolean) {
        if (add) {
            participants.value!!.add(TestTool.testData()[Random.nextInt(TestTool.testData().size)])
        } else {
            if (participants.value!!.size > 2) {
                participants.value!!.removeAt(participants.value!!.size - 1)
            }
        }
        participants.value = participants.value
    }
    //TODO test code end

    /**
     * Method for leave the meeting
     */
    fun leaveMeeting() {
        _callLiveData.value?.let {
            inMeetingRepository.leaveMeeting(it.callId)
        }
    }

    /**
     * Set a title for the chat
     *
     * @param newTitle the chat title
     */
    fun setTitleChat(newTitle: String) {
        if (chatRoom.value == null) {
            _chatTitle.value = newTitle
        } else {
            chatRoom.value?.chatId?.let {
                inMeetingRepository.setTitleChatRoom(
                    it,
                    newTitle,
                    EditChatRoomNameListener(MegaApplication.getInstance(), this)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(
            Constants.EVENT_CALL_STATUS_CHANGE,
            MegaChatCall::class.java
        ).removeObserver(callStatusObserver)

        LiveEventBus.get(Constants.EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        chatRoom.value?.let {
            if (it.chatId == chatId) {
                _chatTitle.value = name
            }
        }
    }
}