package mega.privacy.android.domain.entity.account

/**
 * Represents the period of a subscription offer
 */
sealed class OfferPeriod {
    /**
     * Offer period in months
     * @property value Number of months
     */
    data class Month(val value: Int) : OfferPeriod()

    /**
     * Offer period in years
     * @property value Number of years
     */
    data class Year(val value: Int) : OfferPeriod()
}
