package mega.privacy.android.feature.sync.domain.entity.progress

/** Which sync transfer rows a list shows: a desktop tab, a media type or a file name search. */
internal sealed interface SyncTransferFilter {

    /** Every pending row, both directions mixed by priority. */
    data object Pending : SyncTransferFilter

    /** The pending rows of one [direction]. */
    data class Direction(val direction: SyncTransferDirection) : SyncTransferFilter

    /** Every completed row, newest first. */
    data object Completed : SyncTransferFilter

    /** The pending and completed rows of one media [type]. */
    data class Media(val type: SyncMediaType) : SyncTransferFilter

    /**
     * The pending and completed rows whose file name contains [text], ignoring case, narrowed to
     * one [direction] when it is set.
     */
    data class Search(
        val text: String,
        val direction: SyncTransferDirection? = null,
    ) : SyncTransferFilter
}
