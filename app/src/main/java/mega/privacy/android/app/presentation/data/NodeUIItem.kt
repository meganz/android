package mega.privacy.android.app.presentation.data

import mega.privacy.android.domain.entity.node.Node

class NodeUIItem(
    val node: Node,
    var isSelected: Boolean
) : Node by node