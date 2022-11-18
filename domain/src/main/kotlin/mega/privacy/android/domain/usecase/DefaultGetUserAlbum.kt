package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get user album use case implementation
 */
class DefaultGetUserAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetUserAlbum {
    override fun invoke(albumId: AlbumId): Flow<Album.UserAlbum?> = flow {
        val userAlbum = albumRepository.getUserSet(albumId)?.let { mapToUserAlbum(it) }

        emit(userAlbum)
        emitAll(monitorUpdate(albumId))
    }.flowOn(defaultDispatcher)

    private fun monitorUpdate(albumId: AlbumId): Flow<Album.UserAlbum> =
        albumRepository.monitorUserSetsUpdate()
            .mapNotNull { userSets ->
                userSets.firstOrNull { it.id == albumId.id }
            }.map(::mapToUserAlbum)

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
