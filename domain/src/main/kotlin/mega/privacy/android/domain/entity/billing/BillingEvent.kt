package mega.privacy.android.domain.entity.billing

/**
 * Billing event
 *
 */
sealed class BillingEvent {
    /**
     * On purchase update
     *
     * Emit when onPurchasesUpdated getting call
     * @property purchases
     * @property activeSubscription
     */
    data class OnPurchaseUpdate(
        val purchases: List<MegaPurchase>,
        val activeSubscription: MegaPurchase?,
    ) : BillingEvent()
}