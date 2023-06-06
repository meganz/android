package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get Photos from a folder (support recursive sub folder)
 */
class GetPhotosByFolderIdInFolderLinkUseCase @Inject constructor(
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
        return getMonitoredList(folderId, recursive).onStart {
            emit(
                getFolderPhotos(
                    folderId,
                    recursive
                )
            )
        }
    }

    private suspend fun getFolderPhotos(
        folderId: NodeId,
        recursive: Boolean,
    ): List<Photo> =
        photosRepository.getPhotosByFolderIdInFolderLink(folderId, recursive)


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getMonitoredList(folderId: NodeId, recursive: Boolean) =
        nodeRepository.monitorNodeUpdates()
            .mapLatest { getFolderPhotos(folderId, recursive) }
}
