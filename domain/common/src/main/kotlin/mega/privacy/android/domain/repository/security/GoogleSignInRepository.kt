package mega.privacy.android.domain.repository.security

import mega.privacy.android.domain.entity.login.GoogleSignInResult

/**
 * Repository for Google Sign-In operations.
 */
interface GoogleSignInRepository {
    /**
     * Parse the Google ID token and return the sign-in result.
     *
     * @param idToken The raw Google ID token JWT obtained from Credential Manager.
     * @return [GoogleSignInResult] with email, sub, and profile info.
     * @throws [mega.privacy.android.domain.exception.login.GoogleSignInException] on failure.
     */
    suspend fun signIn(idToken: String): GoogleSignInResult
}
