package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mega.privacy.android.data.wrapper.CameraUploadSyncManagerWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.camerauploads.GetMediaStoreFileTypesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPendingUploadListUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import nz.mega.sdk.MegaNode
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject

/**
 * Default implementation of [ProcessMediaForUpload]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getPrimaryFolderPathUseCase [GetPrimaryFolderPathUseCase]
 * @property getMediaStoreFileTypesUseCase [GetMediaStoreFileTypesUseCase]
 * @property isSecondaryFolderEnabled [IsSecondaryFolderEnabled]
 * @property selectionQuery [GetCameraUploadSelectionQuery]
 * @property localPathSecondary [GetCameraUploadLocalPathSecondary]
 * @property updateTimeStamp [UpdateCameraUploadTimeStamp]
 * @property getPendingUploadListUseCase [GetPendingUploadListUseCase]
 * @property saveSyncRecordsToDB [SaveSyncRecordsToDB]
 * @property cameraUploadRepository [CameraUploadSyncManagerWrapper]
 */
class DefaultProcessMediaForUpload @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getMediaStoreFileTypesUseCase: GetMediaStoreFileTypesUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val selectionQuery: GetCameraUploadSelectionQuery,
    private val localPathSecondary: GetCameraUploadLocalPathSecondary,
    private val updateTimeStamp: UpdateCameraUploadTimeStamp,
    private val getPendingUploadListUseCase: GetPendingUploadListUseCase,
    private val saveSyncRecordsToDB: SaveSyncRecordsToDB,
    private val cameraUploadSyncManagerWrapper: CameraUploadSyncManagerWrapper,
) : ProcessMediaForUpload {

    override suspend fun invoke(
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    ) {
        val mediaStoreTypes = getMediaStoreFileTypesUseCase()
        val secondaryEnabled = isSecondaryFolderEnabled()
        coroutineScope {
            val list = mutableListOf<Job>()
            list.add(
                preparePrimaryPhotos(
                    types = mediaStoreTypes,
                    primaryUploadNode = primaryUploadNode,
                    secondaryUploadNode = secondaryUploadNode,
                    tempRoot = tempRoot,
                )
            )
            list.add(
                prepareSecondaryPhotos(
                    types = mediaStoreTypes,
                    primaryUploadNode = primaryUploadNode,
                    secondaryUploadNode = secondaryUploadNode,
                    tempRoot = tempRoot,
                    isSecondaryEnabled = secondaryEnabled,
                )
            )
            list.add(
                preparePrimaryVideos(
                    types = mediaStoreTypes,
                    primaryUploadNode = primaryUploadNode,
                    secondaryUploadNode = secondaryUploadNode,
                    tempRoot = tempRoot,
                )
            )
            list.add(
                prepareSecondaryVideos(
                    types = mediaStoreTypes,
                    primaryUploadNode = primaryUploadNode,
                    secondaryUploadNode = secondaryUploadNode,
                    tempRoot = tempRoot,
                    isSecondaryEnabled = secondaryEnabled,
                )
            )
            list.joinAll()
            // Reset backup state as active.
            cameraUploadSyncManagerWrapper.updatePrimaryFolderBackupState(BackupState.ACTIVE)
            cameraUploadSyncManagerWrapper.updateSecondaryFolderBackupState(BackupState.ACTIVE)
        }
    }

    private fun CoroutineScope.preparePrimaryPhotos(
        types: List<MediaStoreFileType>, primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    ) = launch {
        val primaryPhotos: Queue<CameraUploadMedia> = LinkedList()
        for (type in types) {
            if (type == MediaStoreFileType.IMAGES_INTERNAL || type == MediaStoreFileType.IMAGES_EXTERNAL) {
                primaryPhotos.addAll(
                    cameraUploadRepository.getMediaQueue(
                        mediaStoreFileType = type,
                        parentPath = getPrimaryFolderPathUseCase(),
                        isVideo = false,
                        selectionQuery = selectionQuery(SyncTimeStamp.PRIMARY_PHOTO),
                    )
                )
            }
        }
        val pendingUploadsList = getPendingUploadListUseCase(
            mediaList = primaryPhotos,
            isSecondary = false,
            isVideo = false,
        )
        saveSyncRecordsToDB(
            list = pendingUploadsList,
            primaryUploadNode = primaryUploadNode,
            secondaryUploadNode = secondaryUploadNode,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_PHOTO)
    }

    private fun CoroutineScope.preparePrimaryVideos(
        types: List<MediaStoreFileType>, primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    ) = launch {
        val primaryVideos: Queue<CameraUploadMedia> = LinkedList()
        for (type in types) {
            if (type == MediaStoreFileType.VIDEO_INTERNAL || type == MediaStoreFileType.VIDEO_EXTERNAL) {
                primaryVideos.addAll(
                    cameraUploadRepository.getMediaQueue(
                        mediaStoreFileType = type,
                        parentPath = getPrimaryFolderPathUseCase(),
                        isVideo = true,
                        selectionQuery = selectionQuery(SyncTimeStamp.PRIMARY_VIDEO),
                    )
                )
            }
        }
        val pendingVideoUploadsList = getPendingUploadListUseCase(
            mediaList = primaryVideos,
            isSecondary = false,
            isVideo = true,
        )
        saveSyncRecordsToDB(
            list = pendingVideoUploadsList,
            primaryUploadNode = primaryUploadNode,
            secondaryUploadNode = secondaryUploadNode,
            rootPath = tempRoot,
        )
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_VIDEO)
    }

    private fun CoroutineScope.prepareSecondaryPhotos(
        types: List<MediaStoreFileType>, primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?, isSecondaryEnabled: Boolean,
    ) = launch {
        if (isSecondaryEnabled) {
            val secondaryPhotos: Queue<CameraUploadMedia> = LinkedList()
            for (type in types) {
                if (type == MediaStoreFileType.IMAGES_INTERNAL || type == MediaStoreFileType.IMAGES_EXTERNAL) {
                    secondaryPhotos.addAll(
                        cameraUploadRepository.getMediaQueue(
                            mediaStoreFileType = type,
                            parentPath = localPathSecondary(),
                            isVideo = false,
                            selectionQuery = selectionQuery(SyncTimeStamp.SECONDARY_PHOTO),
                        )
                    )
                }
            }
            val pendingUploadsListSecondary = getPendingUploadListUseCase(
                mediaList = secondaryPhotos,
                isSecondary = true,
                isVideo = false,
            )
            saveSyncRecordsToDB(
                list = pendingUploadsListSecondary,
                primaryUploadNode = primaryUploadNode,
                secondaryUploadNode = secondaryUploadNode,
                rootPath = tempRoot,
            )
            updateTimeStamp(null, SyncTimeStamp.SECONDARY_PHOTO)
        }
    }

    private fun CoroutineScope.prepareSecondaryVideos(
        types: List<MediaStoreFileType>, primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?, isSecondaryEnabled: Boolean,
    ) = launch {
        if (isSecondaryEnabled) {
            val secondaryVideos: Queue<CameraUploadMedia> = LinkedList()
            for (type in types) {
                if (type == MediaStoreFileType.VIDEO_INTERNAL || type == MediaStoreFileType.VIDEO_EXTERNAL) {

                    secondaryVideos.addAll(
                        cameraUploadRepository.getMediaQueue(
                            mediaStoreFileType = type,
                            parentPath = localPathSecondary(),
                            isVideo = true,
                            selectionQuery = selectionQuery(SyncTimeStamp.SECONDARY_VIDEO),
                        )
                    )
                }
            }
            val pendingVideoUploadsListSecondary = getPendingUploadListUseCase(
                mediaList = secondaryVideos,
                isSecondary = true,
                isVideo = true,
            )
            saveSyncRecordsToDB(
                list = pendingVideoUploadsListSecondary,
                primaryUploadNode = primaryUploadNode,
                secondaryUploadNode = secondaryUploadNode,
                rootPath = tempRoot,
            )
            updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)
        }
    }
}
