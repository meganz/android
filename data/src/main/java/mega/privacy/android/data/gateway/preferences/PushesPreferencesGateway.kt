package mega.privacy.android.data.gateway.preferences

/**
 * Pushes Preferences Gateway
 */
interface PushesPreferencesGateway {

    /**
     * Gets the push token stored in the data store.
     *
     * @return The push token, or an empty string if none has been stored.
     */
    suspend fun getPushToken(): String

    /**
     * Sets the push token in the data store.
     *
     * @param newToken The push token.
     */
    suspend fun setPushToken(newToken: String)

    /**
     * Clears the stored push token.
     */
    suspend fun clearPushToken()
}
