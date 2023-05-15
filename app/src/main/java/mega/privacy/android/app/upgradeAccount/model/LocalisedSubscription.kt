package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.util.Locale

/**
 * Localised Subscription
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property handle          Subscription option handle
 * @property storage         Amount of storage of the subscription option
 * @property transfer        Amount of transfer quota of the subscription option
 * @property amount          Currency amount object, containing price amount for subscription option and local currency
 * @property localisedPrice  Mapper to get localised price
 * @property localisedPriceCurrencyCode Mapper to get localised price and currency code (e.g. "EUR")
 */
data class LocalisedSubscription(
    val accountType: AccountType,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyAmount,
    val localisedPrice: LocalisedPriceStringMapper,
    val localisedPriceCurrencyCode: LocalisedPriceCurrencyCodeStringMapper,
) {
    /**
     * method to call LocalisedPriceStringMapper to return string containing localised price and currency sign
     *
     * @param locale [Locale]
     * @return String
     */
    fun localisePrice(locale: Locale): String = localisedPrice(amount, locale)

    /**
     * method to call LocalisedPriceCurrencyCodeStringMapper to return pair of strings containing localised price, currency sign and currency code
     *
     * @param locale [Locale]
     * @return Pair<String, String>
     */
    fun localisePriceCurrencyCode(locale: Locale): Pair<String, String> =
        localisedPriceCurrencyCode(amount, locale)
}
