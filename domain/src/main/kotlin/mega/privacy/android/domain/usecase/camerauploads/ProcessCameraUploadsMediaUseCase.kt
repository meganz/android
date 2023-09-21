package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Use case to retrieve media from the media stores, and save them in the database
 * to be uploaded by Camera Uploads
 *
 * @property getPrimaryFolderPathUseCase
 * @property getSecondaryFolderPathUseCase
 * @property getMediaStoreFileTypesUseCase
 * @property isSecondaryFolderEnabled
 * @property retrieveMediaFromMediaStoreUseCase
 */
class ProcessCameraUploadsMediaUseCase @Inject constructor(
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val getMediaStoreFileTypesUseCase: GetMediaStoreFileTypesUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val retrieveMediaFromMediaStoreUseCase: RetrieveMediaFromMediaStoreUseCase,
    private val saveCameraUploadsRecordUseCase: SaveCameraUploadsRecordUseCase,
) {

    /**
     * Invoke
     * @param tempRoot [String]
     */
    suspend operator fun invoke(
        tempRoot: String,
    ) = coroutineScope {
        val (photoMediaStoreTypes, videoMediaStoreTypes) = getMediaStoreFileTypesUseCase().partition { it.isImageFileType() }
        val primaryFolderPath = getPrimaryFolderPathUseCase()

        val primaryPhotoMedia = async {
            photoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                retrieveMediaFromMediaStoreUseCase(
                    parentPath = primaryFolderPath,
                    types = it,
                    folderType = CameraUploadFolderType.Primary,
                    fileType = SyncRecordType.TYPE_PHOTO,
                    tempRoot = tempRoot,
                )
            } ?: emptyList()
        }

        val primaryVideoMedia = async {
            videoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                retrieveMediaFromMediaStoreUseCase(
                    parentPath = primaryFolderPath,
                    types = it,
                    folderType = CameraUploadFolderType.Primary,
                    fileType = SyncRecordType.TYPE_VIDEO,
                    tempRoot = tempRoot,
                )
            } ?: emptyList()
        }

        val isSecondaryFolderEnabled = isSecondaryFolderEnabled()
        val secondaryFolderPath = getSecondaryFolderPathUseCase()

        val secondaryPhotoMedia =
            if (isSecondaryFolderEnabled) {
                async {
                    photoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                        retrieveMediaFromMediaStoreUseCase(
                            parentPath = secondaryFolderPath,
                            types = it,
                            folderType = CameraUploadFolderType.Secondary,
                            fileType = SyncRecordType.TYPE_PHOTO,
                            tempRoot = tempRoot,
                        )
                    }.orEmpty()
                }
            } else null

        val secondaryVideoMedia =
            if (isSecondaryFolderEnabled) {
                async {
                    videoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                        retrieveMediaFromMediaStoreUseCase(
                            parentPath = secondaryFolderPath,
                            types = it,
                            folderType = CameraUploadFolderType.Secondary,
                            fileType = SyncRecordType.TYPE_VIDEO,
                            tempRoot = tempRoot,
                        )
                    }.orEmpty()
                }
            } else null

        val combinedList = buildList {
            addAll(primaryPhotoMedia.await())
            addAll(primaryVideoMedia.await())
            secondaryPhotoMedia?.let { addAll(it.await()) }
            secondaryVideoMedia?.let { addAll(it.await()) }
        }
        saveCameraUploadsRecordUseCase(combinedList)
    }
}
