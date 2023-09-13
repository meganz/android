package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import javax.inject.Inject

/**
 * Use case to collect and save photos and videos for camera upload
 */
/**
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getMediaStoreFileTypesUseCase [GetMediaStoreFileTypesUseCase]
 * @property isSecondaryFolderEnabled [IsSecondaryFolderEnabled]
 * @property getCameraUploadSelectionQueryUseCase [GetCameraUploadSelectionQueryUseCase]
 * @property updateTimeStamp [UpdateCameraUploadTimeStamp]
 * @property getPendingUploadListUseCase [GetPendingUploadListUseCase]
 * @property saveSyncRecordsToDBUseCase [SaveSyncRecordsToDBUseCase]
 */
class ProcessMediaForUploadUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getMediaStoreFileTypesUseCase: GetMediaStoreFileTypesUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val getCameraUploadSelectionQueryUseCase: GetCameraUploadSelectionQueryUseCase,
    private val updateTimeStamp: UpdateCameraUploadTimeStamp,
    private val getPendingUploadListUseCase: GetPendingUploadListUseCase,
    private val saveSyncRecordsToDBUseCase: SaveSyncRecordsToDBUseCase,
) {

    /**
     * Invoke
     * @param primaryUploadNodeId [NodeId]
     * @param secondaryUploadNodeId [NodeId]
     * @param tempRoot [String]
     */
    suspend operator fun invoke(
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId?,
        tempRoot: String,
    ) {
        val mediaStoreTypes = getMediaStoreFileTypesUseCase()
        val secondaryEnabled = isSecondaryFolderEnabled()
        coroutineScope {
            val list = mutableListOf<Job>()
            list.add(
                preparePrimaryPhotos(
                    types = mediaStoreTypes,
                    uploadNodeId = primaryUploadNodeId,
                    tempRoot = tempRoot,
                )
            )
            if (secondaryEnabled) {
                secondaryUploadNodeId?.let {
                    list.add(
                        prepareSecondaryPhotos(
                            types = mediaStoreTypes,
                            uploadNodeId = it,
                            tempRoot = tempRoot,
                        )
                    )
                }
            }
            list.add(
                preparePrimaryVideos(
                    types = mediaStoreTypes,
                    uploadNodeId = primaryUploadNodeId,
                    tempRoot = tempRoot,
                )
            )
            if (secondaryEnabled) {
                secondaryUploadNodeId?.let {
                    list.add(
                        prepareSecondaryVideos(
                            types = mediaStoreTypes,
                            uploadNodeId = it,
                            tempRoot = tempRoot,
                        )
                    )
                }
            }
            list.joinAll()
        }
    }

    private fun CoroutineScope.preparePrimaryPhotos(
        types: List<MediaStoreFileType>,
        uploadNodeId: NodeId,
        tempRoot: String,
    ) = launch {
        val primaryPhotos = arrayListOf<CameraUploadsMedia>()
        for (type in types) {
            if (type == MediaStoreFileType.IMAGES_INTERNAL || type == MediaStoreFileType.IMAGES_EXTERNAL) {
                primaryPhotos.addAll(
                    cameraUploadRepository.getMediaList(
                        mediaStoreFileType = type,
                        selectionQuery = getCameraUploadSelectionQueryUseCase(SyncTimeStamp.PRIMARY_PHOTO),
                    )
                )
            }
        }
        val pendingUploadsList = getPendingUploadListUseCase(
            mediaList = primaryPhotos,
            isSecondary = false,
            isVideo = false,
        )
        saveSyncRecordsToDBUseCase(
            list = pendingUploadsList,
            uploadNodeId = uploadNodeId,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_PHOTO)
    }

    private fun CoroutineScope.preparePrimaryVideos(
        types: List<MediaStoreFileType>,
        uploadNodeId: NodeId,
        tempRoot: String,
    ) = launch {
        val primaryVideos = arrayListOf<CameraUploadsMedia>()
        for (type in types) {
            if (type == MediaStoreFileType.VIDEO_INTERNAL || type == MediaStoreFileType.VIDEO_EXTERNAL) {
                primaryVideos.addAll(
                    cameraUploadRepository.getMediaList(
                        mediaStoreFileType = type,
                        selectionQuery = getCameraUploadSelectionQueryUseCase(SyncTimeStamp.PRIMARY_VIDEO),
                    )
                )
            }
        }
        val pendingVideoUploadsList = getPendingUploadListUseCase(
            mediaList = primaryVideos,
            isSecondary = false,
            isVideo = true,
        )
        saveSyncRecordsToDBUseCase(
            list = pendingVideoUploadsList,
            uploadNodeId = uploadNodeId,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_VIDEO)
    }

    private fun CoroutineScope.prepareSecondaryPhotos(
        types: List<MediaStoreFileType>,
        uploadNodeId: NodeId,
        tempRoot: String,
    ) = launch {
        val secondaryPhotos = arrayListOf<CameraUploadsMedia>()
        for (type in types) {
            if (type == MediaStoreFileType.IMAGES_INTERNAL || type == MediaStoreFileType.IMAGES_EXTERNAL) {
                secondaryPhotos.addAll(
                    cameraUploadRepository.getMediaList(
                        mediaStoreFileType = type,
                        selectionQuery = getCameraUploadSelectionQueryUseCase(SyncTimeStamp.SECONDARY_PHOTO),
                    )
                )
            }
        }
        val pendingUploadsListSecondary = getPendingUploadListUseCase(
            mediaList = secondaryPhotos,
            isSecondary = true,
            isVideo = false,
        )
        saveSyncRecordsToDBUseCase(
            list = pendingUploadsListSecondary,
            uploadNodeId = uploadNodeId,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_PHOTO)
    }

    private fun CoroutineScope.prepareSecondaryVideos(
        types: List<MediaStoreFileType>,
        uploadNodeId: NodeId,
        tempRoot: String,
    ) = launch {
        val secondaryVideos = arrayListOf<CameraUploadsMedia>()
        for (type in types) {
            if (type == MediaStoreFileType.VIDEO_INTERNAL || type == MediaStoreFileType.VIDEO_EXTERNAL) {

                secondaryVideos.addAll(
                    cameraUploadRepository.getMediaList(
                        mediaStoreFileType = type,
                        selectionQuery = getCameraUploadSelectionQueryUseCase(SyncTimeStamp.SECONDARY_VIDEO),
                    )
                )
            }
        }
        val pendingVideoUploadsListSecondary = getPendingUploadListUseCase(
            mediaList = secondaryVideos,
            isSecondary = true,
            isVideo = true,
        )
        saveSyncRecordsToDBUseCase(
            list = pendingVideoUploadsListSecondary,
            uploadNodeId = uploadNodeId,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)
    }
}
