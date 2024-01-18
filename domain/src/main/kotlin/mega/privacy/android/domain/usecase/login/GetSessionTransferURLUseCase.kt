package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.exception.NullSessionTransferURLException
import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Get session transfer URL
 */
class GetSessionTransferURLUseCase @Inject constructor(
    private val repository: LoginRepository,
) {
    /**
     * Invoke
     *
     * @return [String] or throw an exception if it fails
     */
    suspend operator fun invoke(path: String): String =
        repository.getSessionTransferURL(path) ?: throw NullSessionTransferURLException()
}