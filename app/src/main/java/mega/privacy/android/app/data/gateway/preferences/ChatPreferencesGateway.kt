package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatImageQuality

interface ChatPreferencesGateway {
    /**
     * Gets chat image quality.
     *
     * @return Chat image quality.
     */
    fun getChatImageQualityPreference(): Flow<ChatImageQuality>

    /**
     * Sets chat image quality.
     *
     * @param quality New chat image quality.
     * @return Chat image quality.
     */
    suspend fun setChatImageQualityPreference(quality: ChatImageQuality)

    /**
     * Clears chat preferences.
     */
    suspend fun clearPreferences()
}