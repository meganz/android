package mega.privacy.android.domain.usecase

/**
 * Checks if the credentials of a given user are verified.
 */
fun interface AreCredentialsVerified {

    /**
     * Invoke.
     *
     * @param userEmail User's email.
     * @return True if credentials are verified, false otherwise.
     */
    suspend fun invoke(userEmail: String): Boolean
}