package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import android.util.Log
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
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.*
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT
import nz.mega.sdk.MegaChatError.ERROR_OK
import java.util.*

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback {

    var currentChatId: Long = MEGACHAT_INVALID_HANDLE
    private val _pinItemEvent = MutableLiveData<Event<Participant>>()
    val pinItemEvent: LiveData<Event<Participant>> = _pinItemEvent

    fun onItemClick(item: Participant) {
        _pinItemEvent.value = Event(item)
    }

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

    init {
        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)
    }

    /**
     * Method to know if this chat is public
     *
     * @return True, if it's public. False, otherwise
     */
    fun isChatRoomPublic(): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
        if (chatId != MEGACHAT_INVALID_HANDLE && currentChatId == chatId) {
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
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            return inMeetingRepository.getMeeting(it.chatId)
        }

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
        return inMeetingRepository.getChatRoom(currentChatId)
    }

    /**
     * Method to set a chat
     *
     * @param chatId chat ID
     */
    fun setChatId(chatId: Long) {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return

        currentChatId = chatId

        inMeetingRepository.getChatRoom(currentChatId)?.let {
            setCall(it.chatId)
            _chatTitle.value = getTitleChat(it)
        }
    }

    /**
     * Get the chat ID of the current meeting
     *
     * @return chat ID
     */
    fun getChatId(): Long {
        return currentChatId
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
        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
        return CallUtil.getUserNameCall(
            MegaApplication.getInstance().applicationContext,
            peerId
        )
    }

    /**
     * Method to find out if there is a participant in the call
     *
     * @param peerId Use handle
     * @param list of participants with this user handle
     */
    fun existsParticipants(peerId: Long): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
            TYPE_OWN_PRIVILEGE -> {
                bannerText?.let {
                    it.setBackgroundColor(
                        ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            R.color.teal_300
                        )
                    )
                    it.text = StringResourcesUtils.getString(
                        R.string.your_are_moderator
                    )
                    return true
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
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            //The chat exists
            inMeetingRepository.startCall(
                it.chatId,
                audioEnable,
                videoEnable,
                listener
            )
            return
        }

        //The chat doesn't exist
        inMeetingRepository.createMeeting(
            _chatTitle.value!!,
            CreateGroupChatWithPublicLink()
        )
    }

    fun getOwnPrivileges(): Int {
        return inMeetingRepository.getOwnPrivileges(currentChatId)
    }

    /**
     * Method to know if the participant is a moderator.
     */
    fun isParticipantModerator(peerId: Long): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            return inMeetingRepository.isMyContact(it, peerId)
        }
        return false
    }

    /**
     * Method for updating participant privileges
     */
    fun updateParticipantsPrivileges(): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()

        inMeetingRepository.getChatRoom(currentChatId)?.let {
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
    fun createParticipant(session: MegaChatSession): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val peer = listParticipants.filter {
                    it.peerId == session.peerid && it.clientId == session.clientid
                }
                if (!peer.isNullOrEmpty()) {
                    return false
                }
            }

            val isModerator = isParticipantModerator(session.peerid)
            val isContact = isMyContact(session.peerid)
            var hasHiRes = false
            participants.value?.let {
                when {
                    it.size < 4 -> {
                        hasHiRes = true
                    }
                }
            }

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
            return true
        }
        return false
    }

    /**
     * Method for checking which participants need to change their resolution
     */
    fun checkParticipantsResolution(isAdded: Boolean): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        participants.value?.let { listParticipants ->

            val iterator = listParticipants.iterator()
            iterator.forEach {
                when {
                    isAdded && listParticipants.size >= 5 -> {
                        when {
                            it.hasHiRes -> {
                                it.hasHiRes = false
                                when {
                                    it.isVideoOn -> {
                                        listWithChanges.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
                when {
                    !isAdded && listParticipants.size < 5 -> {
                        when {
                            !it.hasHiRes -> {
                                it.hasHiRes = true
                                when {
                                    it.isVideoOn -> {
                                        listWithChanges.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return listWithChanges
    }

    /**
     * Method for removing a participant
     */
    fun removeParticipant(session: MegaChatSession): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { participant ->
                participant.forEach {
                    when {
                        it.peerId == session.peerid && it.clientId == session.clientid -> {
                            participant.remove()
                            logDebug("Num of participants:" + participants.value?.size)
                            return true
                        }
                    }
                }
            }
        }
        return false
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
        if (currentChatId == MEGACHAT_INVALID_HANDLE) {
            _chatTitle.value = newTitle
        } else {
            inMeetingRepository.getChatRoom(currentChatId)?.let {
                inMeetingRepository.setTitleChatRoom(
                    it.chatId,
                    newTitle,
                    EditChatRoomNameListener(MegaApplication.getInstance(), this)
                )
            }
        }
    }

    /**
     * Add High Resolution
     */
    fun addHiRes(listener: MeetingVideoListener, session: MegaChatSession?, chatId: Long) {
        logDebug("Add HiRes")
        session?.let { sessionParticipant ->
            inMeetingRepository.addRemoteVideo(
                chatId,
                sessionParticipant.clientid,
                true,
                listener
            )

            when {
                !sessionParticipant.canRecvVideoHiRes() -> {
                    inMeetingRepository.requestHiResVideo(
                        chatId,
                        sessionParticipant.clientid,
                        RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                    )
                }
            }
        }
    }

    /**
     * Remove High Resolution
     */
    fun removeHiRes(listener: MeetingVideoListener, session: MegaChatSession?, chatId: Long) {
        logDebug("Add HiRes")
        session?.let { sessionParticipant ->
            when {
                sessionParticipant.canRecvVideoHiRes() -> {
                    inMeetingRepository.stopHiResVideo(
                        chatId,
                        sessionParticipant.clientid,
                        RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                    )
                }
            }

            inMeetingRepository.removeRemoteVideo(
                chatId,
                sessionParticipant.clientid,
                true,
                listener
            )
        }
    }

    /**
     * Add Low Resolution
     */
    fun addLowRes(listener: MeetingVideoListener, session: MegaChatSession?, chatId: Long) {
        logDebug("Add LowRes")
        session?.let { sessionParticipant ->
            inMeetingRepository.addRemoteVideo(
                chatId,
                sessionParticipant.clientid,
                false,
                listener
            )
            when {
                !sessionParticipant.canRecvVideoLowRes() -> {
                    val list: MegaHandleList = MegaHandleList.createInstance()
                    list.addMegaHandle(sessionParticipant.clientid)
                    inMeetingRepository.requestLowResVideo(
                        chatId,
                        list,
                        RequestLowResVideoListener(MegaApplication.getInstance().applicationContext)
                    )
                }
            }
        }
    }

    /**
     * Remove Low Resolution
     */
    fun removeLowRes(listener: MeetingVideoListener, session: MegaChatSession?, chatId: Long) {
        logDebug("Remove LowRes")
        session?.let { sessionParticipant ->
            when {
                sessionParticipant.canRecvVideoLowRes() -> {
                    val list: MegaHandleList = MegaHandleList.createInstance()
                    list.addMegaHandle(sessionParticipant.clientid)
                    inMeetingRepository.stopLowResVideo(
                        chatId,
                        list,
                        RequestLowResVideoListener(MegaApplication.getInstance().applicationContext)
                    )
                }
            }

            inMeetingRepository.removeRemoteVideo(
                chatId,
                sessionParticipant.clientid,
                false,
                listener
            )
        }
    }

    /**
     * Close Video
     *
     * @param participant
     */
    fun onCloseVideo(participant: Participant) {
        if (participant.videoListener == null)
            return

        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                when {
                    participant.hasHiRes -> removeHiRes(
                        participant.videoListener!!,
                        it,
                        chat.chatId
                    )
                    else -> removeLowRes(
                        participant.videoListener!!,
                        it,
                        chat.chatId
                    )
                }
            }
        }

        participant.videoListener = null
    }

    /**
     * Active Video
     *
     * @param participant
     */
    fun onActivateVideo(participant: Participant) {
        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                when {
                    participant.hasHiRes -> addHiRes(
                        participant.videoListener!!,
                        it,
                        chat.chatId
                    )
                    else -> addLowRes(
                        participant.videoListener!!,
                        it,
                        chat.chatId
                    )
                }
            }
        }
    }

    /**
     * Change video resolution
     *
     * @param participant
     */
    fun onChangeResolution(participant: Participant) {
        if (participant.videoListener == null)
            return

        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                if (participant.hasHiRes) {
                    //Change LowRes to HiRes
                    removeLowRes(participant.videoListener!!, it, chat.chatId)
                    addHiRes(participant.videoListener!!, it, chat.chatId)

                } else {
                    //Change HiRes to LowRes
                    removeHiRes(participant.videoListener!!, it, chat.chatId)
                    addLowRes(participant.videoListener!!, it, chat.chatId)
                }
            }
        }

        participant.videoListener = null
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        if (currentChatId == chatId) {
            _chatTitle.value = name
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


    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface){
        inMeetingRepository.joinPublicChat(chatId, listener)
    }

    fun createEphemeralAccountAndJoinChat(chatId: Long, firstName: String, lastName: String) {
        inMeetingRepository.createEphemeralAccountPlusPlus(firstName, lastName,
            object : BaseListener(MegaApplication.getInstance().applicationContext) {
                override fun onRequestFinish(
                    api: MegaApiJava, request: MegaRequest,
                    e: MegaError
                ) {
                    if (e.errorCode != MegaError.API_OK) {

                    }

                    joinPublicChat(chatId, AutoJoinPublicChatListener(context))
                }
            })
    }

    fun answerChatCall(
        audioEnable: Boolean,
        videoEnable: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            //The chat exists
            inMeetingRepository.answerCall(
                it.chatId,
                audioEnable,
                videoEnable,
                listener
            )
            return
        }
    }

    fun answerChatCall(
        enableVideo: Boolean,
        enableAudio: Boolean,
        callback: AnswerChatCallListener.OnCallAnsweredCallback
    ) = inMeetingRepository.answerCall(
        currentChatId,
        enableVideo,
        enableAudio,
        AnswerChatCallListener(
            MegaApplication.getInstance().applicationContext,
            callback
        )
    )

    fun getMyOwnInfo(audio: Boolean, video: Boolean): Participant =
        inMeetingRepository.getMyInfo(getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR, audio, video)
}
