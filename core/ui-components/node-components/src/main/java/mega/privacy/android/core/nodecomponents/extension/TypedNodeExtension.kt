package mega.privacy.android.core.nodecomponents.extension

import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Check if the node is not an S4 container
 */
internal fun TypedNode.isNotS4Container(): Boolean =
    (this as? TypedFolderNode)?.isS4Container != true

