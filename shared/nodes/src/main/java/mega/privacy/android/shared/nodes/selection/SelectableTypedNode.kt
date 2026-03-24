package mega.privacy.android.shared.nodes.selection

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.Selectable
import mega.privacy.android.shared.nodes.model.TypedNodeItem

internal interface SelectableTypedNode<T : TypedNode> : TypedNodeItem<T>, Selectable