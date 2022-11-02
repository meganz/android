package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get user albums use case implementation.
 */
class DefaultGetUserAlbums @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetUserAlbums {
    override suspend fun invoke(): Flow<List<Album.UserAlbum>> = flow {
        val userAlbums = albumRepository.getAllUserSets()
            .map { set ->
                val photo = set.cover?.let { photosRepository.getPhotoFromNodeID(NodeId(it)) }
                Album.UserAlbum(
                    id = AlbumId(set.id),
                    title = set.name,
                    cover = photo,
                )
            }
        emit(userAlbums)
    }.flowOn(defaultDispatcher)
}
