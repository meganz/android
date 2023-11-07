package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to monitor timeline nodes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorMediaDiscoveryNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(parentID: Long, recursive: Boolean): Flow<List<ImageNode>> {
        return flow {
            emit(getMediaDiscoveryNodes(parentID, recursive))
            emitAll(monitorMediaDiscoveryNodes(parentID, recursive))
        }.flowOn(defaultDispatcher)
    }

    private suspend fun getMediaDiscoveryNodes(parentID: Long, recursive: Boolean) =
        photosRepository.getMediaDiscoveryNodes(
            parentID = parentID,
            recursive = recursive
        )

    private fun monitorMediaDiscoveryNodes(
        parentID: Long,
        recursive: Boolean
    ): Flow<List<ImageNode>> {
        return nodeRepository.monitorNodeUpdates()
            .mapLatest {
                getMediaDiscoveryNodes(parentID, recursive)
            }
    }
}
