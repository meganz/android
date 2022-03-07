package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default request account deletion
 *
 * @property accountRepository
 */
class DefaultRequestAccountDeletion @Inject constructor(private val accountRepository: AccountRepository) : RequestAccountDeletion {
    override suspend fun invoke(){
        return accountRepository.requestDeleteAccountLink()
    }
}