package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber


/**
 * Monitor verified phone number
 */
fun interface MonitorVerifiedPhoneNumber {

    /**
     * Invoke
     *
     * @return flow of verified phone number changes
     */
    operator fun invoke(): Flow<VerifiedPhoneNumber>
}