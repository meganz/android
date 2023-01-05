package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Mapper for CurrencyAmount convert to String (formatted string with price and currency)
 */
typealias FormattedPriceStringMapper = (@JvmSuppressWildcards CurrencyAmount) -> @JvmSuppressWildcards String

/**
 * Convert CurrencyAmount to String
 * @param currencyAmount [CurrencyAmount]
 * @return String
 */
internal fun toFormattedPriceString(
    currencyAmount: CurrencyAmount,
    locale: Locale = Locale.getDefault(),
): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = Currency.getInstance(currencyAmount.currency.currency)
    return format.format(currencyAmount.value)
}