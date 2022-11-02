package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get album photos use case implementation.
 */
class DefaultGetAlbumPhotos @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetAlbumPhotos {
    override suspend fun invoke(albumId: AlbumId): Flow<List<Photo>> = flow {
        val photos = albumRepository.getAlbumElementIDs(albumId)
            .mapNotNull { nodeId -> photosRepository.getPhotoFromNodeID(nodeId) }
        emit(photos)
    }.flowOn(defaultDispatcher)
}
