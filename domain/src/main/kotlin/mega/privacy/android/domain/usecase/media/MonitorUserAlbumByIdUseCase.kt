package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

class MonitorUserAlbumByIdUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val getAlbumCoverPhotoUseCase: GetUserAlbumCoverPhotoUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(albumId: AlbumId): Flow<MediaAlbum.User?> =
        albumRepository.monitorUserSetsUpdate()
            .mapNotNull { sets -> sets.find { it.id == albumId.id } }
            .mapLatest {
                albumRepository.clearAlbumCache(albumId)
                getUserAlbum(albumId, true)
            }
            .onStart {
                // emit whatever is stored in the cache
                emit(getUserAlbum(albumId, false))
            }

    private suspend fun getUserAlbum(albumId: AlbumId, refresh: Boolean): MediaAlbum.User? =
        albumRepository.getUserSet(albumId)?.let { set ->
            MediaAlbum.User(
                id = AlbumId(set.id),
                title = set.name,
                cover = getAlbumCoverPhotoUseCase(albumId, refresh),
                creationTime = set.creationTime,
                modificationTime = set.modificationTime,
                isExported = set.isExported,
            )
        }
}