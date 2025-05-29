package mega.privacy.android.data.gateway.user

/**
 * Gateway for managing user login preferences.
 */
internal interface UserLoginPreferenceGateway {
    /**
     * Adds a user handle to the list of logged-in users.
     *
     * @param userHandle The user handle to add.
     */
    suspend fun addLoggedInUserHandle(userHandle: Long)

    /**
     * Checks if a user has logged in before.
     *
     * @param userHandle The user handle to check.
     * @return True if the user has logged in before, false otherwise.
     */
    suspend fun hasUserLoggedInBefore(userHandle: Long): Boolean
} 