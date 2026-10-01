package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get the storage over quota deletion deadline (Unix timestamp in seconds).
 */
class GetOverDiskQuotaDeadlineUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return the deadline timestamp in seconds, or a negative value if there is no deadline.
     */
    suspend operator fun invoke(): Long = accountRepository.getOverDiskQuotaDeadline()
}
