package mega.privacy.android.app.presentation.settings.calls.model

import mega.privacy.android.domain.entity.CallsSoundNotifications

/**
 * Data class representing the state of the calls settings.
 *
 * @property soundNotifications  Current sound notifications status.
 */
data class SettingsCallsState(val soundNotifications: CallsSoundNotifications? = null)