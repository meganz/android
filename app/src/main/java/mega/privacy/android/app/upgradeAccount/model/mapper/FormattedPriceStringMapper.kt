package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency

/**
 * Mapper for CurrencyAmount convert to String (formatted string with price and currency)
 */
typealias FormattedPriceStringMapper = (@JvmSuppressWildcards CurrencyAmount) -> @JvmSuppressWildcards String

/**
 * Convert CurrencyAmount to String
 * @param currencyAmount [CurrencyAmount]
 * @return String
 */
internal fun toFormattedPriceString(currencyAmount: CurrencyAmount): String {
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyAmount.currency.currency)
    return format.format(currencyAmount.value)
}