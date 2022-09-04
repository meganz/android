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

    /**
     * Workaround for caching db read
     */
    private var tempCameraUploadFolderId: Long? = null
    private var tempMediaUploadFolderId: Long? = null

    override suspend fun invoke(source: List<Photo>): List<Photo> = filterCameraUploadPhotos(source)

    private suspend fun filterCameraUploadPhotos(source: List<Photo>): List<Photo> {
        createTempSyncFolderIds()
        val filterList = source.filter {
            !inSyncFolder(it.parentId)
        }
        resetTempSyncFolderIds()
        return filterList
    }

    private fun resetTempSyncFolderIds() {
        tempCameraUploadFolderId = null
        tempMediaUploadFolderId = null
    }

    private suspend fun createTempSyncFolderIds() {
        if (tempCameraUploadFolderId == null)
            tempCameraUploadFolderId = photosRepository.getCameraUploadFolderId()
        if (tempMediaUploadFolderId == null)
            tempMediaUploadFolderId = photosRepository.getMediaUploadFolderId()
    }

    private suspend fun inSyncFolder(parentId: Long): Boolean =
        parentId == tempCameraUploadFolderId || parentId == tempMediaUploadFolderId

}