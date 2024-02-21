package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality

interface ChatPreferencesGateway {
    /**
     * Gets chat image quality.
     *
     * @return Chat image quality.
     */
    fun getChatImageQualityPreference(): Flow<ChatImageQuality>

    /**
     * Gets the current chat video quality.
     *
     * @return Chat video quality.
     */
    suspend fun getChatVideoQualityPreference(): VideoQuality

    /**
     * Sets chat image quality.
     *
     * @param quality New chat image quality.
     * @return Chat image quality.
     */
    suspend fun setChatImageQualityPreference(quality: ChatImageQuality)

    /**
     * Get last contact permission requested time
     */
    fun getLastContactPermissionRequestedTime(): Flow<Long>

    /**
     * Set last contact permission requested time
     *
     * @param time
     */
    suspend fun setLastContactPermissionRequestedTime(time: Long)

    /**
     * Clears chat preferences.
     */
    suspend fun clearPreferences()
}