package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint

/**
 * Default implementation of [CalculateCurrencyAmount]
 */
class DefaultCalculateCurrencyAmount : CalculateCurrencyAmount {
    override fun invoke(currencyPoint: CurrencyPoint, currency: Currency) = when (currencyPoint) {
        is CurrencyPoint.LocalCurrencyPoint -> CurrencyAmount((currencyPoint.value / 1000000.00).toFloat(),
            currency)
        is CurrencyPoint.SystemCurrencyPoint -> CurrencyAmount((currencyPoint.value / 100.00).toFloat(),
            currency)
    }
}