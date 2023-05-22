package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.upgradeAccount.model.LocalisedProductPrice
import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper for CurrencyAmount convert to LocalisedProductPrice (formatted strings with price and currency code)
 */
class LocalisedPriceCurrencyCodeStringMapper @Inject constructor() {

    /**
     * Invoke
     * Convert CurrencyAmount to LocalisedProductPrice
     * @param currencyAmount [CurrencyAmount]
     * @param locale [Locale]
     * @return LocalisedProductPrice
     */
    internal operator fun invoke(
        currencyAmount: CurrencyAmount,
        locale: Locale,
    ): LocalisedProductPrice {
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
        currencyFormatter.currency = Currency.getInstance(currencyAmount.currency.code)
        val currencyCode = currencyFormatter.currency?.currencyCode ?: "EUR"
        return LocalisedProductPrice(currencyFormatter.format(currencyAmount.value), currencyCode)
    }
}