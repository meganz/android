package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get the number of nodes (files and folders) in the account.
 */
class GetNumberOfNodesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return the number of nodes.
     */
    suspend operator fun invoke(): Long = accountRepository.getNumberOfNodes()
}
