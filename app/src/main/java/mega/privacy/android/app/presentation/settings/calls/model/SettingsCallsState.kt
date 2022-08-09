package mega.privacy.android.app.presentation.settings.calls.model

import mega.privacy.android.domain.entity.CallsSoundNotifications

/**
 * Data class representing the state of the chat settings.
 *
 * @property soundNotifications   Current chat image quality.
 */
data class SettingsCallsState(val soundNotifications: CallsSoundNotifications? = null)