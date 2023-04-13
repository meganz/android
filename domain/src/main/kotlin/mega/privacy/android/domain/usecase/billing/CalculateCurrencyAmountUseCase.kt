package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint
import javax.inject.Inject

/**
 * Calculate currency amount
 */
class CalculateCurrencyAmountUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @return [CurrencyAmount]
     */
    operator fun invoke(currencyPoint: CurrencyPoint, currency: Currency) = when (currencyPoint) {
        is CurrencyPoint.LocalCurrencyPoint -> CurrencyAmount(
            (currencyPoint.value / 1000000.00).toFloat(),
            currency
        )
        is CurrencyPoint.SystemCurrencyPoint -> CurrencyAmount(
            (currencyPoint.value / 100.00).toFloat(),
            currency
        )
    }
}