package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Acknowledge the last purge notification so it is not shown again on any device.
 */
class AcknowledgeLastPurgeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke.
     *
     * @param purgeTimestamp the Unix timestamp (seconds) of the purge to acknowledge.
     */
    suspend operator fun invoke(purgeTimestamp: Long) =
        accountRepository.setLastPurgeAcknowledged(purgeTimestamp)
}
