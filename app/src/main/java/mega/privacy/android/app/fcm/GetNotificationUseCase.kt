package mega.privacy.android.app.fcm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.ScheduledMeetingPushMessage
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to generate a {@link Notification} given a {@link PushMessage}
 *
 * @property context
 * @property getChatRoom
 */
class GetNotificationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getChatRoom: GetChatRoom,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke
     *
     * @param pushMessage   PushMessage
     * @return              Pair with the Notification Id and the Notification object
     */
    suspend operator fun invoke(pushMessage: PushMessage): Pair<Int, Notification> =
        withContext(dispatcher) {
            when (pushMessage) {
                is ScheduledMeetingPushMessage -> {
                    val chatId = pushMessage.chatRoomHandle
                    val channelId = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
                    val showMeetingIntent = getShowChatIntent(chatId)
                    val title = pushMessage.getChatRoomTitle()
                    val description = if (pushMessage.isStartReminder) {
                        context.getString(R.string.notification_sched_meeting_starts_now)
                    } else {
                        context.getString(R.string.notification_sched_meeting_starts_15_minutes)
                    }
                    val priority = if (pushMessage.isStartReminder) {
                        NotificationCompat.PRIORITY_MAX
                    } else {
                        NotificationCompat.PRIORITY_HIGH
                    }

                    chatId.toInt() to NotificationCompat.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(description)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setPriority(priority)
                        .setContentIntent(showMeetingIntent)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                        .apply {
                            if (pushMessage.isStartReminder) {
                                addAction(
                                    NotificationCompat.Action.Builder(
                                        null,
                                        context.getString(R.string.action_join),
                                        getJoinMeetingIntent(chatId)
                                    ).build()
                                )
                                addAction(
                                    NotificationCompat.Action.Builder(
                                        null,
                                        context.getString(R.string.message_button),
                                        showMeetingIntent
                                    ).build()
                                )
                            }
                        }.build()
                }

                else -> error("Unsupported Push Message type")
            }
        }

    /**
     * Get chat room title given it a ScheduledMeetingPushMessage
     *
     * @receiver    ScheduledMeetingPushMessage needed to get the title
     * @return      Title
     */
    private suspend fun ScheduledMeetingPushMessage.getChatRoomTitle(): String =
        runCatching { getChatRoom(chatRoomHandle)?.title }
            .onFailure(Timber.Forest::e)
            .getOrNull() ?: title ?: context.getString(R.string.unknown_name_label)

    /**
     * Get PendingIntent to show chat screen
     *
     * @param chatId    Chat Id
     * @return          PendingIntent
     */
    private fun getShowChatIntent(chatId: Long): PendingIntent {
        val intent = Intent(context, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            putExtra(Constants.CHAT_ID, chatId)
        }
        return PendingIntentCompat.getActivity(
            context,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT,
            false
        )
    }

    /**
     * Get PendingIntent to join a meeting
     *
     * @param chatId    Meeting's Chat Id
     * @return          PendingIntent
     */
    private fun getJoinMeetingIntent(chatId: Long): PendingIntent {
        val intent = Intent(context, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = MeetingActivity.MEETING_ACTION_JOIN
            putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        }
        return PendingIntentCompat.getActivity(
            context,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT,
            false
        )
    }
}
