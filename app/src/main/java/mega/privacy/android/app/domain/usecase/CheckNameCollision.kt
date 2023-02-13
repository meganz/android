package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Checks and returns name collisions
 */
fun interface CheckNameCollision {
    /**
     * Checks for collisions of nodeHandle node in the parentHandle node folder
     * @param nodeHandle the node of the handle we want to check
     * @param parentHandle the node of the destination folder
     * @param type the type of collision we want to detect
     */
    suspend operator fun invoke(
        nodeHandle: NodeId,
        parentHandle: NodeId,
        type: NameCollisionType,
    ): NameCollision
}