package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default root node exists
 *
 * @property accountRepository
 */
class DefaultRootNodeExists @Inject constructor(private val accountRepository: AccountRepository) : RootNodeExists {
    override fun invoke(): Boolean {
        return accountRepository.getRootNode() != null
    }
}