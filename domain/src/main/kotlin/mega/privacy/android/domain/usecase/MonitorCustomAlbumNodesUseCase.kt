package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get custom album nodes use case
 */
class MonitorCustomAlbumNodesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(albumId: AlbumId): Flow<List<ImageNode>> = flow {
        emit(getAlbumNodes(albumId))
        emitAll(monitorAlbumNodes(albumId))
    }.flowOn(defaultDispatcher)

    private suspend fun getAlbumNodes(albumId: AlbumId): List<ImageNode> =
        albumRepository.getAlbumElementIDs(albumId, refresh = false)
            .mapNotNull { photosRepository.getImageNode(it.nodeId) }

    private fun monitorAlbumNodes(albumId: AlbumId): Flow<List<ImageNode>> =
        albumRepository.monitorAlbumElementIds(albumId)
            .mapLatest { getAlbumNodes(albumId) }
}
