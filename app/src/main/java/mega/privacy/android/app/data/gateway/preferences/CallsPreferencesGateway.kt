package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsSoundNotifications

interface CallsPreferencesGateway {
    /**
     * Gets if notification sounds are activated when there are changes in participants in group calls or meetings.
     *
     * @return If notification sounds are enabled or disabled.
     */
    fun getCallsSoundNotificationsPreference(): Flow<CallsSoundNotifications>

    /**
     * Enable or disable notification sounds are enabled when there are changes in participants in group calls or meetings.
     *
     * @param soundNotifications True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsSoundNotificationsPreference(soundNotifications: CallsSoundNotifications)

    /**
     * Clears calls preferences.
     */
    suspend fun clearPreferences()
}