package mega.privacy.android.core.nodecomponents.action

import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Input to [NodeOptionsActionViewModel.processIntent].
 */
sealed interface NodeOptionsActionIntent {

    /**
     * The selected nodes changed; recompute the actions available for them.
     *
     * @property nodes the currently selected nodes
     * @property nodeSourceType the source the nodes belong to
     */
    data class SelectedNodesChanged(
        val nodes: Set<TypedNode>,
        val nodeSourceType: NodeSourceType,
    ) : NodeOptionsActionIntent

    /** The action-triggered event has been handled. */
    data object ActionTriggeredConsumed : NodeOptionsActionIntent

    /** The dismiss event has been handled. */
    data object DismissConsumed : NodeOptionsActionIntent
}
