package mega.privacy.android.domain.usecase

/**
 * Verifies the credentials of a given user.
 */
fun interface VerifyCredentials {

    /**
     * Invoke.
     *
     * @param userEmail User's email
     */
    suspend fun invoke(userEmail: String)
}