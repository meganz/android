package mega.privacy.android.domain.usecase

/**
 * Is User Logged In
 *
 */
fun interface IsUserLoggedIn {
    /**
     * Invoke
     *
     * @return boolean for loggedIn state
     */
    suspend operator fun invoke(): Boolean
}