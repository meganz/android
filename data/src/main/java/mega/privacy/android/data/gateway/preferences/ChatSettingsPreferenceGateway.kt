package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.settings.ChatSettings

/**
 * Chat Settings Preference Gateway
 */
interface ChatSettingsPreferenceGateway {

    /**
     * Monitor the Chat Settings
     *
     * @return A [Flow] of [ChatSettings]. The [Flow] emits null if no Chat Settings are stored
     */
    fun monitorChatSettings(): Flow<ChatSettings?>

    /**
     * Retrieves the stored Chat Settings
     *
     * @return The stored [ChatSettings], or null if none are stored
     */
    suspend fun getChatSettings(): ChatSettings?

    /**
     * Stores the given Chat Settings
     *
     * @param chatSettings The [ChatSettings] to store
     */
    suspend fun setChatSettings(chatSettings: ChatSettings)

    /**
     * Sets the notification sound for chat
     *
     * @param sound The notification sound value
     */
    suspend fun setNotificationSound(sound: String?)

    /**
     * Sets whether vibration is enabled for chat notifications
     *
     * @param enabled The vibration enabled value
     */
    suspend fun setVibrationEnabled(enabled: String?)

    /**
     * Sets the chat video quality
     *
     * @param quality The video quality value
     */
    suspend fun setVideoQuality(quality: String?)

    /**
     * Clears all stored Chat Settings
     */
    suspend fun clearPreferences()
}
