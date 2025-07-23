package mega.privacy.android.core.nodecomponents.util

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node

internal fun Node.isOutShare() = if (this is FolderNode) {
    this.isPendingShare || this.isShared
} else {
    false
}