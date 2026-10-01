package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Set User Credentials Use Case
 *
 */
class SetUserCredentialsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(credentials: UserCredentials) =
        repository.setCredentials(credentials)
}