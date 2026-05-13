package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import javax.inject.Inject

/**
 * Decodes a Google ID token (JWT) to extract the user's email, Google sub
 * (used as MEGA password), and optional first/last name.
 */
class DecodeGoogleIdTokenUseCase @Inject constructor(
    private val googleSignInRepository: GoogleSignInRepository,
) {
    suspend operator fun invoke(idToken: String): GoogleSignInResult =
        googleSignInRepository.signIn(idToken)
}
