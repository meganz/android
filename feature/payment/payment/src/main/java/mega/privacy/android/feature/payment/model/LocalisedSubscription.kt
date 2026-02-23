package mega.privacy.android.feature.payment.model

import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.core.formatter.model.FormattedSize
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import java.util.Locale

/**
 * Localised Subscription
 *
 * @property monthlySubscription                Subscription option for monthly plan, null if only yearly is available
 * @property yearlySubscription                 Subscription option for yearly plan, null if only monthly is available
 * @property localisedPriceCurrencyCode         Mapper to get localised price and currency code (e.g. "EUR")
 * @property formattedSize                      Mapper to get correctly formatted size for storage and transfer
 */
data class LocalisedSubscription(
    val monthlySubscription: Subscription?,
    val yearlySubscription: Subscription?,
    val localisedPriceCurrencyCode: LocalisedPriceCurrencyCodeStringMapper,
    val formattedSize: FormattedSizeMapper,
) {
    init {
        require(monthlySubscription != null || yearlySubscription != null) {
            "At least one of monthlySubscription or yearlySubscription must be non-null"
        }
    }

    val accountType: AccountType
        get() = (monthlySubscription ?: yearlySubscription)!!.accountType

    val storage: Int
        get() = (monthlySubscription ?: yearlySubscription)!!.storage

    private val yearlyAmountPerMonth: CurrencyAmount?
        get() = yearlySubscription?.let {
            CurrencyAmount(it.amount.value / 12, it.amount.currency)
        }

    /**
     * Check if this subscription has an option for the given period.
     */
    fun hasSubscriptionFor(isMonthly: Boolean): Boolean =
        if (isMonthly) monthlySubscription != null else yearlySubscription != null

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised price, currency sign and currency code
     *
     * @param locale [Locale]
     * @param isMonthly [Boolean]
     * @return Pair<String, String>
     */
    fun localisePriceCurrencyCode(locale: Locale, isMonthly: Boolean): LocalisedProductPrice {
        val subscription = if (isMonthly) monthlySubscription else yearlySubscription
        requireNotNull(subscription) { "Subscription for isMonthly=$isMonthly is not available" }
        return localisedPriceCurrencyCode(subscription.amount, locale)
    }

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised discounted price, currency sign and currency code
     *
     * @param locale [Locale]
     * @param isMonthly [Boolean]
     */
    fun localiseDiscountedPriceMonthlyCurrencyCode(
        locale: Locale,
        isMonthly: Boolean,
    ): LocalisedProductPrice? =
        if (isMonthly) {
            monthlySubscription?.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(it, locale)
            }
        } else {
            yearlySubscription?.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(it, locale)
            }
        }

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised discounted price, currency sign and currency code
     *
     * @param locale [Locale]
     * @param isMonthly [Boolean]
     */
    fun localiseDiscountedPriceYearlyCurrencyCode(
        locale: Locale,
        isMonthly: Boolean,
    ): LocalisedProductPrice? =
        if (isMonthly) {
            monthlySubscription?.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(CurrencyAmount(it.value * 12, it.currency), locale)
            }
        } else {
            yearlySubscription?.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(CurrencyAmount(it.value * 12, it.currency), locale)
            }
        }

    /**
     * product price of the yearly amount per month. Returns null if yearly subscription is not available.
     */
    fun localisePriceOfYearlyAmountPerMonth(locale: Locale): LocalisedProductPrice? =
        yearlyAmountPerMonth?.let { localisedPriceCurrencyCode(it, locale) }

    /**
     * method to call FormattedSizeMapper to return pair of int and string containing correctly formatted size for storage
     *
     * @return Pair<Int, String>
     */
    fun formatStorageSize(usePlaceholder: Boolean = true): FormattedSize =
        formattedSize(size = storage, usePlaceholder = usePlaceholder)

    /**
     * method to call FormattedSizeMapper to return pair of int and string containing correctly formatted size for transfer
     *
     * @param isMonthly [Boolean]
     * @return Pair<Int, String>
     */
    fun formatTransferSize(isMonthly: Boolean): FormattedSize {
        val subscription = if (isMonthly) monthlySubscription else yearlySubscription
        requireNotNull(subscription) { "Subscription for isMonthly=$isMonthly is not available" }
        return formattedSize(size = subscription.transfer)
    }

    val hasDiscount: Boolean
        get() = monthlySubscription?.discountedAmountMonthly != null ||
                yearlySubscription?.discountedAmountMonthly != null

    /**
     * Get subscription for the given period. Returns null if not available for that period.
     */
    fun getSubscription(isMonthly: Boolean): Subscription? = if (isMonthly) {
        monthlySubscription
    } else {
        yearlySubscription
    }
}
