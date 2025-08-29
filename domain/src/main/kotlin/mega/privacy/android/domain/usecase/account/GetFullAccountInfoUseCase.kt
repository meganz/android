package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
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
        getPaymentMethodUseCase(true)
        if (monitorStorageStateEventUseCase().value.storageState == StorageState.Unknown) {
            getAccountDetailsUseCase(true)
        } else {
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
        getPricing(true)
        getNumberOfSubscription(true)
    }
}