package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.verification.VerificationStatus

/**
 * Monitor verification status
 */
fun interface MonitorVerificationStatus {
    /**
     * Invoke
     *
     * @return flow containing updates to the phone number verification status
     */
    operator fun invoke(): Flow<VerificationStatus>
}