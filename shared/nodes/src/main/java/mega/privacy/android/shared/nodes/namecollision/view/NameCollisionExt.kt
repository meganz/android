package mega.privacy.android.shared.nodes.namecollision.view

import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.shared.nodes.namecollision.NameCollisionType

/**
 * The action that raised this collision: a local upload, or a copy or move of an existing node.
 */
internal fun NameCollision.toNameCollisionType(): NameCollisionType = when (this) {
    is FileNameCollision -> NameCollisionType.UPLOAD
    is NodeNameCollision.Default ->
        if (type == NodeNameCollisionType.MOVE) NameCollisionType.MOVE else NameCollisionType.COPY

    is NodeNameCollision.Chat, is NodeNameCollision.FileLink -> NameCollisionType.COPY
}
