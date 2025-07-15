package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchaseState
import javax.inject.Inject

/**
 * Use case to monitor successful purchases.
 * It filters the billing events to only include successful purchase updates.
 */
class MonitorSuccessfulPurchasesUseCase @Inject constructor(private val monitorBillingEventUseCase: MonitorBillingEventUseCase) {
    operator fun invoke() = monitorBillingEventUseCase()
        .filterIsInstance<BillingEvent.OnPurchaseUpdate>()
        .filter { it.purchases.firstOrNull()?.megaPurchaseState == MegaPurchaseState.Purchased }
}