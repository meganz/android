package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.billing.BillingEvent

/**
 * Monitor billing event
 * listen purchase updated when user process the purchase
 */
fun interface MonitorBillingEvent {
    operator fun invoke(): Flow<BillingEvent>
}