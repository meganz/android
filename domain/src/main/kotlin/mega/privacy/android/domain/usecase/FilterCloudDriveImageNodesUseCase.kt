package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Filter cloud drive image nodes
 *
 * @property photosRepository
 */
class FilterCloudDriveImageNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    suspend operator fun invoke(source: List<ImageNode>) =
        createTempSyncFolderIds().let { sync ->
            source.filter { it.parentId.longValue !in sync }
        }

    private suspend fun createTempSyncFolderIds() =
        listOfNotNull(
            photosRepository.getCameraUploadFolderId(),
            photosRepository.getMediaUploadFolderId()
        )

}