package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.ChatConnectionListener
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApi.INIT_WAITING_NEW_SESSION
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {

    /**
     * Get the initial name of the meeting created
     *
     * @return String meeting's name
     */
    fun getInitialMeetingName(): String {
        return StringResourcesUtils.getString(
            R.string.type_meeting_name, megaChatApi.myFullname
        )
    }

    /**
     * Set a title for a chat
     *
     * @param chatId chat ID
     * @param newTitle new chat title
     * @param listener MegaChatRequestListenerInterface
     */
    fun setTitleChatRoom(
        chatId: Long,
        newTitle: String,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.setChatTitle(chatId, newTitle, listener)
    }

    /**
     * Method for starting a meeting
     *
     * @param chatId chat ID
     * @param enableVideo if Video is enabled
     * @param enableAudio if Audio is enabled
     * @param listener MegaChatRequestListenerInterface
     */
    fun startCall(
        chatId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Starting call with video enable $enableVideo and audio enable $enableAudio")
        megaChatApi.startChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Method for starting a meeting
     *
     * @param chatId chat ID
     * @param enableAudio if Audio is enabled
     * @param enableVideo if Video is enabled
     * @param listener MegaChatRequestListenerInterface
     */
    fun answerCall(
        chatId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Answering call with video enable $enableVideo and audio enable $enableAudio")
        megaChatApi.answerChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Get a call from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatCall
     */
    fun getMeeting(chatId: Long): MegaChatCall? {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return null

        return megaChatApi.getChatCall(chatId)
    }

    /**
     * Method to know if it's me
     *
     * @param peerId The handle
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean {
        return peerId == megaApi.myUserHandleBinary
    }


    /**
     * Get the session in a one to one call
     *
     * @param call The MegaChatCall
     * @return MegaChatSession
     */
    fun getSessionOneToOneCall(call: MegaChatCall): MegaChatSession? {
        val clientId = call.sessionsClientid?.get(0)
        clientId?.let {
            return call.getMegaChatSession(it)
        }

        return null
    }

    /**
     * Get a chat from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatRoom
     */
    fun getChatRoom(chatId: Long): MegaChatRoom? {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return null

        return megaChatApi.getChatRoom(chatId)
    }

    /**
     * Get contact name
     *
     * @param peerId contact handle
     * @return String The contact's name
     */
    fun getContactOneToOneCallName(peerId: Long): String {
        val name: String =
            ChatController(MegaApplication.getInstance().applicationContext).getParticipantFirstName(
                peerId
            )
        if (TextUtil.isTextEmpty(name)) {
            return megaChatApi.getContactEmail(peerId)
        }

        return name
    }

    /**
     * Method for creating a meeting
     *
     * @param meetingName the name of the meeting
     * @param listener MegaChatRequestListenerInterface
     */
    fun createMeeting(meetingName: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.createMeeting(meetingName, listener)

    /**
     * Method to switch a call on hold
     *
     * @param chatId chat ID
     * @param isOn True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(chatId: Long, isOn: Boolean) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            megaChatApi.setCallOnHold(chatId, isOn, SetCallOnHoldListener(context))
        }
    }

    /**
     * Method for ignore a call
     *
     * @param chatId chat ID
     */
    fun ignoreCall(chatId: Long) {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return

        megaChatApi.setIgnoredCall(chatId)
        MegaApplication.getInstance().stopSounds()
        CallUtil.clearIncomingCallNotification(chatId)
    }

    /**
     * Method for leave a meeting
     *
     * @param callId call ID
     */
    fun leaveMeeting(callId: Long, listener: MegaChatRequestListenerInterface) {
        if (callId == MEGACHAT_INVALID_HANDLE)
            return

        megaChatApi.hangChatCall(callId, listener)
    }

    /**
     * Method for getting a participant's email
     *
     * @param peerId userHandle
     * @param listener MegaRequestListenerInterface
     * @return the email
     */
    fun getEmailParticipant(peerId: Long, listener: MegaRequestListenerInterface): String? {
        val email = megaChatApi.getUserEmailFromCache(peerId)

        if (email != null)
            return email

        megaApi.getUserEmail(peerId, listener)
        return null
    }

    /**
     * Get the avatar
     *
     * @param chat
     * @param peerId
     * @return the avatar
     */
    fun getAvatarBitmap(chat: MegaChatRoom, peerId: Long): Bitmap? {
        var avatar = CallUtil.getImageAvatarCall(chat, peerId)
        if (avatar == null) {
            avatar = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return avatar
    }

    /**
     * Create a participant with my data
     *
     * @param chat MegaChatRoom
     * @return me as a participant
     */
    fun getMeToSpeakerView(chat: MegaChatRoom): Participant {
        var isAudioOn = true
        var isVideoOn = true

        getMeeting(chat.chatId)?.let {
            isAudioOn = it.hasLocalAudio()
            isVideoOn = it.hasLocalVideo()
        }

        val avatar = getAvatarBitmap(chat, megaApi.myUserHandleBinary)
        return Participant(
            megaApi.myUserHandleBinary,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname,
            avatar,
            true,
            getOwnPrivileges(chat.chatId) == MegaChatRoom.PRIV_MODERATOR,
            isAudioOn,
            isVideoOn,
            isContact = false,
            isSpeaker = true,
            hasHiRes = true,
            videoListener = null,
            isChosenForAssign = false,
            isGuest = false
        )
    }

    /**
     * Method to know if a user is my contact
     *
     * @param peerId
     * @return True, if it's. False, otherwise.
     */
    fun isMyContact(peerId: Long): Boolean {
        val email = ChatController(context).getParticipantEmail(peerId)
        val contact = megaApi.getContact(email)

        return contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE
    }

    /**
     * Method to get the participant's name
     *
     * @param peerId
     * @return The name
     */
    fun participantName(peerId: Long): String {
        return ChatController(context).getParticipantFullName(peerId)
    }

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener MeetingVideoListener
     */
    fun addRemoteVideoOneToOneCall(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        logDebug("Add Chat remote video listener of $clientId")
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener MeetingVideoListener
     */
    fun removeRemoteVideoOneToOneCall(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        logDebug("Remove chat video listener of $clientId")
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener GroupVideoListener
     */
    fun addRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: GroupVideoListener
    ) {
        logDebug("Add Chat remote video listener of client $clientId")
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener GroupVideoListener
     */
    fun removeRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: GroupVideoListener
    ) {
        logDebug("Remove Chat remote video listener of client $clientId")
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method for requesting the video in high quality
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Request HiRes video of client $clientId")
        megaChatApi.requestHiResVideo(chatId, clientId, listener)
    }

    /**
     * Method to stop receiving the video in high quality
     *
     * @param chatId chatId
     * @param clientId List with clients ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun stopHiResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Stop HiRes video of client  ${clientId[0]}")
        megaChatApi.stopHiResVideo(chatId, clientId, listener)
    }

    /**
     * Method for requesting the video in low quality
     *
     * @param chatId chatId
     * @param clientId List with clients ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun requestLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Request LowRes video of client ${clientId[0]}")
        megaChatApi.requestLowResVideo(chatId, clientId, listener)
    }

    /**
     * Method to stop receiving the video in low quality
     *
     * @param chatId chatId
     * @param clientId List with clients ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun stopLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface
    ) {
        logDebug("Stop LowRes video of client  ${clientId[0]}")
        megaChatApi.stopLowResVideo(chatId, clientId, listener)
    }

    /**
     * Method of obtaining the local video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun addLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener) {
        logDebug("Add chat local video listener")
        megaChatApi.addChatLocalVideoListener(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun removeLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener) {
        logDebug("Remove chat video listener")
        megaChatApi.removeChatVideoListener(chatId, MEGACHAT_INVALID_HANDLE, false, listener)
    }

    /**
     * Method to get own privileges in a chat
     *
     * @param chatId
     * @return the privileges
     */
    fun getOwnPrivileges(chatId: Long): Int {
        getChatRoom(chatId)?.let {
            return it.ownPrivilege
        }

        return -1
    }

    fun chatLogout(listener: MegaChatRequestListenerInterface) = megaChatApi.logout(listener)

    fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface
    ) {
        // INIT_WAITING_NEW_SESSION    = 1,    /// No \c sid provided at init() --> force a login+fetchnodes
        val initResult = megaChatApi.init(null)

        if (initResult == INIT_WAITING_NEW_SESSION) {
            megaApi.createEphemeralAccountPlusPlus(firstName, lastName, listener)
        } else {
            logWarning("Init chat failed, result: $initResult")
        }
    }

    fun fetchNodes(listener: MegaRequestListenerInterface) = megaApi.fetchNodes(listener)

    fun chatConnect(listener: MegaChatRequestListenerInterface) = megaChatApi.connect(listener)

    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.openChatPreview(link, listener)

    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) =
        megaChatApi.autojoinPublicChat(chatId, listener)

    fun registerConnectionUpdateListener(chatId: Long, callback: () -> Unit) =
        megaChatApi.addChatListener(ChatConnectionListener(chatId, callback))

    fun getMyInfo(moderator: Boolean, audio: Boolean, video: Boolean): Participant {
        return Participant(
            megaApi.myUserHandleBinary,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname ?: "",
            null, true, moderator, audio, video
        )
    }

    fun updateChatPermissions(
        chatId: Long, peerId: Long,
        listener: MegaChatRequestListenerInterface?
    ) {
        megaChatApi.updateChatPermissions(
            chatId,
            peerId,
            MegaChatRoom.PRIV_MODERATOR,
            listener
        )
    }

    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        var bitmap: Bitmap?
        val mail = ChatController(context).getParticipantEmail(peerId)

        val userHandleString = MegaApiAndroid.userHandleToBase64(peerId)
        val myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaApi.myUserHandleBinary)
        bitmap = if (userHandleString == myUserHandleEncoded) {
            AvatarUtil.getAvatarBitmap(mail)
        } else {
            if (TextUtil.isTextEmpty(mail)) AvatarUtil.getAvatarBitmap(userHandleString) else AvatarUtil.getUserAvatar(
                userHandleString,
                mail
            )
        }

        if (bitmap == null){
            bitmap = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return bitmap
    }

    fun getParticipantEmail(peerId: Long): String? =
        ChatController(context).getParticipantEmail(peerId)

    /**
     * Determine if I am a guest
     *
     * @return True, if my account is an ephemeral account. False otherwise
     */
    fun amIAGuest(): Boolean = megaApi.isEphemeralPlusPlus
}