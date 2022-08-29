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
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.converterShortCodes
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.StringUtils.isTextEmpty
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApi.CHAT_CONNECTION_ONLINE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCall.CALL_STATUS_JOINING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
import nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage.TYPE_ALTER_PARTICIPANTS
import nz.mega.sdk.MegaChatMessage.TYPE_CALL_ENDED
import nz.mega.sdk.MegaChatMessage.TYPE_CALL_STARTED
import nz.mega.sdk.MegaChatMessage.TYPE_CHAT_TITLE
import nz.mega.sdk.MegaChatMessage.TYPE_CONTACT_ATTACHMENT
import nz.mega.sdk.MegaChatMessage.TYPE_CONTAINS_META
import nz.mega.sdk.MegaChatMessage.TYPE_INVALID
import nz.mega.sdk.MegaChatMessage.TYPE_NORMAL
import nz.mega.sdk.MegaChatMessage.TYPE_PRIV_CHANGE
import nz.mega.sdk.MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE
import nz.mega.sdk.MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE
import nz.mega.sdk.MegaChatMessage.TYPE_SET_PRIVATE_MODE
import nz.mega.sdk.MegaChatMessage.TYPE_SET_RETENTION_TIME
import nz.mega.sdk.MegaChatMessage.TYPE_TRUNCATE
import nz.mega.sdk.MegaChatMessage.TYPE_VOICE_CLIP
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Get last message use case to retrieve formatted last message from specific chat
 *
 * @property context        Needed to get strings
 * @property megaChatApi    Needed to retrieve chat messages
 */
class GetLastMessageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaChatApi: MegaChatApiAndroid,
) {

    companion object {
        private const val LAST_MSG_LOADING = 255
    }

    private val chatController: ChatController by lazy { ChatController(context) }
    private val chatManagement: ChatManagement by lazy { MegaApplication.getChatManagement() }

    /**
     * Get a formatted String with the last message
     *
     * @param chatId    Chat Id to retrieve chat message
     * @param msgId     Message Id to retrieve chat message
     * @return          Single
     */
    fun get(chatId: Long, msgId: Long): Single<SpannableString> =
        Single.fromCallable {
            megaChatApi.getChatCall(chatId)?.let { chatCall ->
                if (megaChatApi.getChatConnectionState(chatId) == CHAT_CONNECTION_ONLINE) {
                    when (chatCall.status) {
                        CALL_STATUS_TERMINATING_USER_PARTICIPATION, CALL_STATUS_USER_NO_PRESENT -> {
                            return@fromCallable if (chatCall.isRinging) {
                                getString(R.string.notification_subtitle_incoming).toSpannableString()
                            } else {
                                getString(R.string.ongoing_call_messages).toSpannableString()
                            }
                        }
                        CALL_STATUS_JOINING, CALL_STATUS_IN_PROGRESS -> {
                            val requestSent = chatManagement.isRequestSent(chatCall.callId)
                            return@fromCallable if (requestSent) {
                                getString(R.string.outgoing_call_starting).toSpannableString()
                            } else {
                                getString(R.string.call_started_messages).toSpannableString()
                            }
                        }
                    }
                }
            }

            val chatListItem = requireNotNull(megaChatApi.getChatListItem(chatId))
            val chatRoom by lazy { megaChatApi.getChatRoom(chatId) }
            val chatMessage by lazy { megaChatApi.getMessage(chatId, msgId) }

            return@fromCallable when (chatListItem.lastMessageType) {
                TYPE_INVALID ->
                    getString(R.string.no_conversation_history).toSpannableString()
                LAST_MSG_LOADING ->
                    getString(R.string.general_loading).toSpannableString()
                TYPE_NORMAL, TYPE_CHAT_TITLE, TYPE_CALL_STARTED, TYPE_CALL_ENDED, TYPE_TRUNCATE, TYPE_ALTER_PARTICIPANTS, TYPE_PRIV_CHANGE, TYPE_CONTAINS_META ->
                    chatController.createManagementString(chatMessage, chatRoom)
                        .colorUnreadIfNeeded(chatRoom)
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
                TYPE_CONTACT_ATTACHMENT -> {
                    val message = converterShortCodes(getString(R.string.contacts_sent, chatMessage.usersCount.toString()))
                    if (chatListItem.lastMessageSender == megaChatApi.myUserHandle) {
                        "${getString(R.string.word_me)}: $message".toSpannableString()
                    } else {
                        message.colorUnreadIfNeeded(chatRoom)
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
                            "${getString(R.string.word_me)}: ${converterShortCodes(chatListItem.lastMessage)}".toSpannableString()
                        else ->
                            converterShortCodes(chatListItem.lastMessage).colorUnreadIfNeeded(chatRoom)
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

    private fun String.colorUnreadIfNeeded(chatRoom: MegaChatRoom): SpannableString =
        toSpannableString().apply {
            if (chatRoom.isGroup && chatRoom.unreadCount > 0) {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.teal_300_teal_200)),
                    0,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

    private fun String.toSpannableString(): SpannableString =
        SpannableString(this)

    private fun Spanned.toSpannableString(): SpannableString =
        SpannableString(this)
}

