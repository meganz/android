package mega.privacy.android.data.constant

/**
 * Sort Order Source
 */
sealed interface SortOrderSource {
    object Default : SortOrderSource
    object OutgoingShares : SortOrderSource
}