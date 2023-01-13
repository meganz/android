package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * The use case for getting ticker
 */
fun interface GetTicker {

    /**
     * Get ticker
     *
     * @param intervalMilliSeconds the interval time
     */
    suspend operator fun invoke(intervalMilliSeconds: Long): Flow<Unit>
}
