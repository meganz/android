package mega.privacy.android.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.formatter.stripChatFormatTags
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.notifications.ChatMessageNotificationData
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Manager class to show a chat message Notification
 *
 * @property notificationManagerCompat    [NotificationManagerCompat]
 *
 */
@Singleton
class ChatMessageNotificationManager @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val chatManagement: ChatManagement,
) {
    companion object {
        private const val ME_PERSON_KEY = "me"

        // Distinct from the call notification, which uses the bare base64 chat handle hash
        private const val CONVERSATION_ID_PREFIX = "ChatMessages"

        /**
         * Notification extra holding the chat message id of every message in the conversation,
         * in the same order as the [NotificationCompat.MessagingStyle] messages.
         * Replies typed in the notification are stored as [MegaApiJava.INVALID_HANDLE].
         */
        internal const val EXTRA_MESSAGE_IDS = "mega.privacy.android.app.notifications.MESSAGE_IDS"

        /**
         * Id of the conversation notification of a chat
         *
         * @param chatId Chat id
         */
        internal fun getNotificationId(chatId: Long) =
            (CONVERSATION_ID_PREFIX + MegaApiJava.userHandleToBase64(chatId)).hashCode()
    }

    // Every update reads the shown conversation, changes it and posts it again, and messages of
    // one chat can arrive concurrently
    private val lock = Any()

    /**
     * Show Notification given a [ChatMessageNotificationData]
     *
     * Every chat has a single conversation notification. A new message is appended to the
     * conversation already shown, so the replies typed in the notification and the answers to
     * them stay in one thread.
     *
     * @param context                   [Context]
     * @param chatMessageNotificationData   [ChatMessageNotificationData]
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun show(
        context: Context,
        chatMessageNotificationData: ChatMessageNotificationData,
        fileDurationMapper: FileDurationMapper,
    ) = with(chatMessageNotificationData) {
        val notificationId = getNotificationId(chat.chatId)

        when (this) {
            is ChatMessageNotificationData.DeletedMessage -> {
                Timber.d("Message deleted. Removing it from the conversation notification")
                removeMessage(context, notificationId, msg.messageId, chat.unreadCount)
                return@with
            }

            is ChatMessageNotificationData.SeenMessage -> {
                Timber.d("Message seen. Removing it from the conversation notification")
                removeMessage(context, notificationId, msg.messageId, chat.unreadCount)
                return@with
            }

            is ChatMessageNotificationData.Message -> {
                if (chat.chatId == chatManagement.openChatId) {
                    Timber.d("Chat opened. Removing notification")
                    notificationManagerCompat.cancel(notificationId)
                    return@with
                }
            }
        }

        val pendingIntent = MegaActivity.getPendingIntentWithExtraDestinations(
            context,
            listOf(ShowChatMessagesNavKey(chatId = chat.chatId, openFromList = true))
        )

        val notificationColor = ContextCompat.getColor(context, R.color.red_600_red_300)
        val title = EmojiUtilsShortcodes.emojify(chat.title)

        val msgContent = EmojiUtilsShortcodes.emojify(
            getMsgContent(
                context,
                msg,
                fileDurationMapper
            )
        )

        val largeIcon =
            getAvatar(context, senderAvatar, chat, senderAvatarColor)?.let {
                Util.getCircleBitmap(it)
            }

        val isNormalTypeMsg = msg.type == ChatMessageType.NORMAL

        // Without its own icon the system falls back to the large icon, i.e. the sender's avatar,
        // for the replies typed in the notification.
        val meLabel = context.getString(R.string.chat_last_message_sender_me)
        val myAvatarIcon = getMyAvatarIcon(myAvatar, myAvatarColor, meLabel)

        // In a one to one chat the system draws the large icon for the other party, so only group
        // chats need the sender on every message
        val senderIcon = largeIcon
            ?.takeIf { chat.isGroup }
            ?.let { IconCompat.createWithBitmap(it) }
        val message = NotificationCompat.MessagingStyle.Message(
            if (isNormalTypeMsg) msgContent.stripChatFormatTags() else msgContent,
            msg.timestamp * 1000,
            Person.Builder()
                .setName(senderName)
                .setKey(msg.userHandle.toString())
                .setIcon(senderIcon)
                .build()
        )

        synchronized(lock) {
            val shownNotification = getShownNotification(notificationId)
            val shown = shownNotification
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
                ?: NotificationCompat.MessagingStyle(
                    Person.Builder()
                        .setName(meLabel)
                        .setKey(ME_PERSON_KEY)
                        .setIcon(myAvatarIcon)
                        .build()
                ).also {
                    it.conversationTitle = title
                    it.isGroupConversation = chat.isGroup
                }

            // The same message is notified by both the push worker and the chat listener, and
            // again when it is edited, so a message already in the conversation is replaced,
            // not appended.
            val shownIds = shownNotification.messageIds()
            val shownIndex = shownIds.indexOf(msg.messageId)
            val isReplacing = shownIndex in shown.messages.indices
            val (conversation, messageIds) = if (isReplacing) {
                Timber.d("Message ${msg.messageId} already in conversation $notificationId, replacing it")
                shown.copyWith(shown.messages.toMutableList().also { it[shownIndex] = message }) to
                        shownIds
            } else {
                shown.also { it.addMessage(message) } to shownIds + msg.messageId
            }

            val notification =
                NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_CHAT_ID)
                    .setSmallIcon(iconPackR.drawable.ic_stat_notify)
                    .setAutoCancel(true)
                    .setColor(notificationColor)
                    .setStyle(conversation)
                    .setContentIntent(pendingIntent)
                    .setWhen(msg.timestamp * 1000)
                    .setOnlyAlertOnce(isReplacing)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        notificationBehaviour.sound?.let { setSound(it.toUri()) }
                            ?: setSilent(true)
                        largeIcon?.let { setLargeIcon(it) }
                    }
                    .addAction(buildReplyAction(context, chat.chatId, notificationId))
                    .addExtras(messageIdsExtras(messageIds.matching(conversation)))
                    .build()

            Timber.d("Showing conversation notification $notificationId for chat ${chat.chatId}")
            notificationManagerCompat.notify(notificationId, notification)
        }
    }

    /**
     * Appends a reply typed in the notification to the conversation it shows.
     * Posting the updated notification is what clears the system's in-progress reply indicator.
     *
     * @param context           [Context]
     * @param notificationId    Id of the message notification the reply was typed in
     * @param reply             Reply text
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun appendReply(context: Context, notificationId: Int, reply: CharSequence) =
        updateMessagingNotification(context, notificationId) { shown, style ->
            style.addMessage(reply, System.currentTimeMillis(), style.user)
            val messageIds = shown.messageIds() + MegaApiJava.INVALID_HANDLE
            addExtras(messageIdsExtras(messageIds.matching(style)))
        }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun removeMessage(
        context: Context,
        notificationId: Int,
        messageId: Long,
        unreadCount: Int,
    ): Unit = synchronized(lock) {
        if (unreadCount == 0) {
            Timber.d("No unread messages in chat. Removing notification $notificationId")
            notificationManagerCompat.cancel(notificationId)
            return
        }
        val shown = getShownNotification(notificationId) ?: return
        val conversation = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(shown)
            ?: return
        val messageIds = shown.messageIds()
        val index = messageIds.indexOf(messageId)
        if (index < 0 || index >= conversation.messages.size) return

        val remainingIds = messageIds.filterIndexed { i, _ -> i != index }.toLongArray()
        if (remainingIds.all { it == MegaApiJava.INVALID_HANDLE }) {
            Timber.d("Only own replies left in chat. Removing notification $notificationId")
            notificationManagerCompat.cancel(notificationId)
            return
        }

        val remaining =
            conversation.copyWith(conversation.messages.filterIndexed { i, _ -> i != index })
        val updated = NotificationCompat.Builder(context, shown)
            .setOnlyAlertOnce(true)
            .setStyle(remaining)
            .addExtras(messageIdsExtras(remainingIds))
            .build()
        notificationManagerCompat.notify(notificationId, updated)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateMessagingNotification(
        context: Context,
        notificationId: Int,
        update: NotificationCompat.Builder.(Notification, NotificationCompat.MessagingStyle) -> Unit,
    ): Unit = synchronized(lock) {
        val notification = getShownNotification(notificationId) ?: run {
            Timber.w("Notification $notificationId is no longer shown, nothing to update")
            return
        }
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
            ?: return

        val updated = NotificationCompat.Builder(context, notification)
            .setOnlyAlertOnce(true)
            .apply { update(notification, style) }
            .setStyle(style)
            .build()
        notificationManagerCompat.notify(notificationId, updated)
    }

    private fun getShownNotification(notificationId: Int): Notification? = runCatching {
        notificationManagerCompat.activeNotifications
            .firstOrNull { it.id == notificationId }
            ?.notification
    }.onFailure { Timber.w(it, "Cannot read the active notifications") }.getOrNull()

    private fun NotificationCompat.MessagingStyle.copyWith(
        messages: List<NotificationCompat.MessagingStyle.Message>,
    ) = NotificationCompat.MessagingStyle(user).also { style ->
        style.conversationTitle = conversationTitle
        style.isGroupConversation = isGroupConversation
        messages.forEach { style.addMessage(it) }
    }

    private fun Notification?.messageIds(): LongArray =
        this?.extras?.getLongArray(EXTRA_MESSAGE_IDS) ?: LongArray(0)

    private fun messageIdsExtras(messageIds: LongArray) =
        Bundle().apply { putLongArray(EXTRA_MESSAGE_IDS, messageIds) }

    /**
     * The style keeps only the most recent messages, so the ids have to be trimmed the same way
     */
    private fun LongArray.matching(style: NotificationCompat.MessagingStyle): LongArray =
        takeLast(style.messages.size).toLongArray()

    private fun buildReplyAction(
        context: Context,
        chatId: Long,
        notificationId: Int,
    ): NotificationCompat.Action {
        val label = context.getString(sharedR.string.chat_notification_reply_action)
        val remoteInput = RemoteInput.Builder(ChatMessageReplyReceiver.KEY_TEXT_REPLY)
            .setLabel(label)
            .build()

        return NotificationCompat.Action.Builder(
            iconPackR.drawable.ic_stat_notify,
            label,
            ChatMessageReplyReceiver.getPendingIntent(context, chatId, notificationId)
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAllowGeneratedReplies(true)
            .setShowsUserInterface(false)
            .build()
    }

    private fun getMsgContent(
        context: Context,
        msg: ChatMessage,
        fileDurationMapper: FileDurationMapper,
    ) = when (msg.type) {
        ChatMessageType.NODE_ATTACHMENT, ChatMessageType.VOICE_CLIP -> {
            with(msg.nodeList) {
                val node = if (isEmpty()) null else this[0]
                val duration = (node as? FileNode)?.type?.let { fileDurationMapper(it) }
                    ?: 0.seconds

                if (node == null) msg.content
                else if (!typeForName(node.name).isAudioVoiceClip) node.name
                else "\uD83C\uDF99 " + CallUtil.milliSecondsToTimer(
                    if (duration.inWholeSeconds == 0L) 0 else duration.inWholeMilliseconds
                )
            }
        }

        ChatMessageType.CONTACT_ATTACHMENT -> {
            Timber.d("TYPE_CONTACT_ATTACHMENT")
            val userCount = msg.usersCount
            if (userCount == 1L) {
                msg.userNames[0]
            } else {
                val name = StringBuilder("")
                name.append(msg.userNames[0])
                for (j in 1 until userCount.toInt()) {
                    name.append(", " + msg.userNames[j])
                }
                name.toString()
            }
        }

        ChatMessageType.CONTAINS_META -> {
            Timber.d("TYPE_CONTAINS_META")
            if (msg.containsMeta?.type == ContainsMetaType.GEOLOCATION) {
                "\uD83D\uDCCD " + context.getString(R.string.title_geolocation_message)
            } else {
                msg.content
            }
        }

        else -> {
            Timber.d("OTHER")
            msg.content
        }
    }

    private fun getMyAvatarIcon(myAvatar: File?, myAvatarColor: Int, name: String) =
        if (myAvatar?.exists() == true && myAvatar.length() > 0) {
            BitmapFactory.decodeFile(myAvatar.absolutePath, BitmapFactory.Options())
        } else {
            AvatarUtil.getDefaultAvatar(myAvatarColor, name, Constants.AVATAR_SIZE, true, true)
        }?.let { IconCompat.createWithBitmap(Util.getCircleBitmap(it)) }

    private fun getAvatar(
        context: Context,
        senderAvatar: File?,
        chat: ChatRoom,
        senderAvatarColor: Int,
    ) = if (senderAvatar?.exists() == true && senderAvatar.length() > 0) {
        BitmapFactory.decodeFile(senderAvatar.absolutePath, BitmapFactory.Options())
    } else {
        val color = if (chat.isGroup) {
            ContextCompat.getColor(context, R.color.grey_012_white_012)
        } else {
            senderAvatarColor
        }

        AvatarUtil.getDefaultAvatar(
            color,
            chat.title,
            Constants.AVATAR_SIZE,
            true,
            true
        )
    }
}