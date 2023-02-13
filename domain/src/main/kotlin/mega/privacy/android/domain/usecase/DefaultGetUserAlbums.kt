package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get user albums use case implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetUserAlbums @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetUserAlbums {
    override fun invoke(): Flow<List<Album.UserAlbum>> = flow {
        emit(getUserAlbums())
        emitAll(monitorUserAlbumsUpdate())
    }.flowOn(defaultDispatcher)

    private suspend fun getUserAlbums(): List<Album.UserAlbum> =
        albumRepository.getAllUserSets()
            .map { set ->
                val photo = set.cover?.let { eid ->
                    if (eid == -1L) null
                    else albumRepository.getAlbumElementIDs(albumId = AlbumId(set.id))
                        .find { it.id == eid }
                        ?.let { photosRepository.getPhotoFromNodeID(it.nodeId, it) }
                }
                Album.UserAlbum(
                    id = AlbumId(set.id),
                    title = set.name,
                    cover = photo,
                    modificationTime = set.modificationTime,
                )
            }

    private fun monitorUserAlbumsUpdate(): Flow<List<Album.UserAlbum>> =
        albumRepository.monitorUserSetsUpdate()
            .mapLatest { getUserAlbums() }
}
