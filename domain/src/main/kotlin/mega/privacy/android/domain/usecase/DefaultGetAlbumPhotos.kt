package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get album photos use case implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAlbumPhotos @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetAlbumPhotos {
    override fun invoke(albumId: AlbumId): Flow<List<Photo>> = flow {
        emit(getAlbumPhotos(albumId))
        emitAll(monitorAlbumPhotosUpdate(albumId))
    }.flowOn(defaultDispatcher)

    private suspend fun getAlbumPhotos(albumId: AlbumId): List<Photo> =
        albumRepository.getAlbumElementIDs(albumId)
            .mapNotNull { nodeId -> photosRepository.getPhotoFromNodeID(nodeId) }

    private fun monitorAlbumPhotosUpdate(albumId: AlbumId): Flow<List<Photo>> =
        albumRepository.monitorAlbumElementIds(albumId)
            .filter(List<NodeId>::isNotEmpty)
            .mapLatest { getAlbumPhotos(albumId) }
}
