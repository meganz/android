package mega.privacy.android.data.repository

import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Default implementation of [BillingRepository]
 *
 */
internal class DefaultBillingRepository @Inject constructor(
    private val accountInfoWrapper: AccountInfoWrapper,
) : BillingRepository {

    override suspend fun getLocalPricing(sku: String): MegaSku? =
        accountInfoWrapper.availableSkus.firstOrNull { megaSku ->
            megaSku.sku == sku
        }
}

