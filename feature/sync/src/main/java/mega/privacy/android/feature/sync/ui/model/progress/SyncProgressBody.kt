package mega.privacy.android.feature.sync.ui.model.progress

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

/** What a pending tab shows; "no information yet" is always [Loading]. */
@Stable
internal sealed interface SyncProgressBody {

    /** The status, the store or the root node is not known yet. */
    data object Loading : SyncProgressBody

    /** The pending rows in the order of the projection, with an optional trailing footer. */
    data class Transfers(val items: ImmutableList<SyncActiveListItem>) : SyncProgressBody
}

/** What the Completed tab shows; "no information yet" is always [Loading]. */
@Stable
internal sealed interface SyncProgressCompletedBody {

    /** The store or the root node is not known yet. */
    data object Loading : SyncProgressCompletedBody

    /** The store is known and holds no completed row. */
    data object Empty : SyncProgressCompletedBody

    /** The completed rows, newest first. */
    data class Transfers(val items: ImmutableList<SyncCompletedTransferUiModel>) :
        SyncProgressCompletedBody
}
