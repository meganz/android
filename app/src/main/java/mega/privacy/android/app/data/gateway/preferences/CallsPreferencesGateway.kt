package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsSoundNotifications

interface CallsPreferencesGateway {
    /**
     * Gets if sounds are enabled on participant changes in group calls and meetings.
     *
     * @return Chat image quality.
     */
    fun getCallSoundNotificationsPreference(): Flow<CallsSoundNotifications>

    /**
     * Enable or disable sounds on participant changes in calls and group meetings.
     *
     * @param soundNotifications True, if must be enabled. False, if must be disabled.
     * @return sound notifications status.
     */
    suspend fun setCallSoundNotificationsPreference(soundNotifications: CallsSoundNotifications)

    /**
     * Clears calls preferences.
     */
    suspend fun clearPreferences()
}