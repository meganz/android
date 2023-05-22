package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.util.Locale

/**
 * Localised Subscription
 *
 * @property accountType                        Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property storage                            Amount of storage of the subscription option
 * @property monthlyTransfer                    Amount of monthly transfer quota of the subscription option
 * @property yearlyTransfer                     Amount of yearly transfer quota of the subscription option
 * @property monthlyAmount                      Currency amount object, containing monthly price amount for subscription option and local currency
 * @property yearlyAmount                       Currency amount object, containing yearly price amount for subscription option and local currency
 * @property localisedPrice                     Mapper to get localised price
 * @property localisedPriceCurrencyCode         Mapper to get localised price and currency code (e.g. "EUR")
 * @property formattedSize                      Mapper to get correctly formatted size for storage and transfer
 */
data class LocalisedSubscription(
    val accountType: AccountType,
    val storage: Int,
    val monthlyTransfer: Int,
    val yearlyTransfer: Int,
    val monthlyAmount: CurrencyAmount,
    val yearlyAmount: CurrencyAmount,
    val localisedPrice: LocalisedPriceStringMapper,
    val localisedPriceCurrencyCode: LocalisedPriceCurrencyCodeStringMapper,
    val formattedSize: FormattedSizeMapper,
) {
    /**
     * method to call LocalisedPriceStringMapper to return string containing localised price and currency sign
     *
     * @param locale [Locale]
     * @return String
     */
    fun localisePrice(locale: Locale): String = localisedPrice(monthlyAmount, locale)

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised price, currency sign and currency code
     *
     * @param locale [Locale]
     * @param isMonthly [Boolean]
     * @return Pair<String, String>
     */
    fun localisePriceCurrencyCode(locale: Locale, isMonthly: Boolean): LocalisedProductPrice =
        when (isMonthly) {
            true -> localisedPriceCurrencyCode(monthlyAmount, locale)
            false -> localisedPriceCurrencyCode(yearlyAmount, locale)
        }


    /**
     * method to call FormattedSizeMapper to return pair of int and string containing correctly formatted size for storage
     *
     * @return Pair<Int, String>
     */
    fun formatStorageSize(): FormattedSize = formattedSize(storage)

    /**
     * method to call FormattedSizeMapper to return pair of int and string containing correctly formatted size for transfer
     *
     * @param isMonthly [Boolean]
     * @return Pair<Int, String>
     */
    fun formatTransferSize(isMonthly: Boolean): FormattedSize =
        when (isMonthly) {
            true -> formattedSize(monthlyTransfer)
            false -> formattedSize(yearlyTransfer)
        }
}
