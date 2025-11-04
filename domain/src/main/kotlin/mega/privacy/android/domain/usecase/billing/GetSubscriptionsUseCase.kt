package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Get list of  yearly subscriptions
 *
 * @property accountRepository                  [AccountRepository]
 * @property getLocalPricingUseCase             [GetLocalPricingUseCase]
 * @property subscriptionMapper                 [SubscriptionMapper]
 */
class GetSubscriptionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val billingRepository: BillingRepository,
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val getSubscriptionOptionsUseCase: GetSubscriptionOptionsUseCase,
    private val subscriptionMapper: SubscriptionMapper,
) {
    /**
     * Invoke
     *
     * @return [List<Subscription>]
     */
    suspend operator fun invoke(): Subscriptions {
        val paymentOptions = getSubscriptionOptionsUseCase().filter { plan ->
            plan.accountType !== AccountType.BUSINESS &&
                    plan.accountType !== AccountType.PRO_FLEXI &&
                    plan.accountType !== AccountType.STARTER &&
                    plan.accountType !== AccountType.BASIC &&
                    plan.accountType !== AccountType.ESSENTIAL &&
                    plan.accountType !== AccountType.UNKNOWN
        }
        val skus = paymentOptions.map { it.sku }.distinct()
        billingRepository.querySkus(skus)
        val monthlyOption = getOptions(paymentOptions, 1).map { option ->
            val localPricing = getLocalPricingUseCase(option.sku)
            subscriptionMapper(option, localPricing)
        }
        val yearlyOption = getOptions(paymentOptions, 12).map { option ->
            val localPricing = getLocalPricingUseCase(option.sku)
            subscriptionMapper(option, localPricing)
        }
        return Subscriptions(
            monthlySubscriptions = monthlyOption,
            yearlySubscriptions = yearlyOption
        )
    }

    private fun getOptions(paymentOptions: List<SubscriptionOption>, numberOfMonth: Int) =
        paymentOptions.filter { plan ->
            plan.months == numberOfMonth
        }
}