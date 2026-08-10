package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Queries a reset password link.
 */
class QueryResetPasswordLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     * @param link as the reset password link
     *
     * @return ResetPasswordLinkInfo containing email and flag associated with the reset password link
     */
    suspend operator fun invoke(link: String) =
        accountRepository.queryResetPasswordLink(link)
}