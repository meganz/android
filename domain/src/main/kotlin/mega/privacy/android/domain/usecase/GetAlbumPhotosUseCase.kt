package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get album photos use case
 */
class GetAlbumPhotosUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(albumId: AlbumId, refreshElements: Boolean): Flow<List<Photo>> = flow {
        emit(getAlbumPhotos(albumId, refreshElements, toInvalidatePhotos = listOf()))
        emitAll(monitorAlbumPhotosUpdate(albumId))
    }.flowOn(defaultDispatcher)

    private suspend fun getAlbumPhotos(
        albumId: AlbumId,
        refreshElements: Boolean,
        toInvalidatePhotos: List<AlbumPhotoId>,
    ): List<Photo> =
        albumRepository.getAlbumElementIDs(albumId, refreshElements)
            .mapNotNull { albumPhotoId ->
                photosRepository.getPhotoFromNodeID(
                    nodeId = albumPhotoId.nodeId,
                    albumPhotoId = albumPhotoId,
                    refresh = toInvalidatePhotos.let { list ->
                        list.any { it.nodeId == albumPhotoId.nodeId || it.nodeId.longValue == -1L }
                    },
                )
            }

    private fun monitorAlbumPhotosUpdate(albumId: AlbumId): Flow<List<Photo>> =
        albumRepository.monitorAlbumElementIds(albumId)
            .filter(List<AlbumPhotoId>::isNotEmpty)
            .mapLatest { getAlbumPhotos(albumId, refreshElements = false, toInvalidatePhotos = it) }
}
