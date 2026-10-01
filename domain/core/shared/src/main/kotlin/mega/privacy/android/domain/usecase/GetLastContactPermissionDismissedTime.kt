package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Get last contact permission dismissed time
 */
fun interface GetLastContactPermissionDismissedTime {
    /**
     * invoke
     */
    suspend operator fun invoke(): Flow<Long>
}