package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.mapper.CameraUploadsRecordMapper
import javax.inject.Inject

/**
 * Retrieve a list of [CameraUploadsMedia] from the media store
 */
class RetrieveMediaFromMediaStoreUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val cameraUploadsRecordMapper: CameraUploadsRecordMapper,
) {

    /**
     * Retrieve a list of [CameraUploadsMedia] from the media store
     *
     * @param parentPath used for filtering the media contained in the parent path
     * @param types types of files that we want to retrieve. This types will be converted to proper Uri
     *
     * @return a list of [CameraUploadsRecord]
     */
    suspend operator fun invoke(
        parentPath: String,
        types: List<MediaStoreFileType>,
        folderType: CameraUploadFolderType,
        fileType: CameraUploadsRecordType,
        tempRoot: String,
    ): List<CameraUploadsRecord> = coroutineScope {
        val selectionQuery = cameraUploadRepository.getMediaSelectionQuery(parentPath)

        val (recordsInPrimaryFolder, recordsInSecondaryFolder) =
            cameraUploadRepository.getAllCameraUploadsRecords()
                .partition { it.folderType == CameraUploadFolderType.Primary }

        return@coroutineScope types.flatMap {
            cameraUploadRepository.getMediaList(
                mediaStoreFileType = it,
                selectionQuery = selectionQuery,
            ).map {
                async {
                    yield()
                    runCatching {
                        val exists = checkCameraUploadsRecordAlreadyExists(
                            cameraUploadsMedia = it,
                            recordsToCheck =
                            if (folderType == CameraUploadFolderType.Primary)
                                recordsInPrimaryFolder
                            else recordsInSecondaryFolder,
                        )
                        if (!exists) {
                            cameraUploadsRecordMapper(
                                media = it,
                                folderType = folderType,
                                fileType = fileType,
                                tempRoot = tempRoot,
                            )
                        } else null
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Check if the camera uploads media has already been inserted in the database
     *
     * @param cameraUploadsMedia the [CameraUploadsMedia] to check
     * @param recordsToCheck the list of [CameraUploadsRecord] saved in the database
     */
    private fun checkCameraUploadsRecordAlreadyExists(
        cameraUploadsMedia: CameraUploadsMedia,
        recordsToCheck: List<CameraUploadsRecord>,
    ): Boolean {
        return recordsToCheck.find {
            it.mediaId == cameraUploadsMedia.mediaId && it.timestamp == cameraUploadsMedia.timestamp
        } != null
    }
}

