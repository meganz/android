package mega.privacy.android.feature.payment.model

/**
 * LocalisedProductPrice model to share correctly localised strings for price and for currency code
 *
 * @property price [String]  String for properly localised price for subscriptions
 * @property currencyCode [String]   String for local currency code
 */
data class LocalisedProductPrice(
    val price: String,
    val currencyCode: String,
)