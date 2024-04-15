package mega.privacy.android.domain.usecase.login.confirmemail

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * A use case to resend the sign up link to a new contact
 *
 * @property loginRepository [LoginRepository]
 */
class ResendSignUpLinkUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {

    /**
     * Invocation method to resend the sign up link
     *
     * @param email    Email for the account
     * @param fullName Full name of the user
     */
    suspend operator fun invoke(email: String, fullName: String) {
        loginRepository.resendSignupLink(email, fullName)
    }
}
