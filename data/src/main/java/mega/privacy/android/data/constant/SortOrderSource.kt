package mega.privacy.android.data.constant

/**
 * Sort Order Source
 */
sealed interface SortOrderSource {
    object Default : SortOrderSource

    object OutgoingShares : SortOrderSource

    @Deprecated(
        message = "Synonym for OutgoingShares; remove after all callers migrate.",
        replaceWith = ReplaceWith("OutgoingShares"),
    )
    object OutgoingSharesSingleActivity : SortOrderSource
}
