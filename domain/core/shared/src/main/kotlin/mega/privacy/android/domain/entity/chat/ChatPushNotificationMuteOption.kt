package mega.privacy.android.domain.entity.chat

/**
 * Enum class to represent the different options to mute a chat or global chat notifications.
 */
enum class ChatPushNotificationMuteOption {

    /**
     * Mute chat notifications
     */
    Mute,

    /**
     * Unmute chat notifications
     */
    Unmute,

    /**
     * Mute chat notifications for 30 minutes
     */
    Mute30Minutes,

    /**
     * Mute chat notifications for 1 hour
     */
    Mute1Hour,

    /**
     * Mute chat notifications for 6 hours
     */
    Mute6Hours,

    /**
     * Mute chat notifications for 24 hours
     */
    Mute24Hours,


    /**
     * Mute chat notifications until this morning
     */
    MuteUntilThisMorning,

    /**
     * Mute chat notifications until tomorrow morning
     */
    MuteUntilTomorrowMorning,

    /**
     * Mute chat notifications until it is turned back on
     */
    MuteUntilTurnBackOn,
}