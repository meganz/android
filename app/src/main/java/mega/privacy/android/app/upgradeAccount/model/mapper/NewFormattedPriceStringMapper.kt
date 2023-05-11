package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Mapper for CurrencyAmount convert to String (formatted string with price and currency)
 */
typealias NewFormattedPriceStringMapper = (@JvmSuppressWildcards CurrencyAmount) -> @JvmSuppressWildcards Pair<String, String>

/**
 * Convert CurrencyAmount to Pair<String, String>
 * @param currencyAmount [CurrencyAmount]
 * @return Pair<String, String>
 */
internal fun toNewFormattedPriceString(
    currencyAmount: CurrencyAmount,
    locale: Locale = Locale.getDefault(),
): Pair<String, String> {
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = Currency.getInstance(currencyAmount.currency.code)
    val currencyCode = format.currency?.currencyCode ?: "EUR"
    return Pair(format.format(currencyAmount.value), currencyCode)
}