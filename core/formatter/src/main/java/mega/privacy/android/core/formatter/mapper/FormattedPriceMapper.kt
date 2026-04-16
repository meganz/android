package mega.privacy.android.core.formatter.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper for CurrencyAmount to locale-formatted price string
 */
class FormattedPriceMapper @Inject constructor() {
    /**
     * Invoke
     * Convert CurrencyAmount to a locale-formatted price string
     * @param currencyAmount [CurrencyAmount]
     * @param locale [Locale]
     * @return formatted price string
     */
    operator fun invoke(
        currencyAmount: CurrencyAmount,
        locale: Locale = Locale.getDefault(),
    ): String {
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
        currencyFormatter.currency = Currency.getInstance(currencyAmount.currency.code)
        return currencyFormatter.format(currencyAmount.value)
    }
}
