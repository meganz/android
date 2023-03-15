package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.data.wrapper.CameraUploadSyncManagerWrapper
import mega.privacy.android.data.mapper.MediaStoreFileTypeMapper
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import nz.mega.sdk.MegaNode
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject

/**
 * Use case to collect and save photos and videos for camera upload
 */

class DefaultProcessMediaForUpload @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getSyncFileUploadUris: GetSyncFileUploadUris,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val selectionQuery: GetCameraUploadSelectionQuery,
    private val localPath: GetCameraUploadLocalPath,
    private val localPathSecondary: GetCameraUploadLocalPathSecondary,
    private val updateTimeStamp: UpdateCameraUploadTimeStamp,
    private val getPendingUploadList: GetPendingUploadList,
    private val saveSyncRecordsToDB: SaveSyncRecordsToDB,
    private val mediaStoreFileTypeMapper: MediaStoreFileTypeMapper,
    private val cameraUploadSyncManagerWrapper: CameraUploadSyncManagerWrapper,
) : ProcessMediaForUpload {

    override suspend fun invoke(
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    ) {
        val mediaStoreTypes = getSyncFileUploadUris().mapNotNull(mediaStoreFileTypeMapper)
        val secondaryEnabled = isSecondaryFolderEnabled()
        coroutineScope {
            val list = mutableListOf<Job>()
            list.add(
                preparePrimaryPhotos(mediaStoreTypes,
                    primaryUploadNode,
                    secondaryUploadNode,
                    tempRoot)
            )
            list.add(
                prepareSecondaryPhotos(mediaStoreTypes,
                    primaryUploadNode,
                    secondaryUploadNode,
                    tempRoot,
                    secondaryEnabled)
            )
            list.add(
                preparePrimaryVideos(mediaStoreTypes,
                    primaryUploadNode,
                    secondaryUploadNode,
                    tempRoot)
            )
            list.add(
                prepareSecondaryVideos(mediaStoreTypes,
                    primaryUploadNode,
                    secondaryUploadNode,
                    tempRoot,
                    secondaryEnabled)
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
                        type,
                        localPath(),
                        false,
                        selectionQuery(SyncTimeStamp.PRIMARY_PHOTO)
                    )
                )
            }
        }
        val pendingUploadsList = getPendingUploadList(
            primaryPhotos,
            isSecondary = false,
            isVideo = false
        )
        saveSyncRecordsToDB(
            pendingUploadsList, primaryUploadNode, secondaryUploadNode, tempRoot
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
                        type,
                        localPath(),
                        true,
                        selectionQuery(SyncTimeStamp.PRIMARY_VIDEO)
                    )
                )
            }
        }
        val pendingVideoUploadsList = getPendingUploadList(
            primaryVideos,
            isSecondary = false,
            isVideo = true
        )
        saveSyncRecordsToDB(
            pendingVideoUploadsList, primaryUploadNode, secondaryUploadNode, tempRoot
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
                            type,
                            localPathSecondary(),
                            false,
                            selectionQuery(SyncTimeStamp.SECONDARY_PHOTO)
                        )
                    )
                }
            }
            val pendingUploadsListSecondary = getPendingUploadList(
                secondaryPhotos,
                isSecondary = true,
                isVideo = false
            )
            saveSyncRecordsToDB(
                pendingUploadsListSecondary, primaryUploadNode, secondaryUploadNode, tempRoot
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
                            type,
                            localPathSecondary(),
                            true,
                            selectionQuery(SyncTimeStamp.SECONDARY_VIDEO)
                        )
                    )
                }
            }
            val pendingVideoUploadsListSecondary = getPendingUploadList(
                secondaryVideos,
                isSecondary = true,
                isVideo = true
            )
            saveSyncRecordsToDB(
                pendingVideoUploadsListSecondary,
                primaryUploadNode,
                secondaryUploadNode,
                tempRoot
            )
            updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)
        }
    }
}
