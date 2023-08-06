package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Queries a reset password link.
 */
class QueryResetPasswordLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase
) {
    /**
     * Invoke
     * @param link as the reset password link
     */
    suspend operator fun invoke(link: String): String {
        return if (getUrlRegexPatternTypeUseCase(link) == RegexPatternType.RESET_PASSWORD_LINK) {
            accountRepository.queryResetPasswordLink(link)
        } else {
            throw ResetPasswordLinkException.LinkInvalid
        }
    }
}