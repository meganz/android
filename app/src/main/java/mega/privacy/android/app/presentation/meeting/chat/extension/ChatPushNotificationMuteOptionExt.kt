package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

/**
 * Converts the result of muting/unmuting chat notifications to a string.
 */
fun ChatPushNotificationMuteOption.toString(context: Context): String =
    when (this) {
        is ChatPushNotificationMuteOption.Mute -> context.getString(R.string.notifications_are_already_muted)
        is ChatPushNotificationMuteOption.Unmute -> context.getString(R.string.success_unmuting_a_chat)
        is ChatPushNotificationMuteOption.Mute30Minutes -> TODO()
        is ChatPushNotificationMuteOption.Mute1Hour -> TODO()
        is ChatPushNotificationMuteOption.Mutet6Hours -> TODO()
        is ChatPushNotificationMuteOption.Mute24Hours -> TODO()
        is ChatPushNotificationMuteOption.MuteUntilThisMorning -> TODO()
        is ChatPushNotificationMuteOption.MutetUntilTomorrowMorning -> TODO()
    }