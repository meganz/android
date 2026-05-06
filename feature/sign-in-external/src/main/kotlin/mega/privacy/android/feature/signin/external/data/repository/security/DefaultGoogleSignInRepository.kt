package mega.privacy.android.feature.signin.external.data.repository.security

import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import mega.privacy.android.feature.signin.external.data.mapper.login.GoogleIdTokenMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [mega.privacy.android.domain.repository.security.GoogleSignInRepository].
 */
@Singleton
internal class DefaultGoogleSignInRepository @Inject constructor(
    private val googleIdTokenMapper: GoogleIdTokenMapper,
) : GoogleSignInRepository {

    /**
     * @param idToken The raw Google ID token JWT obtained from Credential Manager.
     */
    override suspend fun signIn(idToken: String): GoogleSignInResult =
        runCatching { googleIdTokenMapper(idToken) }
            .getOrElse { throw GoogleSignInException.Unknown(it.message) }
}
