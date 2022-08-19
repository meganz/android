package mega.privacy.android.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class containing all the required to present a product (PRO plan subscription) available.
 *
 * @property handle     Product handle
 * @property level      Product level (PRO I = 1, PRO II = 2, PRO III = 3, PRO LITE = 4, etc.)
 * @property months     Number of subscription months of the (1 for monthly or 12 for yearly)
 * @property storage    Amount of storage of the product
 * @property transfer   Amount of transfer quota of the product
 * @property amount     Amount or price of the product
 * @property currency   Currency of the product
 * @property isBusiness Flag to indicate if the product is business or not
 */
@Parcelize
data class Product(
    var handle: Long,
    var level: Int,
    var months: Int,
    var storage: Int,
    var transfer: Int,
    var amount: Int,
    var currency: String?,
    var isBusiness: Boolean,
) : Parcelable