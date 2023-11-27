package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_1_HOUR
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_24_HOURS
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_30_MINUTES
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_6_HOURS
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

/**
 * Converts the result of muting/unmuting chat notifications to a string.
 */
fun ChatPushNotificationMuteOption.toInfoText(context: Context): String =
    when (this) {
        ChatPushNotificationMuteOption.Mute, ChatPushNotificationMuteOption.MuteUntilTurnBackOn ->
            context.getString(
                R.string.notifications_are_already_muted
            )

        ChatPushNotificationMuteOption.Unmute -> context.getString(R.string.success_unmuting_a_chat)
        ChatPushNotificationMuteOption.Mute30Minutes,
        ChatPushNotificationMuteOption.Mute1Hour,
        ChatPushNotificationMuteOption.Mute6Hours,
        ChatPushNotificationMuteOption.Mute24Hours,
        -> getMutedPeriodText(muteOption = this, context = context)

        ChatPushNotificationMuteOption.MuteUntilThisMorning,
        ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
        -> getUntilMorningText(
            muteOption = this,
            context = context
        )
    }

private fun getUntilMorningText(
    muteOption: ChatPushNotificationMuteOption,
    context: Context,
): String {
    val legacyOption = when (muteOption) {
        ChatPushNotificationMuteOption.MuteUntilThisMorning -> NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING
        ChatPushNotificationMuteOption.MuteUntilTomorrowMorning -> NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING
        else -> throw IllegalArgumentException("Invalid mute option")
    }

    return TimeUtils.getCorrectStringDependingOnCalendar(legacyOption, context)
}

private fun getMutedPeriodText(
    muteOption: ChatPushNotificationMuteOption,
    context: Context,
): String {
    val legacyOption = when (muteOption) {
        ChatPushNotificationMuteOption.Mute30Minutes -> NOTIFICATIONS_30_MINUTES
        ChatPushNotificationMuteOption.Mute1Hour -> NOTIFICATIONS_1_HOUR
        ChatPushNotificationMuteOption.Mute6Hours -> NOTIFICATIONS_6_HOURS
        ChatPushNotificationMuteOption.Mute24Hours -> NOTIFICATIONS_24_HOURS
        else -> throw IllegalArgumentException("Invalid mute option")
    }
    val text = ChatUtil.getMutedPeriodString(legacyOption)
    return context.getString(R.string.success_muting_a_chat_for_specific_time, text)
}
