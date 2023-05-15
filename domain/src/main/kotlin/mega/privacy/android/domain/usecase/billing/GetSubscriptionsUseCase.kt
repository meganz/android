package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get list of subscriptions
 *
 * @property accountRepository                  [AccountRepository]
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property calculateCurrencyAmountUseCase     [CalculateCurrencyAmountUseCase]
 */
class GetSubscriptionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val calculateCurrencyAmountUseCase: CalculateCurrencyAmountUseCase,
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getAppSubscriptionOptionsUseCase: GetAppSubscriptionOptionsUseCase,
) {
    suspend operator fun invoke(): List<Subscription> {
        return getAppSubscriptionOptionsUseCase().map { plan ->
            val sku = getSku(plan.accountType)
            val localPricing = sku?.let { getLocalPricingUseCase(it) }

            Subscription(
                accountType = plan.accountType,
                handle = plan.handle,
                storage = plan.storage,
                transfer = plan.transfer,
                amount = localPricing?.let {
                    calculateCurrencyAmountUseCase(
                        it.amount,
                        it.currency
                    )
                }
                    ?: calculateCurrencyAmountUseCase(plan.amount, plan.currency),
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