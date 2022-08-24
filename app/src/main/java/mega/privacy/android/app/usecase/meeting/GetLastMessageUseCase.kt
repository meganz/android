package mega.privacy.android.app.usecase.meeting

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.converterShortCodes
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.MeetingUtil.getAppropriateStringForCallCancelled
import mega.privacy.android.app.utils.MeetingUtil.getAppropriateStringForCallEnded
import mega.privacy.android.app.utils.MeetingUtil.getAppropriateStringForCallFailed
import mega.privacy.android.app.utils.MeetingUtil.getAppropriateStringForCallNoAnswered
import mega.privacy.android.app.utils.MeetingUtil.getAppropriateStringForCallRejected
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.StringUtils.isTextEmpty
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApi.CHAT_CONNECTION_ONLINE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCall.CALL_STATUS_JOINING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
import nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT
import nz.mega.sdk.MegaChatCall.END_CALL_REASON_BY_MODERATOR
import nz.mega.sdk.MegaChatCall.END_CALL_REASON_CANCELLED
import nz.mega.sdk.MegaChatCall.END_CALL_REASON_ENDED
import nz.mega.sdk.MegaChatCall.END_CALL_REASON_NO_ANSWER
import nz.mega.sdk.MegaChatCall.END_CALL_REASON_REJECTED
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage.TYPE_CALL_ENDED
import nz.mega.sdk.MegaChatMessage.TYPE_CHAT_TITLE
import nz.mega.sdk.MegaChatMessage.TYPE_CONTACT_ATTACHMENT
import nz.mega.sdk.MegaChatMessage.TYPE_INVALID
import nz.mega.sdk.MegaChatMessage.TYPE_PRIV_CHANGE
import nz.mega.sdk.MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE
import nz.mega.sdk.MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE
import nz.mega.sdk.MegaChatMessage.TYPE_SET_PRIVATE_MODE
import nz.mega.sdk.MegaChatMessage.TYPE_SET_RETENTION_TIME
import nz.mega.sdk.MegaChatMessage.TYPE_TRUNCATE
import nz.mega.sdk.MegaChatMessage.TYPE_VOICE_CLIP
import javax.inject.Inject

class GetLastMessageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) {

    companion object {
        private const val LAST_MSG_LOADING = 255
    }

    private val chatController: ChatController by lazy { ChatController(context) }
    private val chatManagement: ChatManagement by lazy { MegaApplication.getChatManagement() }

    fun get(chatId: Long, msgId: Long): Single<SpannableString> =
        Single.fromCallable {
            val chatMessage = megaChatApi.getMessage(chatId, msgId)
                ?: return@fromCallable getString(R.string.no_conversation_history).toSpannableString()

            val chatCall = megaChatApi.getChatCall(chatId)
            if (chatCall != null) {
                if (megaChatApi.getChatConnectionState(chatId) == CHAT_CONNECTION_ONLINE) {
                    when (chatCall.status) {
                        CALL_STATUS_TERMINATING_USER_PARTICIPATION, CALL_STATUS_USER_NO_PRESENT -> {
                            if (chatCall.isRinging) {
                                return@fromCallable getString(R.string.notification_subtitle_incoming).toSpannableString()
                            } else {
                                return@fromCallable getString(R.string.ongoing_call_messages).toSpannableString()
                            }
                        }
                        CALL_STATUS_JOINING, CALL_STATUS_IN_PROGRESS -> {
                            val requestSent = chatManagement.isRequestSent(chatCall.callId)
                            if (requestSent) {
                                return@fromCallable getString(R.string.outgoing_call_starting).toSpannableString()
                            } else {
                                return@fromCallable getString(R.string.call_started_messages).toSpannableString()
                            }
                        }
                    }
                }
            }

            val chatListItem = megaChatApi.getChatListItem(chatId)
            requireNotNull(chatListItem)
            val chatRoom = megaChatApi.getChatRoom(chatId)
            requireNotNull(chatRoom)
            val lastMessageFormatted = converterShortCodes(chatListItem.lastMessage)

            return@fromCallable when (chatMessage.type) {
                TYPE_INVALID ->
                    getString(R.string.no_conversation_history).toSpannableString()
                LAST_MSG_LOADING ->
                    getString(R.string.general_loading).toSpannableString()
                TYPE_PRIV_CHANGE ->
                    chatController.createManagementString(chatMessage, chatRoom).toSpannableString()
                TYPE_TRUNCATE ->
                    String.format(
                        getString(R.string.history_cleared_by), chatListItem.getSenderName()
                    ).cleanHtmlText()
                TYPE_SET_RETENTION_TIME -> {
                    val timeFormatted = ChatUtil.transformSecondsInString(chatRoom.retentionTime)
                    if (timeFormatted.isTextEmpty()) {
                        String.format(
                            getString(R.string.retention_history_disabled),
                            chatListItem.getSenderName()
                        ).cleanHtmlText()
                    } else {
                        String.format(
                            getString(R.string.retention_history_changed_by),
                            chatListItem.getSenderName(),
                            timeFormatted
                        ).cleanHtmlText()
                    }
                }
                TYPE_PUBLIC_HANDLE_CREATE ->
                    String.format(
                        getString(R.string.message_created_chat_link), chatListItem.getSenderName()
                    ).cleanHtmlText()
                TYPE_PUBLIC_HANDLE_DELETE ->
                    String.format(
                        getString(R.string.message_deleted_chat_link), chatListItem.getSenderName()
                    ).cleanHtmlText()
                TYPE_SET_PRIVATE_MODE ->
                    String.format(
                        getString(R.string.message_set_chat_private), chatListItem.getSenderName()
                    ).cleanHtmlText()
                TYPE_CHAT_TITLE ->
                    String.format(
                        getString(R.string.change_title_messages),
                        chatListItem.getSenderName(),
                        lastMessageFormatted
                    ).cleanHtmlText()
                TYPE_CALL_ENDED -> {
                    if (chatCall == null) {
                        getString(R.string.error_message_unrecognizable).toSpannableString()
                    } else
                    when (chatCall.termCode) {
                        END_CALL_REASON_BY_MODERATOR, END_CALL_REASON_ENDED ->
                            getAppropriateStringForCallEnded(chatRoom, chatMessage.duration.toLong()).toSpannableString()
                        END_CALL_REASON_REJECTED ->
                            getAppropriateStringForCallRejected().toSpannableString()
                        END_CALL_REASON_NO_ANSWER ->
                            getAppropriateStringForCallNoAnswered(chatMessage.userHandle).toSpannableString()
                        END_CALL_REASON_CANCELLED ->
                            getAppropriateStringForCallCancelled(chatMessage.userHandle).toSpannableString()
                        else ->
                            getAppropriateStringForCallFailed().toSpannableString()
                    }
                }
                TYPE_CONTACT_ATTACHMENT -> {
                    val message = converterShortCodes(
                        getString(R.string.contacts_sent, chatMessage.usersCount.toString())
                    ).toSpannableString()

                    when {
                        chatListItem.lastMessageSender == megaChatApi.myUserHandle ->
                            message.getColoredMessage(getString(R.string.word_me))
                        chatRoom.isGroup -> {
                            val senderName = chatListItem.getSenderName()
                            if (chatRoom.unreadCount == 0) {
                                message.getColoredMessage(senderName, android.R.attr.textColorSecondary)
                            } else {
                                message.getColoredMessage(senderName, R.attr.colorSecondary)
                            }
                        }
                        else ->
                            message
                    }
                }
                TYPE_VOICE_CLIP -> {
                    val nodeList = chatMessage.megaNodeList
                    if ((nodeList?.size() ?: 0) > 0 && ChatUtil.isVoiceClip(nodeList.get(0).name)) {
                        val duration = ChatUtil.getVoiceClipDuration(nodeList.get(0))
                        CallUtil.milliSecondsToTimer(duration).toSpannableString()
                    } else {
                        SpannableString("--:--")
                    }
                }

                else -> {
                    when {
                        chatListItem.lastMessage.isNullOrBlank() ->
                            getString(R.string.error_message_unrecognizable).toSpannableString()
                        chatListItem.lastMessageSender == megaChatApi.myUserHandle ->
                            lastMessageFormatted.toSpannableString().getColoredMessage(getString(R.string.word_me))
                        chatRoom.isGroup -> {
                            val senderName = chatListItem.getSenderName()
                            if (chatRoom.unreadCount == 0) {
                                lastMessageFormatted.toSpannableString()
                                    .getColoredMessage(senderName, android.R.attr.textColorSecondary)
                            } else {
                                lastMessageFormatted.toSpannableString()
                                    .getColoredMessage(senderName, R.attr.colorSecondary)
                            }
                        }
                        else ->
                            lastMessageFormatted.toSpannableString()
                    }
                }
            }
        }

    private fun MegaChatListItem.getSenderName(): String {
        val isMine = lastMessageSender == megaChatApi.myUserHandle
        val senderName = if (isMine) {
            megaChatApi.myFullname
                ?: megaChatApi.myEmail
                ?: getString(R.string.unknown_name_label)
        } else {
            megaChatApi.getUserFullnameFromCache(lastMessageSender)
                ?: megaChatApi.getUserEmailFromCache(lastMessageSender)
                ?: getString(R.string.unknown_name_label)
        }
        return senderName
    }

    private fun String.cleanHtmlText(): SpannableString =
        Util.toCDATA(this)
            .replace("[A]", "")
            .replace("[/A]", "")
            .replace("[B]", "")
            .replace("[/B]", "")
            .replace("[C]", "")
            .replace("[/C]", "")
            .toSpannedHtmlText()
            .toSpannableString()

    private fun String.toSpannableString(): SpannableString =
        SpannableString(this)

    private fun SpannableString.getColoredMessage(
        senderName: String,
        textColor: Int = android.R.attr.textColorSecondary,
    ): SpannableString {
        val senderMessage = SpannableString("$senderName ").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)),
                0,
                senderName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        setSpan(
            ForegroundColorSpan(getThemeColor(context, textColor)),
            0,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return "$senderMessage $this".toSpannableString()
    }

    private fun Spanned.toSpannableString(): SpannableString =
        SpannableString(this)
}

