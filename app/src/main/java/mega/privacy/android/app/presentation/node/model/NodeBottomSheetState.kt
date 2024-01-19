package mega.privacy.android.app.presentation.node.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.node.view.BottomSheetMenuItem
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node bottom sheet state
 *
 * @property name
 * @property isOnline
 * @property node
 * @property actions
 * @property error
 * @property nodeNameCollisionResult
 * @property moveRequestResult
 * @property deleteVersionsResult
 */
data class NodeBottomSheetState(
    val name: String = "",
    val isOnline: Boolean = false,
    val node: TypedNode? = null,
    val actions: List<BottomSheetMenuItem> = emptyList(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionResult: StateEventWithContent<NodeNameCollisionResult> = consumed(),
    val moveRequestResult: StateEventWithContent<Result<MoveRequestResult>> = consumed(),
    val deleteVersionsResult: StateEventWithContent<Throwable?> = consumed(),
)