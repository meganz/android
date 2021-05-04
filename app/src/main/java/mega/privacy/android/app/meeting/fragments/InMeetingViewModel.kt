package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.lang.reflect.Method
import java.util.*

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback {

    //private var chatRoom: MegaChatRoom? = null
    var chatRoom: MutableLiveData<MegaChatRoom> = MutableLiveData<MegaChatRoom>()

    var waitingForMeetingLink: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    // Meeting
    private val _callLiveData: MutableLiveData<MegaChatCall?> = MutableLiveData<MegaChatCall?>()

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
            EVENT_CALL_STATUS_CHANGE,
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
     * If it's just me on the call
     *
     * @param chatId chat ID
     * @return True, if it's just me on the call. False, if there are more participants
     */
    fun isOnlyMeOnTheCall(chatId: Long): Boolean {
        if (isSameChatRoom(chatId)) {
            _callLiveData.value?.let {
                if (it.numParticipants == 1) {
                    val peerIds = it.peeridParticipants
                    peerIds?.let {
                        return isMe(peerIds.get(0))
                    }
                }
            }
        }
        return false
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

    /**
     * Get the session of a participant
     *
     * @param clientId client ID
     * @return MegaChatSession
     */
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
        return if (isCallOnHold()) {
            true
        } else isSessionOnHoldOfOneToOneCall()
    }

    /**
     * Method to know if a session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    private fun isSessionOnHoldOfOneToOneCall(): Boolean {
        _callLiveData.value?.let { call ->
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
     * Method to obtain the full name of a participant
     *
     * @param peerId the user handle
     * @return The name
     */
    fun getParticipantFullName(peerId: Long): String {
        chatRoom.value?.let { chat ->
            return CallUtil.getUserNameCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }
        return ""
    }

    /**
     * Method to find out if there is a participant in the call
     *
     * @param peerId Use handle
     * @param list of participants with this user handle
     */
    fun existsParticipants(peerId: Long): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()

        chatRoom.value?.let { chat ->
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()

                iterator.forEach {
                    when (it.peerId) {
                        peerId -> {
                            it.name = getParticipantFullName(peerId)
                            listWithChanges.add(it)
                        }
                    }
                }
            }
        }
        return listWithChanges
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
     * Method to know if a banner needs to be displayed and updated
     *
     */
    fun showBannerFixedBanner(
        bannerText: TextView?,
        peerId: Long,
        type: Int
    ): Boolean {
        when (type) {
            TYPE_JOIN -> {
                bannerText?.let {
                    it.setBackgroundColor(
                        ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            R.color.teal_300
                        )
                    )
                    it.text = StringResourcesUtils.getString(
                        R.string.contact_joined_the_call,
                        getParticipantFullName(peerId)
                    )
                    return true
                }
            }
            TYPE_LEFT -> {
                bannerText?.let {
                    it.setBackgroundColor(
                        ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            R.color.teal_300
                        )
                    )
                    it.text = StringResourcesUtils.getString(
                        R.string.contact_left_the_call,
                        getParticipantFullName(peerId)
                    )
                    return true
                }
            }
            TYPE_NETWORK_QUALITY -> {
                //Check local network quality
                _callLiveData.value?.let { call ->
                    val quality = call.networkQuality
                    if (quality == 0) {
                        bannerText?.let {
                            it.setBackgroundColor(
                                ContextCompat.getColor(
                                    MegaApplication.getInstance().applicationContext,
                                    R.color.amber_700_amber_300
                                )
                            )
                            it.text = StringResourcesUtils.getString(
                                R.string.slow_connection_meeting
                            )
                            return true
                        }
                    }
                }
            }

        }
        return false
    }

    /**
     * Method for displaying the correct banner: If the call is muted or on hold
     *
     * @return Banner text
     */
    fun showAppropriateBanner(bannerIcon: ImageView?, bannerText: TextView?): Boolean {
        //Check call or session on hold
        if (isCallOnHold() || isSessionOnHoldOfOneToOneCall()) {
            bannerIcon?.let {
                it.isVisible = false
            }
            bannerText?.let {
                it.text = StringResourcesUtils.getString(R.string.call_on_hold)
            }
            return true
        }

        //Check mute call or session
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
                                    )
                                )
                                return true
                            }

                        }
                    }
                }
            }
            if (!call.hasLocalAudio()) {
                bannerIcon?.let {
                    it.isVisible = false
                }
                bannerText?.let {
                    it.text =
                        StringResourcesUtils.getString(R.string.muted_own_micro)
                }
                return true
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

    /**
     * Method to know if the participant is a moderator.
     */
    private fun isParticipantModerator(peerId: Long): Boolean {
        chatRoom.value?.let {
            val privileges = it.getPeerPrivilegeByHandle(peerId)
            if (privileges == MegaChatRoom.PRIV_MODERATOR)
                return true
        }
        return false
    }

    /**
     * Method to know if the participant is my contact
     */
    private fun isMyContact(peerId: Long): Boolean {
        chatRoom.value?.let {
            return inMeetingRepository.isMyContact(it, peerId)
        }
        return false
    }

    /**
     * Method for updating participant privileges
     */
    fun updateParticipantsPrivileges(): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()

        chatRoom.value?.let { chat ->
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()

                iterator.forEach {
                    val isModerator = isParticipantModerator(it.peerId)
                    when {
                        it.isModerator != isModerator -> {
                            it.isModerator = isModerator
                            listWithChanges.add(it)
                        }
                    }
                }
            }
        }
        return listWithChanges
    }

    /**
     * Method for adding a participant
     */
    fun createParticipant(session: MegaChatSession) {
        chatRoom.value?.let {
            participants.value?.let { listParticipants ->
                val peer = listParticipants.filter {
                    it.peerId == session.peerid && it.clientId == session.clientid
                }
                if (!peer.isNullOrEmpty()) {
                    return
                }
            }

            val isModerator = isParticipantModerator(session.peerid)
            val isContact = isMyContact(session.peerid)
            val hasHiRes = session.isHiResVideo
            val userPeer = Participant(
                session.peerid,
                session.clientid,
                getParticipantFullName(session.peerid),
                null,
                "xxxx",
                false,
                isModerator,
                session.hasAudio(),
                session.hasVideo(),
                isContact,
                false,
                hasHiRes,
                null
            )

            participants.value?.add(userPeer)
            logDebug("Num of participants:" + participants.value?.size)
        }
    }

    /**
     * Method for removing a participant
     */
    fun removeParticipant(session: MegaChatSession) {
        chatRoom.value?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { participant ->
                participant.forEach {
                    if (it.peerId == session.peerid && it.clientId == session.clientid) {
                        participant.remove()
                    }
                }
            }
            logDebug("Num of participants:" + participants.value?.size)
        }
    }

    /**
     * Method for updating participant video
     */
    fun changesInRemoteVideoFlag(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid) {
                    if (it.isVideoOn != session.hasVideo()) {
                        it.isVideoOn = session.hasVideo()
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Method for updating participant audio
     */
    fun changesInRemoteAudioFlag(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid) {
                    if (it.isAudioOn != session.hasAudio()) {
                        it.isAudioOn = session.hasAudio()
                        return true
                    }
                }
            }
        }
        return false
    }

//    fun addParticipant(add: Boolean) {
//        if (add) {
//           participants.value!!.add(TestTool.testData()[Random.nextInt(TestTool.testData().size)])
//        } else {
//            if (participants.value!!.size > 2) {
//                participants.value!!.removeAt(participants.value!!.size - 1)
//            }
//        }
//        participants.value = participants.value
//    }
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

    /**
     * Determine the chat room has only one moderator
     *
     * @return
     */
    fun haveOneModerator(): Boolean {
        return participants.value?.toList()?.filter { it.isModerator }?.size?.let {
            it > 1
        } == true
    }
}