package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Remove backup folder use case
 *
 * When user tries to logout, should delete backups first.
 * This should be called before Logout UseCase(megaApi.logout()).
 */
class RemoveBackupFolderUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {

    /**
     * invoke
     *
     * @param cameraUploadFolderType selects primary or secondary folder type
     */
    suspend operator fun invoke(cameraUploadFolderType: CameraUploadFolderType): BackupRemovalStatus? =
        cameraUploadsRepository.getBackupFolderId(cameraUploadFolderType)?.let {
            cameraUploadsRepository.removeBackupFolder(backupId = it)
        }

}