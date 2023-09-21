package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecordType
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
        fileType: SyncRecordType,
        tempRoot: String,
    ): List<CameraUploadsRecord> = coroutineScope {
        val selectionQuery = cameraUploadRepository.getMediaSelectionQuery(parentPath)
        return@coroutineScope types.flatMap {
            cameraUploadRepository.getMediaList(
                mediaStoreFileType = it,
                selectionQuery = selectionQuery,
            ).map {
                async {
                    yield()
                    runCatching {
                        cameraUploadsRecordMapper(
                            media = it,
                            folderType = folderType,
                            fileType = fileType,
                            tempRoot = tempRoot,
                        )
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }
    }
}

