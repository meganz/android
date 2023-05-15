package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper for CurrencyAmount convert to Pair<String, String> (formatted strings with price and with currency code)
 */
class LocalisedPriceCurrencyCodeStringMapper @Inject constructor() {

    /**
     * Invoke
     * Convert CurrencyAmount to Pair<String, String>
     * @param currencyAmount [CurrencyAmount]
     * @param locale [Locale]
     * @return Pair<String, String>
     */
    internal operator fun invoke(
        currencyAmount: CurrencyAmount,
        locale: Locale,
    ): Pair<String, String> {
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
        currencyFormatter.currency = Currency.getInstance(currencyAmount.currency.code)
        val currencyCode = currencyFormatter.currency?.currencyCode ?: "EUR"
        return Pair(currencyFormatter.format(currencyAmount.value), currencyCode)
    }
}