package mega.privacy.android.domain.usecase.node.namecollision

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import javax.inject.Inject

/**
 *  Use Case to update node collisions result based on rename names
 */
class UpdateNodeNameCollisionsResultUseCase @Inject constructor(
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
) {

    /**
     * Invoke
     *
     * @param nameCollisionResults The list of NodeNameCollision
     * @param renameNames       List of already applied rename names.
     * @param applyOnNext       True if the choice will be applied for the rest of files, false otherwise.
     *
     * @return [NodeCollisionsWithRenameNames]
     */
    suspend operator fun invoke(
        nameCollisionResults: List<NodeNameCollisionResult>,
        renameNames: List<String>,
        applyOnNext: Boolean,
    ): NodeCollisionsWithRenameNames = coroutineScope {
        val mutex = Mutex()
        val semaphore = Semaphore(10)
        val renameNamesSet = renameNames.toMutableSet()
        nameCollisionResults
            .map { collision ->
                async {
                    val nameCollision = collision.nameCollision
                    if (!nameCollision.isFile || collision.renameName == null)
                        return@async collision
                    semaphore.withPermit {
                        var newRenameName =
                            getNodeNameCollisionRenameNameUseCase(collision.nameCollision)
                        if (renameNamesSet.contains(newRenameName)) {
                            newRenameName = generateSequence(newRenameName) {
                                it.getPossibleRenameName()
                            }.first { !renameNamesSet.contains(it) }
                        }
                        if (applyOnNext) {
                            mutex.withLock {
                                renameNamesSet.add(newRenameName)
                            }
                        }
                        collision.copy(
                            nameCollision = when (nameCollision) {
                                is NodeNameCollision.Default -> nameCollision.copy(renameName = newRenameName)

                                is NodeNameCollision.Chat -> nameCollision.copy(renameName = newRenameName)

                                is FileNameCollision -> nameCollision
                            },
                            renameName = newRenameName
                        )
                    }
                }
            }
            .awaitAll() to renameNamesSet.toList()
    }
}


/**
 * NodeCollisionsWithSize
 * List of [NodeNameCollisionResult], list of rename names
 */
typealias NodeCollisionsWithRenameNames = Pair<List<NodeNameCollisionResult>, List<String>>