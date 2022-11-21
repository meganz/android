package mega.privacy.android.app.data.repository

import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.wrapper.PaymentUtilsWrapper
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

/**
 * Default implementation of [BillingRepository]
 *
 * @property myAccountInfo
 */
@ExperimentalContracts
internal class DefaultBillingRepository @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    private val paymentUtilsWrapper: PaymentUtilsWrapper,
) : BillingRepository {

    override suspend fun getLocalPricing(sku: String): MegaSku? =
        paymentUtilsWrapper.getSkuDetails(myAccountInfo.availableSkus, sku)
}

