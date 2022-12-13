package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default implementation of [GetSubscriptions]
 *
 * @property accountRepository           [AccountRepository]
 * @property getLocalPricing             [GetLocalPricing]
 * @property calculateCurrencyAmount     [CalculateCurrencyAmount]
 */
class DefaultGetSubscriptions @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getLocalPricing: GetLocalPricing,
    private val calculateCurrencyAmount: CalculateCurrencyAmount,
    private val getAppSubscriptionOptions: GetAppSubscriptionOptions,
) : GetSubscriptions {
    override suspend fun invoke(): List<Subscription> {
        return getAppSubscriptionOptions().map { plan ->
            val sku = getSku(plan.accountType)
            val localPricing = sku?.let { getLocalPricing(it) }

            Subscription(
                accountType = plan.accountType,
                handle = plan.handle,
                storage = plan.storage,
                transfer = plan.transfer,
                amount = localPricing?.let { calculateCurrencyAmount(it.amount, it.currency) }
                    ?: calculateCurrencyAmount(plan.amount, plan.currency),
            )
        }
    }

    private fun getSku(accountType: AccountType) = when (accountType) {
        AccountType.PRO_LITE -> Skus.SKU_PRO_LITE_MONTH
        AccountType.PRO_I -> Skus.SKU_PRO_I_MONTH
        AccountType.PRO_II -> Skus.SKU_PRO_II_MONTH
        AccountType.PRO_III -> Skus.SKU_PRO_III_MONTH
        else -> null
    }
}