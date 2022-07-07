package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.AddContactListener
import mega.privacy.android.app.meeting.listeners.MeetingAvatarListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApi.INIT_WAITING_NEW_SESSION
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getChatChangesUseCase: GetChatChangesUseCase,
    @ApplicationContext private val context: Context,
) {

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
        listener: MegaChatRequestListenerInterface,
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
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Starting call with video enable $enableVideo and audio enable $enableAudio")
        megaChatApi.startChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Get a call from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatCall
     */
    fun getMeeting(chatId: Long): MegaChatCall? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else megaChatApi.getChatCall(chatId)


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
    fun getChatRoom(chatId: Long): MegaChatRoom? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else megaChatApi.getChatRoom(chatId)

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
     * @param peerId user handle of participant
     * @param listener MegaRequestListenerInterface
     * @return the email of the participant
     */
    fun getEmailParticipant(peerId: Long, listener: MegaRequestListenerInterface): String? {
        if (isMe(peerId))
            return megaChatApi.myEmail

        val email = megaChatApi.getUserEmailFromCache(peerId)

        if (email != null)
            return email

        megaApi.getUserEmail(peerId, listener)
        return null
    }

    /**
     * Get the avatar
     *
     * @param chat The chat room of a meeting
     * @param peerId user Handle of a participant
     * @return the avatar the avatar of a participant
     */
    fun getAvatarBitmap(chat: MegaChatRoom, peerId: Long): Bitmap? {
        var avatar = CallUtil.getImageAvatarCall(chat, peerId)
        if (avatar == null) {
            getRemoteAvatar(peerId)

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
     * @param chat The chat room of a meeting
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
     * @param peerId user handle of a participant
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
     * @param peerId user handle of a participant
     * @return The name of a participant
     */
    fun participantName(peerId: Long): String? =
        if (peerId == MEGACHAT_INVALID_HANDLE) null
        else ChatController(context).getParticipantFullName(peerId)

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chat ID
     * @param clientId client ID of a participant
     * @param hiRes If it's has High resolution
     * @param listener MegaChatVideoListenerInterface
     */
    fun addChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (hiRes) {
            Timber.d("Add Chat remote video listener of client $clientId , with HiRes")
        } else {
            Timber.d("Add Chat remote video listener of client $clientId , with LowRes")
        }
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chat ID
     * @param clientId client ID of a participant
     * @param hiRes If it's has High resolution
     * @param listener MegaChatVideoListenerInterface
     */
    fun removeChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (hiRes) {
            Timber.d("Remove Chat remote video listener of client $clientId, with HiRes")
        } else {
            Timber.d("Remove Chat remote video listener of client $clientId, with LowRes")
        }
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method for requesting the video in high quality
     *
     * @param chatId chat ID
     * @param clientId client ID of a participant
     * @param listener MegaChatRequestListenerInterface
     */
    fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Request HiRes video of client $clientId")
        megaChatApi.requestHiResVideo(chatId, clientId, listener)
    }

    /**
     * Method to stop receiving the video in high quality
     *
     * @param chatId chat ID
     * @param clientId List with clients ID of participants
     * @param listener MegaChatRequestListenerInterface
     */
    fun stopHiResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Stop HiRes video of client  ${clientId[0]}")
        megaChatApi.stopHiResVideo(chatId, clientId, listener)
    }

    /**
     * Method for requesting the video in low quality
     *
     * @param chatId chat ID
     * @param clientId List with clients ID of participants
     * @param listener MegaChatRequestListenerInterface
     */
    fun requestLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Request LowRes video of client ${clientId[0]}")
        megaChatApi.requestLowResVideo(chatId, clientId, listener)
    }

    /**
     * Method to stop receiving the video in low quality
     *
     * @param chatId chat ID
     * @param clientId List with clients ID of participants
     * @param listener MegaChatRequestListenerInterface
     */
    fun stopLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Stop LowRes video of client  ${clientId[0]}")
        megaChatApi.stopLowResVideo(chatId, clientId, listener)
    }

    /**
     * Method to get own privileges in a chat
     *
     * @param chatId chat ID
     * @return my privileges
     */
    fun getOwnPrivileges(chatId: Long): Int {
        getChatRoom(chatId)?.let {
            return it.ownPrivilege
        }

        return -1
    }

    fun chatLogout(listener: MegaChatRequestListenerInterface) = megaChatApi.logout(listener)

    /**
     * Method to create an ephemera plus plus account
     *
     * @param firstName First name of the guest
     * @param lastName Last name of the guest
     * @param listener MegaRequestListenerInterface
     */
    fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) {
        val ret = megaChatApi.initState
        if (ret == MegaChatApi.INIT_NOT_DONE || ret == MegaChatApi.INIT_ERROR) {
            Timber.d("INIT STATE: $ret")
            val initResult = megaChatApi.init(null)
            Timber.d("result of init ---> $initResult")
            if (initResult == INIT_WAITING_NEW_SESSION) {
                megaApi.createEphemeralAccountPlusPlus(firstName, lastName, listener)
            } else {
                Timber.w("Init chat failed, result: $initResult")
            }
        }
    }

    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.openChatPreview(link, listener)

    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) {
        if (!MegaApplication.getChatManagement().isAlreadyJoining(chatId)) {
            Timber.d("Joining to public chat with ID $chatId")
            MegaApplication.getChatManagement().addJoiningChatId(chatId)
            megaChatApi.autojoinPublicChat(chatId, listener)
        }
    }

    fun rejoinPublicChat(
        chatId: Long,
        publicChatHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Rejoining to public chat with ID $chatId")
        megaChatApi.autorejoinPublicChat(chatId, publicChatHandle, listener)
    }

    fun registerConnectionUpdateListener(chatId: Long, callback: () -> Unit) {
        var chatSubscription: Disposable? = null
        chatSubscription = getChatChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { change ->
                    when (change) {
                        is GetChatChangesUseCase.Result.OnChatConnectionStateUpdate -> {
                            if (change.chatid == chatId && change.newState == MegaChatApi.CHAT_CONNECTION_ONLINE) {
                                Timber.d("Connect to chat ${change.chatid} successfully!")
                                callback()
                                chatSubscription?.dispose()
                            }
                        }
                        else -> {
                            // Nothing to do
                        }
                    }
                }
            )
    }

    fun getMyFullName(): String {
        val name = megaChatApi.myFullname
        if (name != null)
            return name

        return megaChatApi.myEmail
    }

    fun getMyInfo(moderator: Boolean, audio: Boolean, video: Boolean): Participant {
        return Participant(
            megaApi.myUserHandleBinary,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname ?: "",
            getAvatarBitmapByPeerId(megaApi.myUserHandleBinary),
            true,
            moderator,
            audio,
            video,
            isGuest = megaApi.isEphemeralPlusPlus
        )
    }

    /**
     * Method for getting a participant's avatar
     *
     * @param peerId user handle of participant
     */
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

        if (bitmap == null) {
            megaApi.getUserAvatar(
                mail,
                CacheFolderManager.buildAvatarFile(
                    context,
                    mail + FileUtil.JPG_EXTENSION
                )?.absolutePath, MeetingAvatarListener(context, peerId)
            )
            bitmap = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return bitmap
    }

    /**
     * Method for getting a participant's email
     *
     * @param peerId user handle of participant
     */
    fun getParticipantEmail(peerId: Long): String? =
        ChatController(context).getParticipantEmail(peerId)

    /**
     * Determine if I am a guest
     *
     * @return True, if my account is an ephemeral account. False otherwise
     */
    fun amIAGuest(): Boolean = megaApi.isEphemeralPlusPlus

    /**
     * Send add contact invitation
     *
     * @param context the Context
     * @param peerId the peerId of users
     * @param callback the callback for sending add contact request
     */
    fun addContact(context: Context, peerId: Long, callback: (String) -> Unit) {
        megaApi.inviteContact(
            ChatController(context).getParticipantEmail(peerId),
            null,
            MegaContactRequest.INVITE_ACTION_ADD,
            AddContactListener(callback)
        )
    }

    /**
     * Get avatar from sdk
     *
     * @param peerId the peerId of participant
     */
    fun getRemoteAvatar(peerId: Long) {
        var email = ChatController(context).getParticipantEmail(peerId)

        if (email == null) {
            email = MegaApiJava.handleToBase64(peerId)
        }

        if (email == null) return

        megaApi.getUserAvatar(
            email,
            CacheFolderManager.buildAvatarFile(
                context,
                email + FileUtil.JPG_EXTENSION
            )?.absolutePath, MeetingAvatarListener(context, peerId)
        )
    }
}