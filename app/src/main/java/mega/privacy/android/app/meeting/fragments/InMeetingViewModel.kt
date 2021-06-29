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
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.listeners.GetUserEmailListener
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.InMeetingFragment.Companion.TYPE_IN_SPEAKER_VIEW
import mega.privacy.android.app.meeting.listeners.*
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall.*
import org.jetbrains.anko.defaultSharedPreferences

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback,
    HangChatCallListener.OnCallHungUpCallback, GetUserEmailListener.OnUserEmailUpdateCallback {

    var currentChatId: Long = MEGACHAT_INVALID_HANDLE
    var previousState: Int = CALL_STATUS_INITIAL

    var isSpeakerSelectionAutomatic: Boolean = true
    var isFromReconnectingStatus: Boolean = false
    var isReconnectingStatus: Boolean = false

    private val _pinItemEvent = MutableLiveData<Event<Participant>>()
    val pinItemEvent: LiveData<Event<Participant>> = _pinItemEvent

    fun onItemClick(item: Participant) {
        _pinItemEvent.value = Event(item)
    }

    private var waitingForMeetingLink: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    // Meeting
    private val _callLiveData = MutableLiveData<MegaChatCall?>(null)
    val callLiveData: LiveData<MegaChatCall?> = _callLiveData

    // Chat title
    private val _chatTitle: MutableLiveData<String> =
        MutableLiveData<String>(inMeetingRepository.getInitialMeetingName())
    val chatTitle: LiveData<String> = _chatTitle

    // List of participants in the meeting
    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    // List of visible participants in the meeting
    var visibleParticipants: MutableList<Participant> = mutableListOf()

    private val _speakerParticipant = MutableLiveData<Participant>(null)
    val speakerParticipant: LiveData<Participant> = _speakerParticipant

    private val updateCallObserver =
        Observer<MegaChatCall> {
            if (isSameChatRoom(it.chatid)) {
                _callLiveData.value = it
            }
        }

    private val updateCallStatusObserver =
        Observer<MegaChatCall> {
            if (isSameChatRoom(it.chatid)) {
                checkPreviousReconnectingStatus(it.status)
                checkReconnectingStatus(it.status)
                previousState = it.status

            }
        }

    init {
        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(updateCallStatusObserver)
    }

    /**
     * Method to check if I am in Reconnecting status
     *
     * @param currentStatus Status of the call
     */
    private fun checkReconnectingStatus(currentStatus: Int) {
        if (currentStatus == CALL_STATUS_CONNECTING) {
            if (previousState == CALL_STATUS_IN_PROGRESS || previousState == CALL_STATUS_JOINING || previousState == CALL_STATUS_CONNECTING) {
                isReconnectingStatus = true
                return
            }
        }

        isReconnectingStatus = false
    }

    /**
     * Method to check if I am coming back from the reconnected state
     *
     * @param currentStatus Status of the call
     */
    private fun checkPreviousReconnectingStatus(currentStatus: Int) {
        if (currentStatus == CALL_STATUS_JOINING || currentStatus == CALL_STATUS_IN_PROGRESS) {
            if (previousState == CALL_STATUS_CONNECTING && isReconnectingStatus) {
                isFromReconnectingStatus = true
            }
            return
        }

        isFromReconnectingStatus = false
    }


    /**
     * Method to know if this chat is public
     *
     * @return True, if it's public. False, otherwise
     */
    fun isChatRoomPublic(): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            return it.isPublic
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
        return chatId != MEGACHAT_INVALID_HANDLE && currentChatId == chatId
    }

    /**
     * Method to know if it is the same call
     *
     * @param callId call ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameCall(callId: Long): Boolean {
        _callLiveData.value?.let {
            return it.callId == callId
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
            _callLiveData.value?.let {
                if(it.status != CALL_STATUS_INITIAL && previousState == CALL_STATUS_INITIAL){
                    previousState = it.status
                }
            }
        }
    }

    /**
     * Method to get a call
     *
     * @return MegaChatCall
     */
    fun getCall(): MegaChatCall? {
        if (currentChatId == MEGACHAT_INVALID_HANDLE)
            return null

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
    fun amIAloneOnTheCall(chatId: Long): Boolean {
        if (isSameChatRoom(chatId)) {
            //Update call
            inMeetingRepository.getMeeting(currentChatId)?.let { call ->
                logDebug("Num participants in the meeting is ${call.numParticipants}")
                if (call.numParticipants == 0) {
                    logDebug("No participants in the call yet")
                    return true
                } else if (call.numParticipants == 1) {
                    val peerIds = call.peeridParticipants
                    peerIds?.let {
                        val isMe = isMe(it.get(0))
                        logDebug("I am the only participant in the call $isMe")
                        return isMe
                    }
                }
            }
        }

        logDebug("I am not the only participant in the call")
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
     * Set speaker selection automatic or manual
     *
     * @param isAutomatic True, if it's automatic. False, if it's manual
     */
    fun setSpeakerSelection(isAutomatic: Boolean) {
        isSpeakerSelectionAutomatic = isAutomatic
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
        if (clientId != MEGACHAT_INVALID_HANDLE) {
            _callLiveData.value?.let {
                return it.getMegaChatSession(clientId)
            }
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
        _callLiveData.value?.let { call ->
            if (call.isOnHold)
                return true

            val session = getSessionOneToOneCall(call)
            session?.let { sessionParticipant ->
                return sessionParticipant.isOnHold
            }

            if (!call.hasLocalVideo() && (session != null && !session.hasVideo())) {
                return true
            }
        }

        return false
    }

    /**
     *  Method to know if it is a one-to-one chat call
     *
     *  @return True, if it is a one-to-one chat call. False, otherwise
     */
    fun isOneToOneCall(): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            return !it.isGroup
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
            return call.isOnHold
        }

        return false
    }

    /**
     * Method to know if a call or session is on hold in meeting
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHold(clientId: Long): Boolean {
        if (isCallOnHold())
            return true

        getSession(clientId)?.let {
            return it.isOnHold
        }

        return false
    }

    /**
     * Method to know if a call or session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHoldOfOneToOneCall(): Boolean {
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
                    return it.isOnHold
                }
            }
        }

        return false
    }

    /**
     * Method to know if a call is on hold another call
     *
     * @param anotherCallChatId chat ID
     * @return True, if is on hold. False, otherwise
     */
    fun isAnotherCallOneToOneCall(anotherCallChatId: Long): Boolean {
        inMeetingRepository.getChatRoom(anotherCallChatId)?.let {
            return !it.isGroup
        }

        return false
    }

    /**
     * Method to know if a session is on hold in one to one another call
     *
     * @param anotherCall MegaChatCall
     * @return True, if is on hold. False, otherwise
     */
    fun isSessionOnHoldAnotherOneToOneCall(anotherCall: MegaChatCall): Boolean {
        anotherCall.let {
            val session = inMeetingRepository.getSessionOneToOneCall(anotherCall)
            session?.let { sessionParticipant ->
                return sessionParticipant.isOnHold
            }
        }

        return false
    }

    /**
     * Method to obtain a specific call
     *
     * @param chatId
     * @return MegaChatCall
     */
    private fun getAnotherCall(chatId: Long): MegaChatCall? {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return null

        return inMeetingRepository.getMeeting(chatId)
    }

    /**
     * Method to know if exists another call in progress or on hold.
     *
     * @return MegaChatCall
     */
    fun getAnotherCall(): MegaChatCall? {
        val anotherCallChatId = CallUtil.getAnotherCallParticipating(currentChatId)
        if (anotherCallChatId != MEGACHAT_INVALID_HANDLE) {
            val anotherCall = inMeetingRepository.getMeeting(anotherCallChatId)
            anotherCall?.let {
                if (isCallOnHold() && !it.isOnHold) {
                    logDebug("This call in on hold, another call in progress")
                    return anotherCall
                }

                if (!isCallOnHold() && it.isOnHold) {
                    logDebug("This call in progress, another call on hold")
                    return anotherCall
                }
            }

        }

        logDebug("No other calls in progress or on hold")
        return null
    }

    /**
     * Get session of a contact in a one-to-one call
     *
     * @param callChat MegaChatCall
     */
    fun getSessionOneToOneCall(callChat: MegaChatCall?): MegaChatSession? {
        return callChat?.getMegaChatSession(callChat.sessionsClientid[0])
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
     * @return list of participants with changes
     */
    fun updateParticipantsName(peerId: Long): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    if (it.peerId == peerId) {
                        it.name = getParticipantFullName(peerId)
                        listWithChanges.add(it)
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
     * Method to switch another call on hold
     *
     * @param isOn True, if I am going to put it on hold. False, otherwise
     */
    fun setAnotherCallOnHold(chatId: Long, isOn: Boolean) {
        inMeetingRepository.getChatRoom(chatId)?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isOn)
        }
    }

    /**
     * Method for determining whether a banner should be displayed
     *
     * @param type type of banner
     * @return True, if should be shown. False, otherwise.

     */
    fun shouldShowFixedBanner(type: Int): Boolean {
        when (type) {
            TYPE_RECONNECTING -> {
                _callLiveData.value?.let {
                    if (isReconnectingStatus) {
                        return true
                    }
                }
            }

            TYPE_NETWORK_QUALITY -> {
                _callLiveData.value?.let { call ->
                    val quality = call.networkQuality
                    if (quality == 0){
                        return true
                    }
                }
            }

            TYPE_SINGLE_PARTICIPANT -> {
                _callLiveData.value?.let {
                    if (it.status >= CALL_STATUS_JOINING && !isRequestSent() && amIAloneOnTheCall(
                            currentChatId
                        ) && it.numParticipants == 1 && isOnline(MegaApplication.getInstance().applicationContext)
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Method to show the appropriate banner
     *
     * @param bannerText the text of the banner to be edited
     * @param peerId user handle
     * @param type type of banner
     * @return True, if should be shown. False, otherwise.
     */
    fun updateFixedBanner(
        bannerText: TextView?,
        peerId: Long,
        type: Int
    ) {
        when (type) {
            TYPE_JOIN ->
                updateFixedBanner(
                    bannerText,
                    ContextCompat.getColor(
                        MegaApplication.getInstance().applicationContext,
                        R.color.teal_300
                    ),
                    StringResourcesUtils.getString(
                        R.string.contact_joined_the_call,
                        getParticipantFullName(peerId)
                    )
                )

            TYPE_LEFT ->
                updateFixedBanner(
                    bannerText,
                    ContextCompat.getColor(
                        MegaApplication.getInstance().applicationContext,
                        R.color.teal_300
                    ),
                    StringResourcesUtils.getString(
                        R.string.contact_left_the_call,
                        getParticipantFullName(peerId)
                    )
                )

            TYPE_RECONNECTING ->
                updateFixedBanner(
                    bannerText,
                    ContextCompat.getColor(
                        MegaApplication.getInstance().applicationContext,
                        R.color.amber_700_amber_300
                    ),
                    StringResourcesUtils.getString(R.string.reconnecting_message)
                )

            TYPE_NETWORK_QUALITY ->
                updateFixedBanner(
                    bannerText,
                    ContextCompat.getColor(
                        MegaApplication.getInstance().applicationContext,
                        R.color.amber_700_amber_300
                    ),
                    StringResourcesUtils.getString(R.string.slow_connection_meeting)
                )

            TYPE_SINGLE_PARTICIPANT ->
                updateFixedBanner(
                    bannerText,
                    ContextCompat.getColor(
                        MegaApplication.getInstance().applicationContext,
                        R.color.teal_300
                    ),
                    StringResourcesUtils.getString(R.string.banner_alone_on_the_call)
                )
        }
    }

    private fun updateFixedBanner(bannerText: TextView?, color: Int, text: String) {
        bannerText?.let {
            it.setBackgroundColor(color)
            it.text = text
        }
    }

    /**
     * Method to know if the session of a participants is null
     */
    fun isSessionOnHold(clientId: Long): Boolean {
        getSession(clientId)?.let {
            return it.isOnHold
        }

        return false
    }

    /**
     * Method for displaying the correct banner: If the call is muted or on hold
     *
     * @return Banner text
     */
    fun showAppropriateBanner(bannerIcon: ImageView?, bannerText: EmojiTextView?): Boolean {
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
                inMeetingRepository.getSessionOneToOneCall(call)?.let { session ->
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
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean {
        _callLiveData.value?.let {
            return it.status != CALL_STATUS_CONNECTING && it.hasLocalVideo() && !it.isOnHold
        }

        return false
    }

    /**
     * Method to start a meeting from create meeting
     *
     * @param videoEnable if the video is enable
     * @param audioEnable if the audio is enable
     * @param listener MegaChatRequestListenerInterface
     */
    fun startMeeting(
        videoEnable: Boolean,
        audioEnable: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            logDebug("The chat exists")
            if (CallUtil.isStatusConnected(
                    MegaApplication.getInstance().applicationContext,
                    it.chatId
                )
            ) {
                logDebug("Chat status is connected")
                inMeetingRepository.startCall(
                    it.chatId,
                    videoEnable,
                    audioEnable,
                    listener
                )

                MegaApplication.setIsWaitingForCall(false)
            }
            return
        }

        logDebug("The chat doesn't exists")
        inMeetingRepository.createMeeting(
            _chatTitle.value!!,
            CreateGroupChatWithPublicLink()
        )
    }

    /**
     * Get my own privileges in the chat
     *
     * @return the privileges
     */
    fun getOwnPrivileges(): Int {
        return inMeetingRepository.getOwnPrivileges(currentChatId)
    }

    /**
     * Method to know if the participant is a moderator.
     */
    fun isParticipantModerator(peerId: Long): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            val privileges = it.getPeerPrivilegeByHandle(peerId)
            return privileges == MegaChatRoom.PRIV_MODERATOR
        }

        return false
    }

    /**
     * Method to know if the participant is my contact
     */
    private fun isMyContact(peerId: Long): Boolean {
        return inMeetingRepository.isMyContact(peerId)
    }

    /**
     * Method to update whether a user is my contact or not
     *
     * @param peerId User handle
     */
    fun updateParticipantsVisibility(peerId: Long) {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    if (it.peerId == peerId) {
                        it.isContact = isMyContact(peerId)
                    }
                }
            }
        }
    }

    /**
     * Method for updating participant privileges
     *
     * @return list of participants with changes
     */
    fun updateParticipantsPrivileges(): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    val isModerator = isParticipantModerator(it.peerId)
                    if (it.isModerator != isModerator) {
                        it.isModerator = isModerator
                        listWithChanges.add(it)
                    }
                }
            }
        }

        return listWithChanges
    }

    /**
     * Method for updating the speaking participant
     *
     * @param peerId
     * @param clientId
     * @return list of participants with changes
     */
    fun updatePeerSelected(peerId: Long, clientId: Long): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()

        participants.value?.forEach {
            if (it.peerId == peerId && it.clientId == clientId) {
                logDebug("New speaker selected found ${it.clientId}")
                it.isSpeaker = true
                _speakerParticipant.value = createSpeakerParticipant(it)
                listWithChanges.add(it)
            } else {
                if (it.isSpeaker) {
                    logDebug("Remove the last speaker ${it.clientId}")
                    it.isSpeaker = false
                    listWithChanges.add(it)
                }
            }
        }

        return listWithChanges
    }

    /**
     * Method that creates the participant speaker
     *
     * @param participant
     * @return speaker participant
     */
    private fun createSpeakerParticipant(participant: Participant): Participant {
        val peerId = participant.peerId
        val clientId = participant.clientId
        val name = participant.name
        val avatar = participant.avatar
        val isAudioOn = participant.isAudioOn
        val isVideoOn = participant.isVideoOn
        val isChosenForAssign = participant.isChosenForAssign
        val isGuest = participant.isGuest

        return Participant(
            peerId,
            clientId,
            name,
            avatar,
            isMe = false,
            isModerator = false,
            isAudioOn = isAudioOn,
            isVideoOn = isVideoOn,
            isContact = false,
            isSpeaker = true,
            hasHiRes = true,
            videoListener = null,
            isChosenForAssign,
            isGuest
        )
    }

    /**
     * Method for creating participants already on the call
     *
     * @param list list of participants
     * @param status if it's grid view or speaker view
     */
    fun createCurrentParticipants(list: MegaHandleList, status: String) {
        _callLiveData.value = inMeetingRepository.getMeeting(currentChatId)
        for (i in 0 until list.size()) {
            getSession(list[i])?.let { session ->
                createParticipant(session, status)?.let { participantCreated ->
                    logDebug("Adding current participant...")
                    participants.value?.add(participantCreated)
                }
            }
        }

        participants.value = participants.value
        logDebug("Num of participants:" + participants.value?.size)
    }

    /**
     * Method for adding a participant to the list
     *
     * @param session MegaChatSession
     * @return the position of the participant
     */
    fun addParticipant(session: MegaChatSession, status: String): Int? {
        createParticipant(session, status)?.let { participantCreated ->
            participants.value?.add(participantCreated)
            logDebug("Adding participant...")
            participants.value = participants.value
            logDebug("Num of participants:" + participants.value?.size)
            return participants.value?.indexOf(participantCreated)
        }

        return INVALID_POSITION
    }

    /**
     * Method for create a participant
     *
     * @param session MegaChatSession
     * @return the position of the participant
     */
    private fun createParticipant(session: MegaChatSession, status: String): Participant? {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val peer = listParticipants.filter { participant ->
                    participant.peerId == session.peerid && participant.clientId == session.clientid
                }

                if (!peer.isNullOrEmpty()) {
                    logDebug("Participants exists")
                    return null
                }
            }

            val isModerator = isParticipantModerator(session.peerid)
            val isContact = isMyContact(session.peerid)
            val hasHiRes = needHiRes(status)
            val name = getParticipantName(session.peerid)
            val avatar = inMeetingRepository.getAvatarBitmap(it, session.peerid)
            val email = inMeetingRepository.getEmailParticipant(
                session.peerid,
                GetUserEmailListener(MegaApplication.getInstance().applicationContext, this)
            )
            var isGuest = false
            if (email == null) {
                isGuest = true
            }

            val newParticipant = Participant(
                session.peerid,
                session.clientid,
                name,
                avatar,
                false,
                isModerator,
                session.hasAudio(),
                session.hasVideo(),
                isContact,
                false,
                hasHiRes,
                null,
                false,
                isGuest
            )

            logDebug("Participant created")
            return newParticipant
        }

        return null
    }

    /**
     * Method for removing a participant
     *
     * @param session MegaChatSession
     * @return the position of the participant
     */
    fun removeParticipant(session: MegaChatSession): Int? {
        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            val iterator = participants.value?.iterator()
            iterator?.let { list ->
                list.forEach {
                    if (it.peerId == session.peerid && it.clientId == session.clientid) {
                        if (it.isSpeaker) {
                            it.isSpeaker = false
                            removeSpeakerParticipant()
                            assignMeAsSpeaker()
                        }

                        val position = participants.value?.indexOf(it)
                        if (it.isVideoOn) {
                            removeVideoOfParticipantRemoved(chat.chatId, it)
                        }
                        list.remove()
                        logDebug("Removing participant...")
                        participants.value = participants.value
                        logDebug("Num of participants:" + participants.value?.size)
                        return position
                    }
                }
            }
        }

        return INVALID_POSITION
    }

    /**
     * Method to delete the video of a participant who has left the call and it doesn't have session
     *
     * @param chatId
     * @param participant
     */
    private fun removeVideoOfParticipantRemoved(chatId: Long, participant: Participant) {
        if (participant.videoListener == null)
            return

        if (participant.hasHiRes) {
            val list: MegaHandleList = MegaHandleList.createInstance()
            list.addMegaHandle(participant.clientId)
            inMeetingRepository.stopHiResVideo(
                chatId,
                list,
                RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
            )
        } else {
            val list: MegaHandleList = MegaHandleList.createInstance()
            list.addMegaHandle(participant.clientId)
            inMeetingRepository.stopLowResVideo(
                chatId,
                list,
                RequestLowResVideoListener(MegaApplication.getInstance().applicationContext)
            )
        }
        inMeetingRepository.removeRemoteVideo(
            chatId,
            participant.clientId,
            participant.hasHiRes,
            participant.videoListener!!
        )
    }

    /**
     * Method for know if the resolution of a participant's video should be high
     *
     * @param status if it's grid view or speaker view
     * @return True, if should be high. False, otherwise
     */
    private fun needHiRes(status: String): Boolean {
        participants.value?.let {
            return status != TYPE_IN_SPEAKER_VIEW
        }

        return false
    }

    /**
     * Method for get the participant name
     *
     * @param peerId user handle
     * @return the name
     */
    private fun getParticipantName(peerId: Long): String {
        return inMeetingRepository.participantName(peerId)
    }

    /**
     * Method for checking which participants need to change their resolution when participant is added or removed
     *
     * In Speaker view, the list of participants should have low res
     * In Grid view, if there is more than 4, low res. Hi res in the opposite case
     *
     * @param status if it's Speaker view or Grid view
     */
    fun checkParticipantsResolution(status: String) {
        logDebug("Check participants resolution")
        participants.value?.let { listParticipants ->
            val iterator = listParticipants.iterator()
            iterator.forEach {
                if (status == TYPE_IN_SPEAKER_VIEW) {
                    logDebug("Change to low resolution ")
                    if (it.hasHiRes) {
                        it.hasHiRes = false
                        if (it.isVideoOn) {
                            logDebug("Change resolution. HiRes to LowRes")
                            onChangeResolution(it)
                        }
                    }
                } else {
                    logDebug("Change to high resolution ")
                    if (!it.hasHiRes) {
                        it.hasHiRes = true
                        if (it.isVideoOn) {
                            onChangeResolution(it)
                        }
                    }
                }
            }
        }
    }

    fun removeSelected(peerId: Long, clientId: Long) {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == peerId && it.clientId == clientId) {
                    if (it.isSpeaker) {
                        it.isSpeaker = false
                    }
                }
            }
        }
    }

    /**
     * Get the avatar
     *
     * @param peerId
     * @return the avatar
     */
    fun getAvatarBitmap(peerId: Long): Bitmap? {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            return inMeetingRepository.getAvatarBitmap(it, peerId)
        }

        return null
    }

    fun removeSpeakerParticipant(){
        _speakerParticipant.value?.let {
            if (getSession(it.clientId) == null) {
                logDebug("The participant with clientId ${it.clientId} doesn't have session")
                removeVideoOfParticipantRemoved(currentChatId, it)
            } else {
                logDebug("The participant with clientId ${it.clientId} has session")
                onCloseVideo(it)
            }
        }
    }

    /**
     * Method for assigning me as a speaker.
     * It's necessary to close the previous speaker's video and assign me as the speaker
     *
     */
    fun assignMeAsSpeaker() {
        logDebug("Assign me as speaker")
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            _speakerParticipant.value = inMeetingRepository.getMeToSpeakerView(it)
        }
    }

    /**
     * Get participant from peerId and clientId
     *
     * @param peerId peer ID
     * @param clientId client ID
     */
    fun getParticipant(peerId: Long, clientId: Long): Participant? {
        participants.value?.let { list ->
            val participants = list.filter {
                it.peerId == peerId && it.clientId == clientId
            }

            if (participants.isNotEmpty()) {
                return participants[0]
            }
        }

        return null
    }

    /**
     * Method for updating participant video
     *
     * @param session
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteVideoFlag(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid && it.isVideoOn != session.hasVideo()) {
                    it.isVideoOn = session.hasVideo()
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method for updating low resolution
     *
     * @param session
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInLowRes(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid && !it.hasHiRes && it.isVideoOn) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method for updating high resolution
     *
     * @param session
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInHiRes(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid && it.hasHiRes && it.isVideoOn) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method for updating participant audio
     *
     * @param session
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteAudioFlag(session: MegaChatSession): Boolean {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == session.peerid && it.clientId == session.clientid && it.isAudioOn != session.hasAudio()) {
                    it.isAudioOn = session.hasAudio()
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method that makes the necessary checks before joining a meeting.
     * If there is another call, it must be put on hold.
     * If there are two other calls, the one in progress is hung up.
     *
     * @param chatIdOfCurrentCall chat id of current call
     */
    fun checkAnotherCallsInProgress(chatIdOfCurrentCall: Long) {
        val numCallsParticipating = CallUtil.getCallsParticipating()
        numCallsParticipating?.let {
            if (numCallsParticipating.isEmpty()) {
                return
            }

            if (numCallsParticipating.size == 1) {
                getAnotherCall(numCallsParticipating[0])?.let { anotherCall ->
                    if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                        logDebug("Another call on hold before join the meeting")
                        setAnotherCallOnHold(anotherCall.chatid, true)
                    }
                }
            } else {
                for (i in 0 until numCallsParticipating.size) {
                    getAnotherCall(numCallsParticipating[i])?.let { anotherCall ->
                        if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                            logDebug("Hang up one of the current calls in order to join the meeting")
                            hangUpSpecificCall(anotherCall.callId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Method for ignore a call
     */
    fun ignoreCall() {
        _callLiveData.value?.let {
            inMeetingRepository.ignoreCall(it.chatid)
        }
    }

    /**
     * Method to hang up a specific call
     *
     * @param callId
     */
    private fun hangUpSpecificCall(callId: Long) {
        inMeetingRepository.leaveMeeting(
            callId,
            HangChatCallListener(MegaApplication.getInstance(), this)
        )
    }

    /**
     * Method for leave the meeting
     */
    fun leaveMeeting() {
        _callLiveData.value?.let {
            inMeetingRepository.leaveMeeting(
                it.callId,
                HangChatCallListener(MegaApplication.getInstance(), this)
            )
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
     * Method of obtaining the video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun addLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener?) {
        if (listener == null)
            return

        logDebug("Adding local video")
        inMeetingRepository.addLocalVideoSpeaker(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun removeLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener?) {
        if (listener == null)
            return

        logDebug("Removing local video")
        inMeetingRepository.removeLocalVideoSpeaker(chatId, listener)
    }

    /**
     * Add High Resolution for remote video in a one to one call
     *
     * @param listener
     * @param session
     * @param chatId
     */
    fun addHiResOneToOneCall(
        listener: MeetingVideoListener,
        session: MegaChatSession?,
        chatId: Long
    ) {
        session?.let { sessionParticipant ->
            if (!sessionParticipant.canRecvVideoHiRes()) {
                logDebug("Adding HiRes video, clientId ${sessionParticipant.clientid}")
                inMeetingRepository.addRemoteVideoOneToOneCall(
                    chatId,
                    sessionParticipant.clientid,
                    true,
                    listener
                )

                inMeetingRepository.requestHiResVideo(
                    chatId,
                    sessionParticipant.clientid,
                    RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                )
            }
        }
    }

    /**
     * Remove High Resolution for remote video in a one to one call
     *
     * @param listener
     * @param session
     * @param chatId
     */
    fun removeHiResOneToOneCall(
        listener: MeetingVideoListener,
        session: MegaChatSession?,
        chatId: Long
    ) {
        session?.let { sessionParticipant ->
            if (sessionParticipant.canRecvVideoHiRes()) {
                logDebug("Removing HiRes video, clientId ${sessionParticipant.clientid}")
                val list: MegaHandleList = MegaHandleList.createInstance()
                list.addMegaHandle(sessionParticipant.clientid)
                inMeetingRepository.stopHiResVideo(
                    chatId,
                    list,
                    RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                )

                inMeetingRepository.removeRemoteVideoOneToOneCall(
                    chatId,
                    sessionParticipant.clientid,
                    true,
                    listener
                )
            }
        }
    }

    /**
     * Add High Resolution for remote video in a meeting
     *
     * @param listener
     * @param session
     * @param chatId
     */
    private fun addHiRes(listener: GroupVideoListener, session: MegaChatSession?, chatId: Long) {
        session?.let { sessionParticipant ->
            if (!sessionParticipant.canRecvVideoHiRes()) {
                logDebug("Adding HiRes video, clientId ${sessionParticipant.clientid}")
                inMeetingRepository.addRemoteVideo(
                    chatId,
                    sessionParticipant.clientid,
                    true,
                    listener
                )

                inMeetingRepository.requestHiResVideo(
                    chatId,
                    sessionParticipant.clientid,
                    RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                )
            }
        }
    }

    /**
     * Remove High Resolution for remote video in a meeting
     *
     * @param listener
     * @param session
     * @param chatId
     */
    private fun removeHiRes(listener: GroupVideoListener, session: MegaChatSession?, chatId: Long) {
        session?.let { sessionParticipant ->
            if (sessionParticipant.canRecvVideoHiRes()) {
                logDebug("Removing HiRes video, clientId ${sessionParticipant.clientid}")
                val list: MegaHandleList = MegaHandleList.createInstance()
                list.addMegaHandle(sessionParticipant.clientid)
                inMeetingRepository.stopHiResVideo(
                    chatId,
                    list,
                    RequestHiResVideoListener(MegaApplication.getInstance().applicationContext)
                )

                inMeetingRepository.removeRemoteVideo(
                    chatId,
                    sessionParticipant.clientid,
                    true,
                    listener
                )
            }
        }
    }

    /**
     * Add Low Resolution for remote video in a meeting
     *
     * @param listener
     * @param session
     * @param chatId
     */
    private fun addLowRes(listener: GroupVideoListener, session: MegaChatSession?, chatId: Long) {
        session?.let { sessionParticipant ->
            if (!sessionParticipant.canRecvVideoLowRes()) {
                logDebug("Adding LowRes video, clientId ${sessionParticipant.clientid}")
                inMeetingRepository.addRemoteVideo(
                    chatId,
                    sessionParticipant.clientid,
                    false,
                    listener
                )

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

    /**
     * Remove Low Resolution for remote video in a meeting
     *
     * @param listener
     * @param session
     * @param chatId
     */
    private fun removeLowRes(
        listener: GroupVideoListener,
        session: MegaChatSession?,
        chatId: Long
    ) {
        session?.let { sessionParticipant ->
            if (sessionParticipant.canRecvVideoLowRes()) {
                logDebug("Removing LowRes video, clientId ${sessionParticipant.clientid}")
                val list: MegaHandleList = MegaHandleList.createInstance()
                list.addMegaHandle(sessionParticipant.clientid)
                inMeetingRepository.stopLowResVideo(
                    chatId,
                    list,
                    RequestLowResVideoListener(MegaApplication.getInstance().applicationContext)
                )

                inMeetingRepository.removeRemoteVideo(
                    chatId,
                    sessionParticipant.clientid,
                    false,
                    listener
                )
            }
        }
    }

    /**
     * Activate Video of participant in a meeting
     *
     * @param participant
     */
    fun onActivateVideo(participant: Participant, isSpeaker: Boolean) {
        val session = getSession(participant.clientId)

        if (session == null || participant.videoListener == null) return

        val isVisible = isParticipantVisible(participant)
        if (!isVisible && !isSpeaker) {
            logDebug("No activate video, the participant with clientId ${participant.clientId} is not visible")
            return
        }

        logDebug("Activate video, the participant with clientId ${participant.clientId} is visible")
        if (participant.hasHiRes) {
            if (!isSpeaker) {
                logDebug("Remove lowRes before request highRes of ${participant.clientId}")
                removeLowRes(participant.videoListener!!, session, currentChatId)
            }

            logDebug("Add high resolution of ${participant.clientId}")
            addHiRes(
                participant.videoListener!!,
                session,
                currentChatId
            )
        } else {
            if (!isSpeaker) {
                logDebug("Remove highRes before request lowRes of ${participant.clientId}")
                removeHiRes(participant.videoListener!!, session, currentChatId)
            }

            if (session.hasVideo()) {
                logDebug("The session had no video, check and delete if lowRes was allowed by default.")
                removeLowRes(participant.videoListener!!, session, currentChatId)
            }

            logDebug("Add low resolution of ${participant.clientId}")
            addLowRes(
                participant.videoListener!!,
                session,
                currentChatId
            )
        }
    }

    /**
     * Close Video of participant in a meeting
     *
     * @param participant
     */
    fun onCloseVideo(participant: Participant) {
        if (participant.videoListener == null) return

        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                logDebug("Close video of ${participant.clientId}")
                when {
                    participant.hasHiRes -> {
                        removeHiRes(
                            participant.videoListener!!,
                            it,
                            chat.chatId
                        )
                    }
                    else -> {
                        removeLowRes(
                            participant.videoListener!!,
                            it,
                            chat.chatId
                        )
                    }
                }
            }
        }
    }

    /**
     * Method for checking which participants need to change their resolution when the UI is changed
     *
     * In Speaker view, the list of participants should have low res
     * In Grid view, if there is more than 4, low res. Hi res in the opposite case
     *
     * @param status if it's Speaker view or Grid view
     */
    fun removeParticipantResolution(status: String) {
        logDebug("Changing the resolution of participants when the UI changes")
        participants.value?.let { listParticipants ->
            val iterator = listParticipants.iterator()
            iterator.forEach {
                if (status == TYPE_IN_SPEAKER_VIEW) {
                    logDebug("Change to low resolution ")
                    if (it.hasHiRes) {
                        it.hasHiRes = false
                        if (it.isVideoOn) {
                            removeSpecificResolution(it)
                        }
                    }
                } else {
                    logDebug("Change to high resolution ")
                    if (!it.hasHiRes) {
                        it.hasHiRes = true
                        if (it.isVideoOn) {
                            removeSpecificResolution(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Change video resolution
     *
     * @param participant
     */
    private fun removeSpecificResolution(participant: Participant) {
        if (participant.videoListener == null)
            return

        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                if (participant.hasHiRes) {
                    removeLowRes(participant.videoListener!!, it, chat.chatId)
                    if (it.canRecvVideoLowRes()) {
                        logDebug("Participant ${participant.clientId} video listener null")
                        participant.videoListener = null
                    }
                } else {
                    removeHiRes(participant.videoListener!!, it, chat.chatId)
                    if (it.canRecvVideoHiRes()) {
                        logDebug("Participant ${participant.clientId} video listener null")
                        participant.videoListener = null
                    }
                }
            }
        }
    }

    /**
     * Adding visible participant
     *
     * @param participant
     */
    fun addParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            visibleParticipants.add(participant)
            return
        }

        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }
        if (checkParticipant.isEmpty()) {
            visibleParticipants.add(participant)
        }
    }

    /**
     * Removing visible participant
     *
     * @param participant
     */
    fun removeParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            return
        }
        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }
        if (checkParticipant.isNotEmpty()) {
            visibleParticipants.remove(participant)
        }
    }

    /**
     * Check if a participant is visible
     *
     * @param participant
     * @return True, if it's visible. False, otherwise
     */
    private fun isParticipantVisible(participant: Participant): Boolean {
        if (visibleParticipants.isNotEmpty()) {
            val participantVisible = visibleParticipants.filter {
                it.peerId == participant.peerId && it.clientId == participant.clientId
            }

            if (participantVisible.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    /**
     * Removing all visible participants
     */
    fun removeVisibleParticipants() {
        visibleParticipants.clear()
    }

    /**
     * Updating visible participants list
     *
     * @param list new list of visible participants
     */
    fun updateVisibleParticipants(list: List<Participant>?) {
        if (list != null && list.isNotEmpty()) {
            val iteratorParticipants = list.iterator()
            iteratorParticipants.forEach { participant ->
                addParticipantVisible(participant)
            }
            logDebug("Num visible participants is " + visibleParticipants.size)
        }
    }

    /**
     * Change video resolution
     *
     * @param participant
     */
    private fun onChangeResolution(participant: Participant) {
        if (participant.videoListener == null)
            return

        inMeetingRepository.getChatRoom(currentChatId)?.let { chat ->
            getSession(participant.clientId)?.let {
                if (participant.hasHiRes) {
                    logDebug("Change resolution. LowRes to HiRes")
                    removeLowRes(participant.videoListener!!, it, chat.chatId)
                    addHiRes(participant.videoListener!!, it, chat.chatId)
                } else {
                    logDebug("Change resolution. HiRes to LowRes")
                    removeHiRes(participant.videoListener!!, it, chat.chatId)
                    addLowRes(participant.videoListener!!, it, chat.chatId)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .removeObserver(updateCallStatusObserver)
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        if (currentChatId == chatId) {
            _chatTitle.value = name
        }
    }

    /**
     * Determine the chat room has only one moderator and the list is not empty and I am moderator
     *
     * @return
     */
    fun shouldAssignModerator(): Boolean {
        val hasOneModerator = participants.value?.toList()?.filter { it.isModerator }?.size?.let {
            when {
                it > 1 -> false
                it == 1 -> getOwnPrivileges() != MegaChatRoom.PRIV_MODERATOR
                else -> getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR
            }
        } == true

        return hasOneModerator && isModerator() && participants.value?.none { isStandardUser(it.peerId) } == false
    }

    // For "join as guest" start
    fun chatLogout(listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.chatLogout(listener)

    fun createEphemeralAccountAndJoinChat(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface
    ) = inMeetingRepository.createEphemeralAccountPlusPlus(firstName, lastName, listener)

    fun fetchNodes(listener: MegaRequestListenerInterface) =
        inMeetingRepository.fetchNodes(listener)

    fun chatConnect(listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.chatConnect(listener)

    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.openChatPreview(link, listener)

    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.joinPublicChat(chatId, listener)

    fun registerConnectionUpdateListener(chatId: Long, callback: () -> Unit) =
        inMeetingRepository.registerConnectionUpdateListener(chatId, callback)
    // For join as guest end

    /**
     * Method for answer a call
     *
     * @param videoEnable if video should be on
     * @param audioEnable if audio should be on
     * @param listener MegaChatRequestListenerInterface
     */
    fun answerChatCall(
        videoEnable: Boolean,
        audioEnable: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            logDebug("The chat exists")
            inMeetingRepository.answerCall(
                it.chatId,
                videoEnable,
                audioEnable,
                listener
            )
            return
        }
    }

    /**
     * Get my own information
     *
     * @param audio local audio
     * @param video local video
     * @return
     */
    fun getMyOwnInfo(audio: Boolean, video: Boolean): Participant =
        inMeetingRepository.getMyInfo(
            getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR,
            audio,
            video
        )

    /**
     * Determine if should hide or show the share link and invite button
     *
     * @return
     */
    fun isLinkVisible(): Boolean =
        isChatRoomPublic() && getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR

    /**
     * Determine if should hide or show the guest share link button
     *
     * @return
     */
    fun isGuestLinkVisible(): Boolean = if (isChatRoomPublic()) {
        getOwnPrivileges() != MegaChatRoom.PRIV_MODERATOR
    } else {
        getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR
    }

    fun getGuestLinkTitle(): String = if (isModeratorOfPrivateRoom()) {
        StringResourcesUtils.getString(R.string.invite_participants)
    } else {
        StringResourcesUtils.getString(R.string.context_get_link)
    }

    fun isModeratorOfPrivateRoom(): Boolean =
        !isChatRoomPublic() && getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR

    /**
     * Determine if I am a guest
     *
     * @return True, if I am a guest. False if not
     */
    fun amIAGuest(): Boolean {
        return inMeetingRepository.amIAGuest()
    }

    fun isStandardUser(peerId: Long): Boolean {
        inMeetingRepository.getChatRoom(currentChatId)?.let {
            val privileges = it.getPeerPrivilegeByHandle(peerId)
            return privileges == MegaChatRoom.PRIV_STANDARD
        }

        return false
    }

    /**
     * Determine if I am a moderator
     *
     * @return
     */
    fun isModerator(): Boolean =
        getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR


    fun updateChatPermissions(
        peerId: Long,
        listener: MegaChatRequestListenerInterface? = null
    ) {
        inMeetingRepository.updateChatPermissions(currentChatId, peerId, listener)
    }

    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        return inMeetingRepository.getAvatarBitmapByPeerId(peerId)
    }

    fun shouldShowTips(): Boolean {
        val sharedPreferences =
            MegaApplication.getInstance().applicationContext.defaultSharedPreferences
        return !sharedPreferences.getBoolean(IS_SHOWED_TIPS, false)
    }

    fun updateShowTips() {
        val sharedPreferences =
            MegaApplication.getInstance().applicationContext.defaultSharedPreferences
        val editor = sharedPreferences.edit()
        editor.putBoolean(IS_SHOWED_TIPS, true).commit()
    }

    companion object {
        const val IS_SHOWED_TIPS = "is_showed_meeting_bottom_tips"
    }

    override fun onCallHungUp(callId: Long) {
        _callLiveData.value?.let {
            if (it.callId == callId) {
                logDebug("Current call hung up")
                _callLiveData.value = null
                currentChatId = MEGACHAT_INVALID_HANDLE
            }
        }
    }

    override fun onUserEmailUpdate(email: String?, handler: Long, position: Int) {
        if (email == null)
            return

        inMeetingRepository.getChatRoom(currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    if (it.peerId == handler) {
                        it.isGuest = false
                    }
                }
            }
        }
    }
}

