package mega.privacy.android.app.meeting.fragments

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.meeting.listeners.HangChatCallListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.*
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
     * @param audioEnabled if Audio is enabled
     * @param videoEnabled if Video is enabled
     * @param listener MegaChatRequestListenerInterface
     */
    fun startMeeting(
        chatId: Long,
        enableAudio: Boolean,
        enableVideo: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.startChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Get a call from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatCall
     */
    fun getMeeting(chatId: Long): MegaChatCall? {
        return when (chatId) {
            MEGACHAT_INVALID_HANDLE -> {
                null
            }
            else -> {
                megaChatApi.getChatCall(chatId)
            }
        }
    }

    /**
     * Method to know if it's me
     *
     * @param peerId The handle
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean {
        return peerId == megaChatApi.myUserHandle
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
        return when (chatId) {
            MEGACHAT_INVALID_HANDLE -> {
                null
            }
            else -> {
                megaChatApi.getChatRoom(chatId)
            }
        }
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
    fun createMeeting(meetingName: String, listener: MegaChatRequestListenerInterface) {
        megaChatApi.createMeeting(meetingName, listener)
    }

    /**
     * Method to switch a call on hold
     *
     * @param chatId chat ID
     * @param isHold True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(chatId: Long, isOn: Boolean) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            megaChatApi.setCallOnHold(chatId, isOn, SetCallOnHoldListener(context))
        }
    }

    /**
     * Method for leave a meeting
     *
     * @param chatId chat ID
     */
    fun leaveMeeting(callId: Long) {
        if (callId == MEGACHAT_INVALID_HANDLE)
            return

        megaChatApi.hangChatCall(callId, HangChatCallListener(context))
    }

    fun isMyContact(chat: MegaChatRoom, peerId: Long): Boolean {
        val userMail = CallUtil.getUserMailCall(chat, peerId)
        if (!TextUtil.isTextEmpty(userMail)) {
            val contact: MegaUser = megaApi.getContact(userMail)
            if (contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                return true
            }
        }
        return false
    }

    fun joinPublicChat(chatId: Long, listener: ChatBaseListener) =
        megaChatApi.autojoinPublicChat(chatId, listener)

    fun answerChatCall(chatId: Long, enableVideo: Boolean, enableAudio: Boolean,
                       listener: ChatBaseListener) =
        megaChatApi.answerChatCall(chatId, enableVideo, enableAudio, listener)
}