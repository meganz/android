package mega.privacy.android.domain.usecase

/**
 * Verifies the credentials of a given user.
 */
fun interface VerifyCredentials {

    /**
     * Invoke.
     *
     * @param userEmail User's email
     * @return True if credentials are verified, false otherwise.
     */
    suspend fun invoke(userEmail: String)
}