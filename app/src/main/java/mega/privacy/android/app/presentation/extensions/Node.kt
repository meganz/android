package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node

/**
 * Check if node is out share
 *
 * @return True if node is out share, false otherwise
 */
internal fun Node.isOutShare() = if (this is FolderNode) {
    this.isPendingShare || this.isShared
} else {
    false
}