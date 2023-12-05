package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get Photos from a folder (support recursive sub folder)
 */
class GetPhotosByFolderIdUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @param recursive
     *
     * @return photo
     */
    operator fun invoke(
        folderId: NodeId,
        recursive: Boolean,
    ): Flow<List<Photo>> {
        return flow { emitAll(getMonitoredList(folderId, recursive)) }
            .onStart { emit(getFolderPhotos(folderId, recursive)) }
            .cancellable()
    }

    private suspend fun getFolderPhotos(
        folderId: NodeId,
        recursive: Boolean,
    ): List<Photo> =
        photosRepository.getPhotosByFolderId(
            folderId = folderId,
            recursive = recursive
        )


    private fun getMonitoredList(folderId: NodeId, recursive: Boolean) =
        nodeRepository.monitorNodeUpdates()
            .conflate()
            .map { getFolderPhotos(folderId, recursive) }
            .cancellable()
}
