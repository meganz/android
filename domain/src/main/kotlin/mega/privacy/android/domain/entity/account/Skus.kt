package mega.privacy.android.domain.entity.account

object Skus {
    const val SKU_PRO_I_MONTH = "mega.android.pro1.onemonth"
    const val SKU_PRO_I_YEAR = "mega.android.pro1.oneyear"
    const val SKU_PRO_II_MONTH = "mega.android.pro2.onemonth"
    const val SKU_PRO_II_YEAR = "mega.android.pro2.oneyear"
    const val SKU_PRO_III_MONTH = "mega.android.pro3.onemonth"
    const val SKU_PRO_III_YEAR = "mega.android.pro3.oneyear"
    const val SKU_PRO_LITE_MONTH = "mega.android.prolite.onemonth"
    const val SKU_PRO_LITE_YEAR = "mega.android.prolite.oneyear"
    const val SKU_STARTER_MONTH = "mega.android.a11.onemonth"
    const val SKU_STARTER_YEAR = "mega.android.a11.oneyear"
    const val SKU_BASIC_MONTH = "mega.android.a12.onemonth"
    const val SKU_BASIC_YEAR = "mega.android.a12.oneyear"
    const val SKU_ESSENTIAL_MONTH = "mega.android.a13.onemonth"
    const val SKU_ESSENTIAL_YEAR = "mega.android.a13.oneyear"

    /**
     * Level value for a sku that is not an upgradeable consumer plan or is unknown/null.
     */
    const val NO_LEVEL = -1
}

/**
 * Tier ranking of a subscription sku, low (cheapest) to high, or [Skus.NO_LEVEL] when the sku is not
 * an upgradeable consumer plan (e.g. business) or is unknown/null. Used to order plans by tier.
 *
 * @return tier level from 0 (Starter) to 6 (Pro III), or [Skus.NO_LEVEL] for non-consumer/unknown skus.
 */
val String?.subscriptionSkuLevel: Int
    get() = when (this) {
        Skus.SKU_STARTER_MONTH, Skus.SKU_STARTER_YEAR -> 0
        Skus.SKU_BASIC_MONTH, Skus.SKU_BASIC_YEAR -> 1
        Skus.SKU_ESSENTIAL_MONTH, Skus.SKU_ESSENTIAL_YEAR -> 2
        Skus.SKU_PRO_LITE_MONTH, Skus.SKU_PRO_LITE_YEAR -> 3
        Skus.SKU_PRO_I_MONTH, Skus.SKU_PRO_I_YEAR -> 4
        Skus.SKU_PRO_II_MONTH, Skus.SKU_PRO_II_YEAR -> 5
        Skus.SKU_PRO_III_MONTH, Skus.SKU_PRO_III_YEAR -> 6
        else -> Skus.NO_LEVEL
    }
