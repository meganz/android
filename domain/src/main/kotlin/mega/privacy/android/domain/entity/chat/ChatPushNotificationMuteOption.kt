package mega.privacy.android.domain.entity.chat

/**
 * Enum class to represent the different options to mute a chat or global chat notifications.
 */
sealed interface ChatPushNotificationMuteOption {

    /**
     * Mute chat notifications
     */
    data object Mute : ChatPushNotificationMuteOption

    /**
     * Unmute chat notifications
     */
    data object Unmute : ChatPushNotificationMuteOption

    /**
     * Mute chat notifications for 30 minutes
     */
    data object Mute30Minutes : ChatPushNotificationMuteOption

    /**
     * Mute chat notifications for 1 hour
     */
    data object Mute1Hour : ChatPushNotificationMuteOption

    /**
     * Mute chat notifications for 6 hours
     */
    data object Mutet6Hours : ChatPushNotificationMuteOption

    /**
     * Mute chat notifications for 24 hours
     */
    data object Mute24Hours : ChatPushNotificationMuteOption


    /**
     * Mute chat notifications until this morning
     */
    data object MuteUntilThisMorning : ChatPushNotificationMuteOption

    /**
     * Mute chat notifications until tomorrow morning
     */
    data object MutetUntilTomorrowMorning : ChatPushNotificationMuteOption
}