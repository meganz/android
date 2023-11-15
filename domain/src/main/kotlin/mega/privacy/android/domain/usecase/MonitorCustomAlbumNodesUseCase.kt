package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get custom album nodes use case
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorCustomAlbumNodesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
) {
    @Volatile
    private var elements: List<AlbumPhotoId> = listOf()

    operator fun invoke(albumId: AlbumId): Flow<List<ImageNode>> = flow {
        emit(getAlbumNodes(albumId))
        emitAll(monitorAlbumNodes(albumId))
    }

    private suspend fun getAlbumNodes(albumId: AlbumId): List<ImageNode> =
        albumRepository.getAlbumElementIDs(albumId, refresh = false).let {
            elements = it
            refreshNodes()
        }

    private fun monitorAlbumNodes(albumId: AlbumId): Flow<List<ImageNode>> = merge(
        albumRepository.monitorAlbumElementIds(albumId)
            .mapLatest { getAlbumNodes(albumId) },
        photosRepository.monitorImageNodes()
            .mapLatest { refreshNodes() },
    )

    private suspend fun refreshNodes(): List<ImageNode> =
        elements.mapNotNull { photosRepository.getImageNode(it.nodeId) }
}
