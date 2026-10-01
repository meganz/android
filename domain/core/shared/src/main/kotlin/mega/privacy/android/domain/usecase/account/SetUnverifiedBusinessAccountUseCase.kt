package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for setting the unverified business account state
 */
class SetUnverifiedBusinessAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Set the state indicating if the current account is an unverified business account
     *
     * @param isUnverified true if the account is unverified, false otherwise
     */
    suspend operator fun invoke(isUnverified: Boolean) {
        accountRepository.setIsUnverifiedBusinessAccount(isUnverified)
    }
}