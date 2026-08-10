package mega.privacy.android.app.fragments.settingsFragments

/**
 * UI state for the chat notifications settings screen.
 *
 * @property notificationsSound the raw notification sound value, or null if none is set.
 * @property isVibrationEnabled whether vibration is enabled for chat notifications.
 */
data class SettingsChatNotificationsUiState(
    val notificationsSound: String? = null,
    val isVibrationEnabled: Boolean = true,
)
