package mega.privacy.android.app.activities.settingsActivities

/**
 * Chat notification preference state
 *
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 */
data class ChatNotificationPreferencesState(
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
)