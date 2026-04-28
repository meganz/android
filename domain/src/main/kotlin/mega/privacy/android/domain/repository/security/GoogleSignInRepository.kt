package mega.privacy.android.domain.repository.security

import mega.privacy.android.domain.entity.login.GoogleSignInResult

/**
 * Repository for Google Sign-In operations.
 */
interface GoogleSignInRepository {
    /**
     * Launch Google Sign-In via Credential Manager and return the result.
     *
     * @return [GoogleSignInResult] with email, sub, and profile info.
     * @throws [mega.privacy.android.domain.exception.login.GoogleSignInException] on failure.
     */
    suspend fun signIn(): GoogleSignInResult
}
