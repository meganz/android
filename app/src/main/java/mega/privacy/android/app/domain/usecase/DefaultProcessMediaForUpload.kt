package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.yield
import mega.privacy.android.app.sync.BackupState
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
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
) : ProcessMediaForUpload {

    override suspend fun invoke(
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    ) {
        val primaryPhotos: Queue<CameraUploadMedia> = LinkedList()
        val primaryVideos: Queue<CameraUploadMedia> = LinkedList()
        val secondaryPhotos: Queue<CameraUploadMedia> = LinkedList()
        val secondaryVideos: Queue<CameraUploadMedia> = LinkedList()
        val mediaStoreTypes = getSyncFileUploadUris().map(mediaStoreFileTypeMapper)
        val secondaryEnabled = isSecondaryFolderEnabled()

        for (type in mediaStoreTypes) {
            if (type == MediaStoreFileType.IMAGES_INTERNAL || type == MediaStoreFileType.IMAGES_EXTERNAL) {
                // Photos
                primaryPhotos.addAll(
                    cameraUploadRepository.getMediaQueue(
                        type,
                        localPath(),
                        false,
                        selectionQuery(SyncTimeStamp.PRIMARY_PHOTO)
                    )
                )
                if (secondaryEnabled) {
                    secondaryPhotos.addAll(
                        cameraUploadRepository.getMediaQueue(
                            type,
                            localPathSecondary(),
                            false,
                            selectionQuery(SyncTimeStamp.SECONDARY_PHOTO)
                        )
                    )
                }
            } else if (type == MediaStoreFileType.VIDEO_INTERNAL || type == MediaStoreFileType.VIDEO_EXTERNAL) {
                // Videos
                primaryVideos.addAll(
                    cameraUploadRepository.getMediaQueue(
                        type,
                        localPath(),
                        true,
                        selectionQuery(SyncTimeStamp.PRIMARY_VIDEO)
                    )
                )
                if (secondaryEnabled) {
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
        }

        val pendingUploadsList = getPendingUploadList(
            primaryPhotos,
            isSecondary = false,
            isVideo = false
        )
        saveSyncRecordsToDB(
            pendingUploadsList, primaryUploadNode, secondaryUploadNode, tempRoot
        )

        val pendingVideoUploadsList = getPendingUploadList(
            primaryVideos,
            isSecondary = false,
            isVideo = true
        )
        saveSyncRecordsToDB(
            pendingVideoUploadsList, primaryUploadNode, secondaryUploadNode, tempRoot
        )

        if (secondaryEnabled) {
            val pendingUploadsListSecondary = getPendingUploadList(
                secondaryPhotos,
                isSecondary = true,
                isVideo = false
            )
            saveSyncRecordsToDB(
                pendingUploadsListSecondary, primaryUploadNode, secondaryUploadNode, tempRoot
            )

            val pendingVideoUploadsListSecondary = getPendingUploadList(
                secondaryVideos,
                isSecondary = true,
                isVideo = true
            )
            saveSyncRecordsToDB(
                pendingVideoUploadsListSecondary, primaryUploadNode, secondaryUploadNode, tempRoot
            )
        }
        yield()

        // Need to maintain timestamp for better performance
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_VIDEO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)

        // Reset backup state as active.
        CameraUploadSyncManager.updatePrimaryFolderBackupState(BackupState.ACTIVE)
        CameraUploadSyncManager.updateSecondaryFolderBackupState(BackupState.ACTIVE)
    }
}
