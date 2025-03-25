package mega.privacy.android.app.notifications

import mega.privacy.android.icon.pack.R as iconPackR
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.app.meeting.CallNotificationIntentService
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID_OF_INCOMING_CALL
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.pushes.PushMessage.CallPushMessage
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager class show a device Notification given a [CallPushMessage]
 *
 * @property notificationManagerCompat  [NotificationManagerCompat]
 */
class CallPushMessageNotificationManager @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
) {
    private val channelId = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
    private var callPushMessageChatId: Long? = null

    /**
     * Show Notification given a [CallPushMessage]
     *
     * @param context       [Context]
     * @param chatId        Chat id
     */
    fun show(context: Context, chatId: Long) {
        Timber.d("Show call push message notification")
        callPushMessageChatId = chatId
        val title = context.getString(R.string.title_mega_info_empty_screen)
        val id = getNotificationId(chatId)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            MegaApplication.getInstance()
                .createOrUpdateAudioManager(
                    false,
                    Constants.AUDIO_MANAGER_CALL_RINGING
                )
            notify(context = context, chatId = chatId, title = title, hasInfo = false, notifId = id)
        }
    }

    /**
     * Remove notification (hide it and stop sounds)
     *
     * @param chatId    Chat Id
     */
    fun remove(chatId: Long) {
        Timber.d("Remove call push message notification")
        hide(chatId)
        rtcAudioManagerGateway.stopIncomingCallSounds()
        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notify(
        context: Context,
        chatId: Long,
        title: String,
        isOneToOne: Boolean? = null,
        hasInfo: Boolean,
        notifId: Int,
    ) {
        val description = context.getString(R.string.title_notification_incoming_call)
        val priority = NotificationCompat.PRIORITY_HIGH

        val notificationColor = ContextCompat.getColor(context, R.color.red_600_red_300)
        val notificationTitle = EmojiUtilsShortcodes.emojify(title)

        val person =
            if (hasInfo) Person.Builder().apply { setName(notificationTitle) }.build() else null

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationTitle)
            .setContentText(description)
            .setContentIntent(getMeetingIntent(context = context, chatId = chatId))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(priority)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setDeleteIntent(
                getPendingIntent(
                    context = context,
                    chatId = chatId,
                    intentAction = CallNotificationIntentService.DISMISS
                )
            )
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(notificationColor)
            .apply {
                if (hasInfo) {
                    person?.let { addPerson(it) }
                }

                if (hasInfo && isOneToOne != null) {
                    addAction(
                        NotificationCompat.Action.Builder(
                            null,
                            if (isOneToOne) context.getString(R.string.answer_call_incoming) else context.getString(
                                R.string.meetings_list_join_scheduled_meeting_option
                            ),
                            getPendingIntent(
                                context = context,
                                chatId = chatId,
                                intentAction = CallNotificationIntentService.ANSWER
                            )
                        ).build()
                    )
                    addAction(
                        NotificationCompat.Action.Builder(
                            null,
                            if (isOneToOne) context.getString(R.string.contact_decline) else context.getString(
                                R.string.ignore_call_incoming
                            ),
                            getPendingIntent(
                                context = context,
                                chatId = chatId,
                                intentAction = if (isOneToOne) CallNotificationIntentService.DECLINE else CallNotificationIntentService.IGNORE
                            )
                        ).build()
                    )
                }
            }.build()

        notificationManagerCompat.notify(notifId, notification)
    }

    /**
     * Update Notification given a [ChatRoom]
     *
     * @param context       [Context]
     * @param chat          [ChatRoom]
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun update(context: Context, chat: ChatRoom) {
        Timber.d("Update call push message notification")
        notify(
            context = context,
            chatId = chat.chatId,
            isOneToOne = !chat.isMeeting && !chat.isGroup,
            title = chat.title,
            hasInfo = true,
            notifId = getNotificationId(chat.chatId)
        )
    }

    /**
     * Hide Notification
     *
     * @param chatId    Chat id
     */
    fun hide(chatId: Long) {
        if (callPushMessageChatId == chatId) {
            Timber.d("Cancel call push message notification")
            notificationManagerCompat.cancel(getNotificationId(chatId))
        }
    }

    private fun getMeetingIntent(context: Context, chatId: Long): PendingIntent? {
        val intent = Intent(
            context,
            MeetingActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MeetingActivity.MEETING_ACTION_RINGING
            putExtra(MEETING_CHAT_ID, chatId)
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
     * Get pending intent
     *
     * @param context       [Context]
     * @param chatId        Chat id
     * @param intentAction  Intent action
     */
    private fun getPendingIntent(
        context: Context,
        chatId: Long,
        intentAction: String,
    ): PendingIntent? = PendingIntent.getService(
        context,
        0,
        Intent(context, CallNotificationIntentService::class.java).apply {
            action = intentAction
            putExtra(CHAT_ID_OF_INCOMING_CALL, chatId)
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    /**
     * Get notification id
     *
     * @param chatId    Chat id
     */
    private fun getNotificationId(chatId: Long): Int = MegaApiAndroid.userHandleToBase64(chatId)
        .hashCode() + Constants.NOTIFICATION_CALL_IN_PROGRESS
}