package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper for CurrencyAmount convert to String (formatted string with price and currency)
 */
class LocalisedPriceStringMapper @Inject constructor() {

    /**
     * Invoke
     * Convert CurrencyAmount to String
     * @param currencyAmount [CurrencyAmount]
     * @param locale [Locale]
     * @return String
     */
    internal operator fun invoke(
        currencyAmount: CurrencyAmount,
        locale: Locale,
    ): String {
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
        currencyFormatter.currency = Currency.getInstance(currencyAmount.currency.code)
        return currencyFormatter.format(currencyAmount.value)
    }
}