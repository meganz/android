package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 *Check whether the user account is new or not
 *
 * @return if the account is new or not
 */
class IsAccountNewUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean =
        accountRepository.isAccountNew()
}