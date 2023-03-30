package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get Photos from a folder
 */
class GetPhotosByFolderIdUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @return photo
     */
    operator fun invoke(folderId: Long, order: SortOrder): Flow<List<Photo>> {
        return flow {
            emit(getChildren(folderId, order))
            emitAll(getMonitoredList(folderId, order))
        }
    }

    private suspend fun getChildren(folderId: Long, order: SortOrder): List<Photo> =
        photosRepository.getPhotosByFolderId(folderId, order)


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getMonitoredList(folderId: Long, order: SortOrder) =
        nodeRepository.monitorNodeUpdates()
            .mapLatest { getChildren(folderId, order) }
}
