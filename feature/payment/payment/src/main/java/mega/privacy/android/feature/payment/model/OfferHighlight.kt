package mega.privacy.android.feature.payment.model

/**
 * How active discount offer(s) are highlighted on the upgrade screen. The screen dispatches on this
 * exhaustively (see `UpgradeAccountState.offerHighlight`).
 */
sealed interface OfferHighlight {
    /** No discount offer; render the standard content. */
    data object None : OfferHighlight

    /** A single discounted plan, featured on top and excluded from the list (Figma 9925-20171). */
    data class Single(val subscription: LocalisedSubscription) : OfferHighlight

    /**
     * Multiple discounted plans, all rendered inline with a shared header (Figma 10286-9598).
     * [subscriptions] are the plans carrying the campaign in either period.
     */
    data class Multiple(val subscriptions: List<LocalisedSubscription>) : OfferHighlight
}
