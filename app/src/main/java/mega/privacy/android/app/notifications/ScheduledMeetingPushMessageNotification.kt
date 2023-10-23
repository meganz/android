package mega.privacy.android.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.CallNotificationIntentService
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID_OF_INCOMING_CALL
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
import mega.privacy.android.app.utils.Constants.SCHEDULED_MEETING_ID
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.ScheduledMeetingPushMessage
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.ScheduledMeetingReminderNotificationJoinButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingReminderNotificationMessageButtonEvent
import javax.inject.Inject

/**
 * Use case to show a device Notification given a [PushMessage]
 *
 * @property notificationManagerCompat  [NotificationManagerCompat]
 */
class ScheduledMeetingPushMessageNotification @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     * Show Notification given a [PushMessage]
     *
     * @param context       [Context]
     * @param pushMessage   [PushMessage]
     */
    @SuppressLint("InlinedApi")
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun show(context: Context, pushMessage: PushMessage) {
        when (pushMessage) {
            is ScheduledMeetingPushMessage -> {
                val chatId = pushMessage.chatRoomHandle
                val schedId = pushMessage.schedId
                val channelId = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
                val showMeetingIntent = getShowChatIntent(context, chatId)
                val title = pushMessage.title ?: context.getString(R.string.unknown_name_label)
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

                val notification = NotificationCompat.Builder(context, channelId)
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
                                    getJoinMeetingIntent(context, chatId, schedId)
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

                notificationManagerCompat.notify(chatId.toInt(), notification)
            }

            else -> error("Unsupported Push Message type")
        }
    }

    /**
     * Get PendingIntent to show chat screen
     *
     * @param context       [Context]
     * @param chatId    Chat Id
     * @return          PendingIntent
     */
    private suspend fun getShowChatIntent(context: Context, chatId: Long): PendingIntent {
        Analytics.initialise(context)
        Analytics.tracker.trackEvent(ScheduledMeetingReminderNotificationMessageButtonEvent)
        val isNewChatEnable = getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)
        val intent = Intent(
            context,
            if (isNewChatEnable) ChatHostActivity::class.java else ChatActivity::class.java
        ).apply {
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
     * @param context       [Context]
     * @param chatId    Meeting's Chat Id
     * @return          PendingIntent
     */
    private fun getJoinMeetingIntent(context: Context, chatId: Long, schedId: Long): PendingIntent {
        Analytics.initialise(context)
        Analytics.tracker.trackEvent(ScheduledMeetingReminderNotificationJoinButtonEvent)
        val intent = Intent(context, MeetingActivity::class.java).apply {
            action = CallNotificationIntentService.START_SCHED_MEET
            putExtra(CHAT_ID_OF_INCOMING_CALL, chatId)
            putExtra(SCHEDULED_MEETING_ID, schedId)

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
