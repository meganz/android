package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.mapper.CameraUploadsRecordMapper
import javax.inject.Inject

/**
 * Retrieve a list of [CameraUploadsMedia] from the media store
 */
class RetrieveMediaFromMediaStoreUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
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
        val selectionQuery = cameraUploadsRepository.getMediaSelectionQuery(parentPath)

        val (recordsInPrimaryFolder, recordsInSecondaryFolder) =
            cameraUploadsRepository.getAllCameraUploadsRecords()
                .partition { it.folderType == CameraUploadFolderType.Primary }

        val semaphore = Semaphore(8)
        return@coroutineScope types.flatMap {
            cameraUploadsRepository.getMediaList(
                mediaStoreFileType = it,
                selectionQuery = selectionQuery,
            ).map {
                async {
                    semaphore.withPermit {
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

