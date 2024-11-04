package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

/**
 * Monitor visibility for almost full storage quota warning banner (yellow banner)
 */
class MonitorAlmostFullStorageBannerVisibilityUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke function
     *
     * @return Flow of Boolean, true if the banner should be visible, false otherwise
     */
    operator fun invoke(): Flow<Boolean> =
        repository.monitorAlmostFullStorageBannerClosingTimestamp().mapNotNull { timestamp ->
            if (timestamp != null) {
                (System.currentTimeMillis() - timestamp) >= 24.hours.inWholeMilliseconds
            } else {
                true
            }
        }
}