package mega.privacy.android.data.constant

/**
 * Sort Order Source
 */
sealed interface SortOrderSource {
    object Default : SortOrderSource

    @Deprecated("Part of old legacy screen")
    object OutgoingShares : SortOrderSource
    object OutgoingSharesSingleActivity : SortOrderSource
}