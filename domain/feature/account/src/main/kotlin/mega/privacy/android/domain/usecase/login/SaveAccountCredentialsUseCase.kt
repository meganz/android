package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for saving in data base the user credentials of the current logged in account.
 */
class SaveAccountCredentialsUseCase @Inject constructor(private val accountRepository: AccountRepository) {

    /**
     * Invoke.
     *
     * @return [AccountSession]
     */
    suspend operator fun invoke() = accountRepository.saveAccountCredentials()
}