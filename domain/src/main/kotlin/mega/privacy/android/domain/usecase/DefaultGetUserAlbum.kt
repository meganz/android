package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get user album use case implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetUserAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetUserAlbum {
    override fun invoke(albumId: AlbumId): Flow<Album.UserAlbum?> = flow {
        emit(getUserAlbum(albumId))
        emitAll(monitorUserAlbumUpdate(albumId))
    }.flowOn(defaultDispatcher)

    private suspend fun getUserAlbum(albumId: AlbumId): Album.UserAlbum? =
        albumRepository.getUserSet(albumId)?.let { set ->
            val photo = set.cover?.let { eid ->
                albumRepository.getAlbumElementIDs(albumId = AlbumId(set.id))
                    .find { it.id == eid }
                    ?.run { photosRepository.getPhotoFromNodeID(nodeId, this) }
            }
            Album.UserAlbum(
                id = AlbumId(set.id),
                title = set.name,
                cover = photo,
                creationTime = set.creationTime,
                modificationTime = set.modificationTime,
                isExported = set.isExported,
            )
        }

    private fun monitorUserAlbumUpdate(albumId: AlbumId): Flow<Album.UserAlbum?> =
        albumRepository.monitorUserSetsUpdate()
            .mapNotNull { sets -> sets.find { it.id == albumId.id } }
            .mapLatest {
                albumRepository.clearAlbumCache(albumId)
                getUserAlbum(albumId)
            }
}
