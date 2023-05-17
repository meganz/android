package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get latest target path of copy
 */
class GetCopyLatestTargetPathUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Long? = accountRepository.getLatestTargetPathCopyPreference()
}