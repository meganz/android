package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default search videos from cloud drive
 *
 * @property photosRepository
 */
class DefaultFilterCloudDrivePhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
) : FilterCloudDrivePhotos {

    override suspend fun invoke(source: List<Photo>) =
        createTempSyncFolderIds().let { sync ->
            source.filter { it.parentId !in sync }
        }

    private suspend fun createTempSyncFolderIds() =
        listOfNotNull(photosRepository.getCameraUploadFolderId(),
            photosRepository.getMediaUploadFolderId())

}