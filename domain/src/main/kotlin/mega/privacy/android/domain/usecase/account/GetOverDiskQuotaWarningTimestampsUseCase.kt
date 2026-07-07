package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get the list of storage over quota warning timestamps (Unix timestamps in seconds).
 */
class GetOverDiskQuotaWarningTimestampsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return the list of warning timestamps in seconds.
     */
    suspend operator fun invoke(): List<Long> =
        accountRepository.getOverDiskQuotaWarningTimestamps()
}
