package mega.privacy.android.app.presentation.node.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.node.view.BottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node bottom sheet state
 *
 * @property name
 * @property isOnline
 * @property node
 * @property actions
 * @property error
 */
data class NodeBottomSheetState(
    val name: String = "",
    val isOnline: Boolean = false,
    val node: TypedNode? = null,
    val actions: List<BottomSheetMenuItem> = emptyList(),
    val error: StateEventWithContent<Throwable> = consumed(),
)