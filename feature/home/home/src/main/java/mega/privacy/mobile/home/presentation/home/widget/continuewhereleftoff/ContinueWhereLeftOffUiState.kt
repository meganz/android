package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.TypedFileNode

internal data class ContinueWhereLeftOffUiState(
    val items: List<ContinueWhereLeftOffItem> = emptyList(),
    val isLoading: Boolean = true,
    val openNodeEvent: StateEventWithContent<TypedFileNode> = consumed(),
    val isOfflineMode: Boolean = false,
) {
    /**
     * Whether the widget is loading. Items cannot arrive without a connection, so while offline
     * the carousel settles on its empty layout instead of a skeleton that never resolves.
     */
    val showsLoading = isLoading && !isOfflineMode
}
