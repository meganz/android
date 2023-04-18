package mega.privacy.android.app.activities.settingsActivities

/**
 * Chat preference state
 *
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 */
data class ChatPreferencesState(
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
)