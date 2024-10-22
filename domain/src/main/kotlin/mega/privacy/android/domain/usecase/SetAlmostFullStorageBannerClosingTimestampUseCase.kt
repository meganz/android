package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Set timestamp for when user closed the almost full storage quota warning banner (yellow banner)
 */
class SetAlmostFullStorageBannerClosingTimestampUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke function
     *
     * @param timestamp
     */
    suspend operator fun invoke(timestamp: Long) {
        repository.setAlmostFullStorageBannerClosingTimestamp(timestamp)
    }
}