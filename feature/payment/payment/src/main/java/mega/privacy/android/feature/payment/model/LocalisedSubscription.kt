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
 * @property monthlySubscription                Subscription option for monthly plan
 * @property yearlySubscription                 Subscription option for yearly plan
 * @property localisedPriceCurrencyCode         Mapper to get localised price and currency code (e.g. "EUR")
 * @property formattedSize                      Mapper to get correctly formatted size for storage and transfer
 */
data class LocalisedSubscription(
    val monthlySubscription: Subscription,
    val yearlySubscription: Subscription,
    val localisedPriceCurrencyCode: LocalisedPriceCurrencyCodeStringMapper,
    val formattedSize: FormattedSizeMapper,
) {
    val accountType: AccountType = monthlySubscription.accountType
    val storage: Int = monthlySubscription.storage

    private val yearlyAmountPerMonth = CurrencyAmount(
        yearlySubscription.amount.value / 12,
        yearlySubscription.amount.currency,
    )

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised price, currency sign and currency code
     *
     * @param locale [Locale]
     * @param isMonthly [Boolean]
     * @return Pair<String, String>
     */
    fun localisePriceCurrencyCode(locale: Locale, isMonthly: Boolean): LocalisedProductPrice =
        when (isMonthly) {
            true -> localisedPriceCurrencyCode(monthlySubscription.amount, locale)
            false -> localisedPriceCurrencyCode(yearlySubscription.amount, locale)
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
            monthlySubscription.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(it, locale)
            }
        } else {
            yearlySubscription.discountedAmountMonthly?.let {
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
            monthlySubscription.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(CurrencyAmount(it.value * 12, it.currency), locale)
            }
        } else {
            yearlySubscription.discountedAmountMonthly?.let {
                localisedPriceCurrencyCode(CurrencyAmount(it.value * 12, it.currency), locale)
            }
        }

    /**
     * product price of the yearly amount per month
     */
    fun localisePriceOfYearlyAmountPerMonth(locale: Locale): LocalisedProductPrice =
        localisedPriceCurrencyCode(yearlyAmountPerMonth, locale)

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
    fun formatTransferSize(isMonthly: Boolean): FormattedSize =
        if (isMonthly) {
            formattedSize(size = monthlySubscription.transfer)
        } else {
            formattedSize(size = yearlySubscription.transfer)
        }

    val hasDiscount: Boolean
        get() = monthlySubscription.discountedAmountMonthly != null || yearlySubscription.discountedAmountMonthly != null

    /**
     * Get offer id based on subscription period
     */
    fun getOfferId(isMonthly: Boolean): String? = if (isMonthly) {
        monthlySubscription.offerId
    } else {
        yearlySubscription.offerId
    }
}
