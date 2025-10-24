package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case for monitoring user-created media albums.
 *
 * This use case continuously emits updates to the list of [MediaAlbum.User] whenever there is a change
 * in the user's album sets. It retrieves user albums from the [AlbumRepository] and represents them
 * as [MediaAlbum.User] instances with cover photos.
 *
 * @property albumRepository Repository providing access to user-created album data.
 * @property photosRepository Repository providing access to photo metadata.
 * @property defaultDispatcher Coroutine dispatcher used for background operations.
 *
 * @return A [Flow] emitting the list of user [MediaAlbum.User] whenever updates occur.
 */
class MonitorMediaAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<MediaAlbum>> =
        albumRepository
            .monitorUserSetsUpdate()
            .mapLatest { getUserAlbums(it) }
            .onStart {
                val userAlbums = applicationScope.async {
                    getUserAlbums(albumRepository.getAllUserSets())
                }
                emit(userAlbums.await())
            }

    private suspend fun getUserAlbums(users: List<UserSet>): List<MediaAlbum.User> =
        withContext(defaultDispatcher) {
            users.map { set ->
                val cover = set.cover?.let { eid ->
                    albumRepository
                        .getAlbumElementIDs(albumId = AlbumId(set.id))
                        .find { it.id == eid }
                        ?.run {
                            photosRepository.getPhotoFromNodeID(
                                nodeId = nodeId,
                                albumPhotoId = this,
                                refresh = users.any { it.id == set.id },
                            )
                        }
                }
                MediaAlbum.User(
                    id = AlbumId(set.id),
                    title = set.name,
                    cover = cover,
                    creationTime = set.creationTime,
                    modificationTime = set.modificationTime,
                    isExported = set.isExported,
                )
            }
        }
}
