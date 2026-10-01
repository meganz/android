package mega.privacy.android.domain.entity.account

import mega.privacy.android.domain.entity.Currency

/**
 * interface to represent pricing amount
 *
 * @property value value of pricing amount
 * @property SystemCurrencyPoint data class, which represents the pricing amount from API
 * @property LocalCurrencyPoint data class, which represents the pricing amount from Google Play store
 */
sealed interface CurrencyPoint {
    val value: Long

    data class SystemCurrencyPoint(override val value: Long) : CurrencyPoint
    data class LocalCurrencyPoint(override val value: Long) : CurrencyPoint
}

/**
 * value class to show either System pricing or Local Store pricing
 */

data class CurrencyAmount(val value: Float, val currency: Currency)