package mega.privacy.android.feature.payment.model

/**
 * How the active discount offer(s) should be highlighted on the upgrade screen for the currently
 * selected billing period. Computed from [UpgradeAccountState] (see `UpgradeAccountState.offerHighlight`).
 *
 * This is the single extension point for the discount-display designs: the screen
 * dispatches on this type exhaustively, so adding the multiple-offer layout is a localized change —
 * split the [Multiple] branch and the compiler flags every site that must handle it.
 */
sealed interface OfferHighlight {
    /** No purchasable discount offer for the selected period; render the standard content. */
    data object None : OfferHighlight

    /**
     * Exactly one discounted plan — shown as a featured [LocalisedSubscription] with a promotional
     * header (Figma 9925-20171). This is the case implemented today.
     */
    data class Single(val subscription: LocalisedSubscription) : OfferHighlight

    /**
     * More than one discounted plan (Figma 10286-9598). Not yet implemented; currently falls back to
     * the standard revamp layout. When the dedicated layout is built, handle this branch instead.
     */
    data class Multiple(val subscriptions: List<LocalisedSubscription>) : OfferHighlight
}
