package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.TypedFileNode

internal data class ContinueWhereLeftOffUiState(
    val items: List<ContinueWhereLeftOffItem> = emptyList(),
    val openNodeEvent: StateEventWithContent<TypedFileNode> = consumed(),
)
