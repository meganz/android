package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Pushes repository.
 */
interface PushesRepository {

    /**
     * Gets push token.
     *
     * @return The push token.
     */
    fun getPushToken(): String

    /**
     * Registers push notifications.
     *
     * @param deviceType    Type of device.
     * @param newToken      New push token.
     * @return The push token.
     */
    suspend fun registerPushNotifications(deviceType: Int, newToken: String): String

    /**
     * Sets push token.
     *
     * @param newToken  The push token.
     */
    fun setPushToken(newToken: String)

    /**
     * Notifies a push has been received.
     *
     * @param beep   True if should beep, false otherwise.
     * @return Result of the request. Required for creating the notification.
     */
    suspend fun pushReceived(beep: Boolean): ChatRequest
}