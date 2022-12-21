package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
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
            .map { mapToUserAlbum(it) }

        emit(userAlbums)
        emitAll(monitorUpdates())
    }.flowOn(defaultDispatcher)

    private fun monitorUpdates(): Flow<List<Album.UserAlbum>> =
        albumRepository.monitorUserSetsUpdate()
            .map { sets ->
                sets.map { mapToUserAlbum(it) }
            }

    private suspend fun mapToUserAlbum(set: UserSet): Album.UserAlbum {
        val photo = set.cover?.let { photosRepository.getPhotoFromNodeID(NodeId(it)) }
        return Album.UserAlbum(
            id = AlbumId(set.id),
            title = set.name,
            cover = photo,
            modificationTime = set.modificationTime,
        )
    }
}
