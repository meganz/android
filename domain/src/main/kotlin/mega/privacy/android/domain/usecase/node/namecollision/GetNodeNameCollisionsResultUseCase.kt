package mega.privacy.android.domain.usecase.node.namecollision

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import javax.inject.Inject

/**
 *  Use Case to copy list of collided nodes
 */
class GetNodeNameCollisionsResultUseCase @Inject constructor(
    private val getNodeNameCollisionResultUseCase: GetNodeNameCollisionResultUseCase,
) {
    /**
     * Invoke
     *
     * @param nameCollisions The list of NodeNameCollision
     * @return list of [NodeNameCollisionResult]
     */
    suspend operator fun invoke(
        nameCollisions: List<NameCollision>,
    ): List<NodeNameCollisionResult> = coroutineScope {
        val semaphore = Semaphore(10)
        nameCollisions.map { nameCollision ->
            async {
                semaphore.withPermit {
                    getNodeNameCollisionResultUseCase(
                        nameCollision = nameCollision,
                    )
                }
            }
        }
    }.awaitAll()
}