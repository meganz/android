package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundNotifications

interface CallsPreferencesGateway {

    /**
     * Gets if notification sounds are activated when there are changes in participants in group calls or meetings.
     *
     * @return If notification sounds are enabled or disabled.
     */
    fun getCallsSoundNotificationsPreference(): Flow<CallsSoundNotifications>

    /**
     * Get calls meeting invitations preference
     *
     * @return If meeting invitations are enabled or disabled.
     */
    fun getCallsMeetingInvitationsPreference(): Flow<CallsMeetingInvitations>

    /**
     * Get calls meeting reminders preference
     *
     * @return If meeting reminders are enabled or disabled.
     */
    fun getCallsMeetingRemindersPreference(): Flow<CallsMeetingReminders>

    /**
     * Enable or disable notification sounds are enabled when there are changes in participants in group calls or meetings.
     *
     * @param soundNotifications True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsSoundNotificationsPreference(soundNotifications: CallsSoundNotifications)

    /**
     * Enable or disable calls meeting invitations preference
     *
     * @param callsMeetingInvitations True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsMeetingInvitationsPreference(callsMeetingInvitations: CallsMeetingInvitations)

    /**
     * Enable or disable calls meeting reminders preference
     *
     * @param callsMeetingReminders True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsMeetingRemindersPreference(callsMeetingReminders: CallsMeetingReminders)

    /**
     * Clears calls preferences.
     */
    suspend fun clearPreferences()
}