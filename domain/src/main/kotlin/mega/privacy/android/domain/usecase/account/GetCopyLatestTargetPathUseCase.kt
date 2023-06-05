package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Get latest target path of copy
 */
class GetCopyLatestTargetPathUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Long? {
        val path = accountRepository.getLatestTargetPathCopyPreference()
        return path?.takeIf { !isNodeInRubbishOrDeletedUseCase(it) }
    }
}