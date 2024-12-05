package mega.privacy.android.app.presentation.settings.calls.model

import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState

/**
 * Data class representing the state of the calls settings.
 *
 * @property soundNotifications         Current sound notifications status.
 * @property callsMeetingInvitations    Current meeting invitations status.
 * @property callsMeetingReminders      Current meeting reminders status.
 */
data class SettingsCallsState(
    val soundNotifications: CallsSoundEnabledState? = null,
    val callsMeetingInvitations: CallsMeetingInvitations? = null,
    val callsMeetingReminders: CallsMeetingReminders? = null,
)
