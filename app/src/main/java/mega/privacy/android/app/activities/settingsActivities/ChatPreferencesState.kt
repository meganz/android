package mega.privacy.android.app.activities.settingsActivities

/**
 * Chat preference state
 *
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 * @property signalPresenceUpdate There is a signal presence update.
 */
data class ChatPreferencesState(
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val signalPresenceUpdate: Boolean = false,
)