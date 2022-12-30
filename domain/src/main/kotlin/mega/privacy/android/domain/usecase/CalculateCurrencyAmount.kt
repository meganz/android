package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint

/**
 * Calculate currency amount
 */
interface CalculateCurrencyAmount {
    /**
     * Invoke
     *
     * @return [CurrencyAmount]
     */
    operator fun invoke(currencyPoint: CurrencyPoint, currency: Currency): CurrencyAmount
}
