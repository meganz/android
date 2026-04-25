package mega.privacy.mobile.home.presentation.continuewhereleftoff

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.TypedFileNode

internal data class ContinueWhereLeftOffListUiState(
    val items: List<ContinueWhereLeftOffItem> = emptyList(),
    val isLoading: Boolean = true,
    val openNodeEvent: StateEventWithContent<TypedFileNode> = consumed(),
)
