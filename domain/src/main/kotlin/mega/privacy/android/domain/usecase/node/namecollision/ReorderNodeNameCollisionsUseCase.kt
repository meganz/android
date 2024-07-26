package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.NameCollision
import javax.inject.Inject

/**
 *  Reorders a list of [NameCollision] for presenting first files, then folders.
 */
class ReorderNodeNameCollisionsUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param collisions List of [NameCollision]
     */
    operator fun invoke(
        collisions: List<NameCollision>,
    ): NodeCollisionsWithSize {
        val sortedList = collisions.sortedBy {
            !it.isFile
        }
        val fileCollisions = sortedList.count { it.isFile }
        val pendingFileCollision = fileCollisions.let {
            if (it > 0) it - 1 else 0
        }
        val pendingFolderCollisions = (sortedList.size - fileCollisions).let {
            if (it > 0) it - 1 else 0
        }
        return NodeCollisionsWithSize(
            sortedList,
            pendingFileCollision,
            pendingFolderCollisions
        )
    }
}

/**
 * NodeCollisionsWithSize
 * List of node [NameCollision], pending file collisions [count], pending folder collisions [count]
 */
typealias NodeCollisionsWithSize = Triple<List<NameCollision>, Int, Int>