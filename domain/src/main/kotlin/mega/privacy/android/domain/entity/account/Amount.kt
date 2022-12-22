package mega.privacy.android.domain.entity.account

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
//In the use case -> convert to CurrencyAmount
@JvmInline
value class CurrencyAmount(val value: Float)