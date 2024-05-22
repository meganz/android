package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node with a flag to indicate if it's disabled
 * @property node The node
 * @property isDisabled Flag to indicate if the node is disabled
 */
internal data class TypedNodeUiModel(
    val node: TypedNode,
    val isDisabled: Boolean = false,
)