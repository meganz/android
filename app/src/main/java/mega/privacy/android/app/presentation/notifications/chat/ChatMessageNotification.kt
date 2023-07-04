package mega.privacy.android.app.presentation.notifications.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.settings.ChatSettings
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File

/**
 * Object for showing chat message notifications.
 */
internal object ChatMessageNotification {

    private const val XIAOMI_MANUFACTURER = "xiaomi"
    private const val GROUP_KEY = "Karere"

    fun show(
        context: Context,
        chat: ChatRoom,
        msg: ChatMessage,
        senderName: String,
        senderAvatar: File?,
        senderAvatarColor: String,
        notificationBehaviour: NotificationBehaviour,
        fileDurationMapper: FileDurationMapper,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, ManagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
            putExtra(Constants.CHAT_ID, chat.chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            msg.msgId.toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationColor = ContextCompat.getColor(context, R.color.red_600_red_300)
        val title = EmojiUtilsShortcodes.emojify(chat.title)
        val msgContent =
            if (msg.type == ChatMessageType.CALL_ENDED) {
                context.getString(R.string.missed_call_notification_title)
            } else {
                EmojiUtilsShortcodes.emojify(
                    getmsgContent(
                        context,
                        msg,
                        senderName,
                        fileDurationMapper
                    )
                )
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            || !XIAOMI_MANUFACTURER.equals(Build.MANUFACTURER, ignoreCase = true)
        ) {
            val largeIcon = getAvatar(context, senderAvatar, chat, senderAvatarColor)
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_CHAT_ID,
                    Constants.NOTIFICATION_CHANNEL_CHAT_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(true)
                    enableVibration(false)
                }

                notificationManager.createNotificationChannel(channel)

                @Suppress("DEPRECATION")
                val messagingStyleContent = NotificationCompat.MessagingStyle(title).also {
                    it.addMessage(msgContent, msg.timestamp, senderName)
                    it.conversationTitle = title
                }

                NotificationCompat.Builder(
                    context,
                    Constants.NOTIFICATION_CHANNEL_CHAT_ID
                ).apply {
                    setSmallIcon(R.drawable.ic_stat_notify)
                    setAutoCancel(true)
                    setShowWhen(true)
                    setGroup(GROUP_KEY)
                    color = notificationColor
                    setStyle(messagingStyleContent)
                    setContentIntent(pendingIntent)
                    setWhen(msg.timestamp * 1000)
                    notificationBehaviour.sound?.let { setSound(it.toUri()) } ?: setSilent(true)
                    setChannelId(
                        if (ChatSettings.VIBRATION_ON == notificationBehaviour.vibration) Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
                        else Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID
                    )
                    priority = NotificationManager.IMPORTANCE_HIGH
                    largeIcon?.let { setLargeIcon(it) }
                }.build()
            } else {
                @Suppress("DEPRECATION")
                val contentStyle = Notification.MessagingStyle(title).also {
                    it.addMessage(msgContent, msg.timestamp, senderName)
                    it.conversationTitle = title
                }

                @Suppress("DEPRECATION")
                Notification.Builder(context).apply {
                    setSmallIcon(R.drawable.ic_stat_notify)
                    setAutoCancel(true)
                    setShowWhen(true)
                    setGroup(GROUP_KEY)
                    setColor(notificationColor)
                    style = contentStyle
                    setContentIntent(pendingIntent)
                    setWhen(msg.timestamp * 1000)
                    notificationBehaviour.sound?.let { setSound(it.toUri()) }
                    if (notificationBehaviour.vibration == ChatSettings.VIBRATION_ON) {
                        setVibrate(longArrayOf(0, 500))
                    }
                    setPriority(Notification.PRIORITY_HIGH)
                    largeIcon?.let { setLargeIcon(it) }
                }.build()
            }

            notificationManager.notify(
                MegaApiJava.userHandleToBase64(msg.msgId).hashCode(),
                notification
            )
        } else {
            val style = NotificationCompat.InboxStyle()

            @Suppress("DEPRECATION")
            val notification = NotificationCompat.Builder(context)
                .apply {
                    setSmallIcon(R.drawable.ic_stat_notify)
                    setContentIntent(pendingIntent)
                    setAutoCancel(true)
                    color = notificationColor
                    setShowWhen(true)
                    notificationBehaviour.sound?.let {
                        setSound(it.toUri())
                    } ?: setSilent(true)
                    if (notificationBehaviour.vibration == ChatSettings.VIBRATION_ON) {
                        setVibrate(longArrayOf(0, 500))
                    }
                    setStyle(style)
                    priority = NotificationManager.IMPORTANCE_HIGH
                    style.addLine(
                        if (chat.isGroup) {
                            "$senderName @ $title: $msgContent"
                        } else {
                            "$title: $msgContent"
                        }
                    )
                    setContentTitle(context.getString(R.string.app_name))
                }.build()

            notificationManager.notify(Constants.NOTIFICATION_SUMMARY_CHAT, notification)
        }
    }

    private fun getmsgContent(
        context: Context,
        msg: ChatMessage,
        senderName: String,
        fileDurationMapper: FileDurationMapper,
    ) = when (msg.type) {
        ChatMessageType.NODE_ATTACHMENT, ChatMessageType.VOICE_CLIP -> {
            with(msg.nodeList) {
                val node = if (isEmpty()) null else this[0]
                val duration = ((node as? FileNode)?.type?.let { fileDurationMapper(it) } ?: 0)
                    .toLong()

                if (node == null) msg.content
                else if (!typeForName(node.name).isAudioVoiceClip) node.name
                else "\uD83C\uDF99 " + CallUtil.milliSecondsToTimer(
                    if (duration == 0L) 0 else duration * 1000
                )
            }
        }

        ChatMessageType.CONTACT_ATTACHMENT -> {
            Timber.d("TYPE_CONTACT_ATTACHMENT")
            val userCount = msg.usersCount
            if (userCount == 1L) {
                senderName
            } else {
                val name = StringBuilder("")
                name.append(msg.userNames[0])
                for (j in 1 until userCount.toInt()) {
                    name.append(", " + msg.userNames[j])
                }
                name.toString()
            }
        }

        ChatMessageType.TRUNCATE -> {
            Timber.d("TYPE_TRUNCATE")
            context.getString(R.string.history_cleared_message)
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

    private fun getAvatar(
        context: Context,
        senderAvatar: File?,
        chat: ChatRoom,
        senderAvatarColor: String,
    ) = if (senderAvatar?.exists() == true && senderAvatar.length() > 0) {
        BitmapFactory.decodeFile(senderAvatar.absolutePath, BitmapFactory.Options())
    } else {
        val color = if (chat.isGroup) {
            ContextCompat.getColor(context, R.color.grey_012_white_012)
        } else {
            Color.parseColor(senderAvatarColor)
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