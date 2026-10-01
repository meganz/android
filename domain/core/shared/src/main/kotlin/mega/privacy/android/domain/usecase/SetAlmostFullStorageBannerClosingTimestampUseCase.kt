package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Set timestamp for when user closed the almost full storage quota warning banner (yellow banner) to the current time
 */
class SetAlmostFullStorageBannerClosingTimestampUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke function
     *
     */
    suspend operator fun invoke() {
        repository.setAlmostFullStorageBannerClosingTimestamp(System.currentTimeMillis())
    }
}