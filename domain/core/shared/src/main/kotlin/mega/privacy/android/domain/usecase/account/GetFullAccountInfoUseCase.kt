package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import javax.inject.Inject

/**
 * Default get full account info
 */
class GetFullAccountInfoUseCase @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase,
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        // Each request stands on its own so that one failing - typically on a poor connection -
        // does not stop the remaining ones from being issued at all.
        runCatching { getPaymentMethodUseCase(true) }
            .onFailure { Log.e("Failed to get the payment method", it) }
        runCatching {
            if (monitorStorageStateEventUseCase().value.storageState == StorageState.Unknown) {
                getAccountDetailsUseCase(true)
            } else {
                getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            }
        }.onFailure { Log.e("Failed to get the account details", it) }
        runCatching { getPricing(true) }
            .onFailure { Log.e("Failed to get the pricing", it) }
        runCatching { getNumberOfSubscription(true) }
            .onFailure { Log.e("Failed to get the number of subscriptions", it) }
    }
}
