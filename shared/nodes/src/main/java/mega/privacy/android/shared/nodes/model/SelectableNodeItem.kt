package mega.privacy.android.shared.nodes.model

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.selection.SelectableTypedNode

data class SelectableNodeItem<T : TypedNode>(
    val nodeItem: TypedNodeItem<T>,
    override val isSelected: Boolean,
) : TypedNodeItem<T> by nodeItem, SelectableTypedNode<T>
