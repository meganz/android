package mega.privacy.android.domain.usecase.impl

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetFullAccountInfo
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.GetSpecificAccountDetail
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Default get full account info
 *
 */
class DefaultGetFullAccountInfo @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getPaymentMethod: GetPaymentMethod,
    private val getSpecificAccountDetail: GetSpecificAccountDetail,
) : GetFullAccountInfo {
    override suspend fun invoke() {
        getPaymentMethod(true)
        if (monitorStorageStateEventUseCase().value.storageState == StorageState.Unknown) {
            getAccountDetailsUseCase(true)
        } else {
            getSpecificAccountDetail(storage = false, transfer = true, pro = true)
        }
        getPricing(true)
        getNumberOfSubscription(true)
    }
}