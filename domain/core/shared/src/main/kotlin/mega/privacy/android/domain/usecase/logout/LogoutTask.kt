package mega.privacy.android.domain.usecase.logout

/**
 * Logout task
 *
 * Any logic that needs to be performed on logout should implement this interface and
 * injected into the set of similar tasks.
 */
interface LogoutTask {
    /**
     * Called before logout
     */
    suspend fun onPreLogout() {}

    /**
     * Called when logout is successful
     */
    suspend fun onLogoutSuccess() {}

    /**
     * Called when logout fails
     */
    suspend fun onLogoutFailed(throwable: Throwable) {}
}