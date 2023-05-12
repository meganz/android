package mega.privacy.android.app.presentation.settings.calls.model

import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundNotifications

/**
 * Data class representing the state of the calls settings.
 *
 * @property soundNotifications         Current sound notifications status.
 * @property callsMeetingInvitations    Current meeting invitations status.
 * @property callsMeetingReminders      Current meeting reminders status.
 * @property meetingNotificationEnabled Meeting Notification enabled.
 */
data class SettingsCallsState(
    val soundNotifications: CallsSoundNotifications? = null,
    val callsMeetingInvitations: CallsMeetingInvitations? = null,
    val callsMeetingReminders: CallsMeetingReminders? = null,
    val meetingNotificationEnabled: Boolean = false,
)
