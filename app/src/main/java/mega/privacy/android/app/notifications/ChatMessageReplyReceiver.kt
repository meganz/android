package mega.privacy.android.app.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.net.toUri
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.data.extensions.enqueueChatReply
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives the text typed in the inline reply of a chat message notification.
 *
 * The reply is shown in the notification straight away and the actual sending is handed to
 * [SendChatReplyWorker], which can outlive this receiver and log the session in again when the
 * app process was killed in the meantime.
 */
@AndroidEntryPoint
class ChatMessageReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatMessageNotificationManager: ChatMessageNotificationManager

    @Inject
    lateinit var workManager: WorkManager

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, MegaApiJava.INVALID_HANDLE)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()

        if (chatId == MegaApiJava.INVALID_HANDLE || reply.isNullOrEmpty()) {
            Timber.w("Ignoring notification reply for chat $chatId")
            return
        }

        Timber.d("Reply received from notification $notificationId for chat $chatId")
        chatMessageNotificationManager.appendReply(context, notificationId, reply)
        workManager.enqueueChatReply(chatId, reply)
    }

    companion object {
        /**
         * Key of the [RemoteInput] result holding the reply text
         */
        const val KEY_TEXT_REPLY = "KEY_TEXT_REPLY"
        private const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
        private const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val REPLY_URI_SCHEME = "mega-chat-reply"

        /**
         * Builds the [PendingIntent] the reply action of a message notification fires.
         *
         * The system matches pending intents by request code and [Intent.filterEquals], which
         * ignores extras. Both the request code and the data URI therefore identify the target,
         * so a reply can never be delivered to a different chat than the one it was typed in.
         *
         * @param context           [Context]
         * @param chatId            Chat the reply is sent to
         * @param notificationId    Id of the message notification carrying the action
         */
        fun getPendingIntent(context: Context, chatId: Long, notificationId: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                Intent(context, ChatMessageReplyReceiver::class.java)
                    .setData("$REPLY_URI_SCHEME://$chatId/$notificationId".toUri())
                    .putExtra(EXTRA_CHAT_ID, chatId)
                    .putExtra(EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
    }
}
